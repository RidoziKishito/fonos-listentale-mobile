package com.example.fonoss.ui.book;

import com.example.fonoss.R;
import dagger.hilt.android.AndroidEntryPoint;

import com.example.fonoss.utils.UiNotifier;
import com.example.fonoss.ui.library.LibraryViewModel;
import com.example.fonoss.adapter.BookAdapter;
import com.example.fonoss.adapter.PlaylistSelectionAdapter;
import com.example.fonoss.data.model.Book;
import com.example.fonoss.data.model.Playlist;

import android.content.Context;
import android.content.res.ColorStateList;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@AndroidEntryPoint
public class BookDetailFragment extends Fragment {

    private Book currentBook;
    private LibraryViewModel libraryViewModel;
    private ImageButton buttonFavorite, buttonDownload;
    private View buttonListen, buttonRead;
    private ProgressBar progressDownload;
    private View relatedBooksSection;
    private List<Book> relatedBooks = new ArrayList<>();
    private BookAdapter relatedBooksAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_book_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        libraryViewModel = new ViewModelProvider(requireActivity()).get(LibraryViewModel.class);
        buttonFavorite = view.findViewById(R.id.button_favorite);
        buttonDownload = view.findViewById(R.id.button_download);
        View buttonAddToPlaylist = view.findViewById(R.id.button_add_to_playlist);
        buttonListen = view.findViewById(R.id.button_listen);
        buttonRead = view.findViewById(R.id.button_read);
        progressDownload = view.findViewById(R.id.progress_download);
        relatedBooksSection = view.findViewById(R.id.section_related_books);

        if (getArguments() != null) {
            currentBook = (Book) getArguments().getSerializable("book");
            if (currentBook != null) {
                bindBookDetails(view);
                setupRelatedBooks(view);
                fetchRelatedBooks();
            }
        }

        // Lắng nghe thay đổi từ Database để cập nhật UI ngay lập tức
        libraryViewModel.getSavedBooks().observe(getViewLifecycleOwner(), books -> updateFavoriteUI());
        libraryViewModel.getDownloadedBooks().observe(getViewLifecycleOwner(), books -> {
            updateDownloadUI();
            checkOfflineAvailability();
        });

        buttonFavorite.setOnClickListener(v -> { if (currentBook != null) libraryViewModel.toggleFavorite(currentBook); });

        buttonAddToPlaylist.setOnClickListener(v -> showPlaylistSelectionDialog());

        buttonDownload.setOnClickListener(v -> {
            if (currentBook != null) {
                if (!libraryViewModel.isDownloaded(currentBook.getId())) startDownload();
                else startDelete();
            }
        });

        view.findViewById(R.id.button_back).setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        buttonListen.setOnClickListener(v -> {
            if (currentBook == null) return;
            if (isAvailable()) {
                Bundle bundle = new Bundle();
                bundle.putSerializable("book", currentBook);
                Navigation.findNavController(v).navigate(R.id.action_bookDetailFragment_to_audioPlayerFragment, bundle);
            } else {
                UiNotifier.info(getContext(), "Connect to internet to listen to this book");
            }
        });

        buttonRead.setOnClickListener(v -> {
            if (currentBook == null) return;
            if (isAvailable()) {
                Bundle bundle = new Bundle();
                bundle.putSerializable("book", currentBook);
                Navigation.findNavController(v).navigate(R.id.action_bookDetailFragment_to_ebookReaderFragment, bundle);
            } else {
                UiNotifier.info(getContext(), "Connect to internet to read this book");
            }
        });

        checkOfflineAvailability();
    }

    private void setupRelatedBooks(View view) {
        RecyclerView recyclerView = view.findViewById(R.id.recycler_related_books);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        relatedBooksAdapter = new BookAdapter(relatedBooks, book -> {
            Bundle bundle = new Bundle();
            bundle.putSerializable("book", book);
            Navigation.findNavController(view).navigate(R.id.action_bookDetailFragment_to_bookDetailFragment, bundle);
        });
        recyclerView.setAdapter(relatedBooksAdapter);
    }

    private void fetchRelatedBooks() {
        if (currentBook == null || currentBook.getGenre() == null) return;
        
        String currentGenre = currentBook.getGenre().toLowerCase();
        String currentSeries = currentBook.getSeries() != null ? normalizeSeries(currentBook.getSeries()) : null;

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("books")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Book> list = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        Book book = doc.toObject(Book.class);
                        if (book != null) {
                            book.setId(doc.getId());
                            // Skip same book
                            if (book.getId().equals(currentBook.getId())) continue;

                            boolean isRelated = false;
                            if (book.getGenre() != null && book.getGenre().toLowerCase().contains(currentGenre)) {
                                isRelated = true;
                            } else if (currentSeries != null && book.getSeries() != null && normalizeSeries(book.getSeries()).equals(currentSeries)) {
                                isRelated = true;
                            }

                            if (isRelated) list.add(book);
                        }
                    }
                    if (!list.isEmpty()) {
                        relatedBooks.clear();
                        relatedBooks.addAll(list);
                        relatedBooksAdapter.notifyDataSetChanged();
                        relatedBooksSection.setVisibility(View.VISIBLE);
                    }
                });
    }

    private String normalizeSeries(String series) {
        return series.toLowerCase().replaceAll("\\s+", "");
    }

    private boolean isAvailable() {
        return isOnline() || (currentBook != null && libraryViewModel.isDownloaded(currentBook.getId()));
    }

    private void checkOfflineAvailability() {
        boolean available = isAvailable();
        buttonListen.setEnabled(available);
        buttonRead.setEnabled(available);
        buttonListen.setAlpha(available ? 1.0f : 0.5f);
        buttonRead.setAlpha(available ? 1.0f : 0.5f);
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private void bindBookDetails(View view) {
        if (currentBook == null) return;
        ((TextView)view.findViewById(R.id.text_book_title)).setText(currentBook.getTitle());
        ((TextView)view.findViewById(R.id.text_author)).setText(String.format("by %s", currentBook.getAuthor()));
        ((TextView)view.findViewById(R.id.text_book_rating)).setText(String.format(Locale.getDefault(), "%.1f", currentBook.getRating()));
        ((TextView)view.findViewById(R.id.text_audio_duration)).setText(currentBook.getDuration());
        ((TextView)view.findViewById(R.id.text_ebook_pages)).setText(currentBook.getPages());
        ((TextView)view.findViewById(R.id.text_book_genre)).setText(currentBook.getGenre());
        ((TextView)view.findViewById(R.id.text_synopsis)).setText(currentBook.getDescription());
        Glide.with(this).load(currentBook.getCoverUrl()).placeholder(android.R.drawable.ic_menu_gallery).into((ImageView) view.findViewById(R.id.image_cover_bg));
        
        updateFavoriteUI();
        updateDownloadUI();
    }

    private void startDownload() {
        updateDownloadProgressUI(true);
        libraryViewModel.enqueueSequentialDownloads(Collections.singletonList(currentBook));
        observeDownloadStatus(currentBook.getId());
    }

    private void observeDownloadStatus(String bookId) {
        libraryViewModel.getDownloadProgress().observe(getViewLifecycleOwner(), progressMap -> {
            if (progressMap != null && !progressMap.containsKey(bookId)) {
                updateDownloadProgressUI(false);
            }
        });
    }

    private void updateDownloadProgressUI(boolean downloading) {
        buttonDownload.setVisibility(downloading ? View.INVISIBLE : View.VISIBLE);
        progressDownload.setVisibility(downloading ? View.VISIBLE : View.GONE);
    }

    private void startDelete() {
        updateDownloadProgressUI(true);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (currentBook != null) {
                libraryViewModel.removeDownload(currentBook.getId());
                updateDownloadProgressUI(false);
                UiNotifier.success(getContext(), "Deleted from device");
            }
        }, 1000);
    }

    private void updateFavoriteUI() {
        if (currentBook != null && libraryViewModel.isFavorite(currentBook.getId())) {
            buttonFavorite.setImageResource(R.drawable.ic_detail_heart_filled);
            buttonFavorite.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.accent_vibrant)));
        } else {
            buttonFavorite.setImageResource(R.drawable.ic_detail_heart);
            buttonFavorite.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.slate_900)));
        }
    }

    private void updateDownloadUI() {
        if (currentBook != null && libraryViewModel.isDownloaded(currentBook.getId())) {
            buttonDownload.setImageResource(R.drawable.ic_detail_trash);
            buttonDownload.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.red_600)));
        } else {
            buttonDownload.setImageResource(R.drawable.ic_detail_download);
            buttonDownload.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.slate_900)));
        }
    }

    private void showPlaylistSelectionDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.dialog_select_playlist, null);
        dialog.setContentView(view);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_select_playlist);
        TextView textEmpty = view.findViewById(R.id.text_no_playlists);

        PlaylistSelectionAdapter adapter = new PlaylistSelectionAdapter(playlist -> {
            libraryViewModel.addBooksToPlaylist(playlist.getId(), Collections.singletonList(currentBook));
            UiNotifier.success(getContext(), "Added to " + playlist.getName());
            dialog.dismiss();
        });
        recyclerView.setAdapter(adapter);

        libraryViewModel.getPlaylists().observe(getViewLifecycleOwner(), playlists -> {
            if (playlists == null || playlists.isEmpty()) {
                textEmpty.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                textEmpty.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                adapter.updateList(playlists);
            }
        });

        dialog.show();
    }
}

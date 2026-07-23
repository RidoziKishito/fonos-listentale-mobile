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
    private RatingViewModel ratingViewModel;
    private ImageButton buttonFavorite, buttonDownload;
    private View buttonListen, buttonRead, buttonRateBook;
    private TextView textBookRating, textRatingCount;
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
        ratingViewModel = new ViewModelProvider(this).get(RatingViewModel.class);

        buttonFavorite = view.findViewById(R.id.button_favorite);
        buttonDownload = view.findViewById(R.id.button_download);
        View buttonAddToPlaylist = view.findViewById(R.id.button_add_to_playlist);
        buttonListen = view.findViewById(R.id.button_listen);
        buttonRead = view.findViewById(R.id.button_read);
        buttonRateBook = view.findViewById(R.id.button_rate_book);
        textBookRating = view.findViewById(R.id.text_book_rating);
        textRatingCount = view.findViewById(R.id.text_rating_count);
        progressDownload = view.findViewById(R.id.progress_download);
        relatedBooksSection = view.findViewById(R.id.section_related_books);

        if (getArguments() != null) {
            currentBook = (Book) getArguments().getSerializable("book");
            if (currentBook != null) {
                bindBookDetails(view);
                setupRelatedBooks(view);
                fetchRelatedBooks();
                ratingViewModel.loadUserRating(currentBook.getId());
            }
        }

        if (buttonRateBook != null) {
            buttonRateBook.setOnClickListener(v -> showRatingDialog());
        }

        ratingViewModel.getToastMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) {
                UiNotifier.info(getContext(), msg);
                ratingViewModel.clearToastMessage();
            }
        });

        ratingViewModel.getIsOperationSuccess().observe(getViewLifecycleOwner(), success -> {
            if (Boolean.TRUE.equals(success) && currentBook != null) {
                refreshBookRatingData();
            }
        });

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

        view.findViewById(R.id.button_ai_chat).setOnClickListener(v -> {
            if (currentBook == null) return;
            Bundle bundle = new Bundle();
            bundle.putSerializable("book", currentBook);
            Navigation.findNavController(v).navigate(R.id.action_bookDetailFragment_to_chatFragment, bundle);
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

        if (textBookRating == null) textBookRating = view.findViewById(R.id.text_book_rating);
        if (textRatingCount == null) textRatingCount = view.findViewById(R.id.text_rating_count);

        if (textBookRating != null) {
            textBookRating.setText(String.format(Locale.getDefault(), "%.1f", currentBook.getRating()));
        }
        if (textRatingCount != null) {
            textRatingCount.setText(String.format(Locale.getDefault(), "(%d)", currentBook.getRatingCount()));
        }

        ((TextView)view.findViewById(R.id.text_audio_duration)).setText(currentBook.getDuration());
        ((TextView)view.findViewById(R.id.text_ebook_pages)).setText(currentBook.getPages());
        ((TextView)view.findViewById(R.id.text_book_genre)).setText(currentBook.getGenre());
        ((TextView)view.findViewById(R.id.text_synopsis)).setText(currentBook.getDescription());
        Glide.with(this).load(currentBook.getCoverUrl()).placeholder(android.R.drawable.ic_menu_gallery).into((ImageView) view.findViewById(R.id.image_cover_bg));

        updateFavoriteUI();
        updateDownloadUI();
    }

    private void refreshBookRatingData() {
        if (currentBook == null) return;
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("books")
                .document(currentBook.getId())
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Double rating = snapshot.getDouble("rating");
                        Long ratingCount = snapshot.getLong("rating_count");
                        if (ratingCount == null) ratingCount = snapshot.getLong("ratingCount");

                        if (rating != null) {
                            currentBook.setRating(rating);
                            if (textBookRating != null) {
                                textBookRating.setText(String.format(Locale.getDefault(), "%.1f", rating));
                            }
                        }
                        if (ratingCount != null) {
                            currentBook.setRatingCount(ratingCount);
                            if (textRatingCount != null) {
                                textRatingCount.setText(String.format(Locale.getDefault(), "(%d)", ratingCount));
                            }
                        }
                    }
                    if (ratingViewModel != null) {
                        ratingViewModel.resetOperationState();
                    }
                });
    }

    private void showRatingDialog() {
        if (currentBook == null || getContext() == null) return;
        if (ratingViewModel.getCurrentUserId() == null) {
            UiNotifier.info(getContext(), getString(R.string.login_required_to_rate));
            return;
        }

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_rate_book, null);
        dialog.setContentView(dialogView);

        TextView textBookTitle = dialogView.findViewById(R.id.text_book_title);
        android.widget.RatingBar ratingBar = dialogView.findViewById(R.id.rating_bar);
        TextView textDescription = dialogView.findViewById(R.id.text_rating_description);
        com.google.android.material.button.MaterialButton buttonSubmit = dialogView.findViewById(R.id.button_submit_rating);
        com.google.android.material.button.MaterialButton buttonRemove = dialogView.findViewById(R.id.button_remove_rating);

        if (textBookTitle != null) {
            textBookTitle.setText(currentBook.getTitle());
        }

        Double existingUserRating = ratingViewModel.getUserRating().getValue();

        if (existingUserRating != null && existingUserRating > 0) {
            if (ratingBar != null) ratingBar.setRating(existingUserRating.floatValue());
            if (buttonSubmit != null) buttonSubmit.setText(getString(R.string.update_rating));
            if (buttonRemove != null) buttonRemove.setVisibility(View.VISIBLE);
            updateRatingDescription(textDescription, existingUserRating.floatValue());
        } else {
            if (ratingBar != null) ratingBar.setRating(0);
            if (buttonSubmit != null) buttonSubmit.setText(getString(R.string.submit_rating));
            if (buttonRemove != null) buttonRemove.setVisibility(View.GONE);
            if (textDescription != null) textDescription.setText("Tap a star to rate");
        }

        if (ratingBar != null) {
            ratingBar.setOnRatingBarChangeListener((bar, rating, fromUser) -> updateRatingDescription(textDescription, rating));
        }

        if (buttonSubmit != null) {
            buttonSubmit.setOnClickListener(v -> {
                if (ratingBar == null || ratingBar.getRating() < 1) {
                    UiNotifier.info(getContext(), getString(R.string.please_select_rating));
                    return;
                }
                ratingViewModel.submitRating(currentBook.getId(), ratingBar.getRating());
                dialog.dismiss();
            });
        }

        if (buttonRemove != null) {
            buttonRemove.setOnClickListener(v -> {
                ratingViewModel.deleteRating(currentBook.getId());
                dialog.dismiss();
            });
        }

        dialog.show();
    }

    private void updateRatingDescription(TextView textView, float rating) {
        if (textView == null) return;
        int r = Math.round(rating);
        switch (r) {
            case 1:
                textView.setText("1 Star - Poor");
                break;
            case 2:
                textView.setText("2 Stars - Below Average");
                break;
            case 3:
                textView.setText("3 Stars - Average");
                break;
            case 4:
                textView.setText("4 Stars - Good");
                break;
            case 5:
                textView.setText("5 Stars - Excellent!");
                break;
            default:
                textView.setText("Tap a star to rate");
                break;
        }
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

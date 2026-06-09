package com.example.fonoss;

import android.content.Context;
import android.content.res.ColorStateList;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.bumptech.glide.Glide;
import java.util.Locale;

public class BookDetailFragment extends Fragment {

    private Book currentBook;
    private LibraryViewModel libraryViewModel;
    private ImageButton buttonFavorite, buttonDownload;
    private View buttonListen, buttonRead;
    private ProgressBar progressDownload;

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
        buttonListen = view.findViewById(R.id.button_listen);
        buttonRead = view.findViewById(R.id.button_read);
        progressDownload = view.findViewById(R.id.progress_download);

        if (getArguments() != null) {
            currentBook = (Book) getArguments().getSerializable("book");
            if (currentBook != null) {
                bindBookDetails(view);
                // Nếu chưa có chapters và đã tải về máy, nạp thêm dữ liệu từ máy ngầm
                if (currentBook.getChapters() == null || currentBook.getChapters().isEmpty()) {
                    Book localBook = libraryViewModel.loadBookLocally(currentBook.getId());
                    if (localBook != null) currentBook.setChapters(localBook.getChapters());
                }
            }
        }

        // Lắng nghe thay đổi từ Database để cập nhật UI ngay lập tức
        libraryViewModel.getSavedBooks().observe(getViewLifecycleOwner(), books -> updateFavoriteUI());
        libraryViewModel.getDownloadedBooks().observe(getViewLifecycleOwner(), books -> {
            updateDownloadUI();
            checkOfflineAvailability();
        });

        if (currentBook != null) observeDownloadStatus(currentBook.getId());

        buttonFavorite.setOnClickListener(v -> { if (currentBook != null) libraryViewModel.toggleFavorite(currentBook); });

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
                UiNotifier.warning(getContext(), "Connect to internet to listen");
            }
        });

        buttonRead.setOnClickListener(v -> {
            if (currentBook == null) return;
            if (isAvailable()) {
                Bundle bundle = new Bundle();
                bundle.putSerializable("book", currentBook);
                Navigation.findNavController(v).navigate(R.id.action_bookDetailFragment_to_ebookReaderFragment, bundle);
            } else {
                UiNotifier.warning(getContext(), "Connect to internet to read");
            }
        });

        checkOfflineAvailability();
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
        if (currentBook != null) {
            libraryViewModel.enqueueSequentialDownloads(java.util.Collections.singletonList(currentBook));
            updateDownloadProgressUI(true);
            UiNotifier.info(getContext(), "Download queued");
        }
    }

    private void observeDownloadStatus(String bookId) {
        libraryViewModel.getDownloadProgress().observe(getViewLifecycleOwner(), progressMap -> {
            Integer progress = progressMap == null ? null : progressMap.get(bookId);
            updateDownloadProgressUI(progress != null && progress >= 0);
        });
    }

    private void updateDownloadProgressUI(boolean isDownloading) {
        progressDownload.setVisibility(isDownloading ? View.VISIBLE : View.GONE);
        buttonDownload.setVisibility(isDownloading ? View.INVISIBLE : View.VISIBLE);
    }

    private void startDelete() {
        if (currentBook != null) {
            libraryViewModel.removeDownload(currentBook.getId());
            UiNotifier.info(getContext(), "Deleted from device");
        }
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
}

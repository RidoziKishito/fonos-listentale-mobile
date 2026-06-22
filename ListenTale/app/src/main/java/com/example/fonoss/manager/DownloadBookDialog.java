package com.example.fonoss.manager;

import com.example.fonoss.R;

import com.example.fonoss.ui.library.LibraryViewModel;
import com.example.fonoss.data.model.Book;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class DownloadBookDialog extends BottomSheetDialogFragment {

    private final Map<String, Book> allBooksMap = new HashMap<>();
    private final Map<String, Book> globalCatalogBooksMap = new HashMap<>();
    private final List<Book> filteredBooks = new ArrayList<>();
    private final Set<String> selectedBookIds = new HashSet<>();
    private final Set<String> downloadedBookIds = new HashSet<>();
    private final Map<String, Integer> downloadingProgressMap = new HashMap<>();

    private DownloadBookAdapter adapter;
    private Button downloadButton;
    private CheckBox selectAllCheckBox;
    private ProgressBar loadingBar;
    private TextView emptyText;
    private LibraryViewModel libraryViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_download_books, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        libraryViewModel = new ViewModelProvider(requireActivity()).get(LibraryViewModel.class);
        loadingBar = view.findViewById(R.id.loading_download_books);
        emptyText = view.findViewById(R.id.text_no_download_books);
        downloadButton = view.findViewById(R.id.button_download_selected);
        selectAllCheckBox = view.findViewById(R.id.checkbox_select_all_books);
        TextInputEditText searchInput = view.findViewById(R.id.input_search_download_books);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_download_books);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new DownloadBookAdapter(filteredBooks, selectedBookIds, downloadedBookIds, downloadingProgressMap,
                this::onSelectionChanged, this::cancelDownload);
        recyclerView.setAdapter(adapter);

        view.findViewById(R.id.button_close_download_dialog).setOnClickListener(v -> dismiss());
        downloadButton.setOnClickListener(v -> downloadSelectedBooks());
        selectAllCheckBox.setOnClickListener(v -> selectAllVisible(selectAllCheckBox.isChecked()));

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterBooks(s == null ? "" : s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        libraryViewModel.getSavedBooks().observe(getViewLifecycleOwner(), this::updateBooksSource);
        libraryViewModel.getInProgressBooks().observe(getViewLifecycleOwner(), this::updateBooksSource);
        libraryViewModel.getCompletedBooks().observe(getViewLifecycleOwner(), this::updateBooksSource);

        libraryViewModel.getDownloadedBooks().observe(getViewLifecycleOwner(), books -> {
            updateBooksSource(books);
            downloadedBookIds.clear();
            if (books != null) {
                for (Book book : books) {
                    if (book != null && book.getId() != null) downloadedBookIds.add(book.getId());
                }
            }
            downloadingProgressMap.keySet().removeAll(downloadedBookIds);
            selectedBookIds.removeAll(downloadedBookIds);
            filterBooks(getCurrentQuery());
        });

        libraryViewModel.getDownloadProgress().observe(getViewLifecycleOwner(), this::updateDownloadProgress);

        loadingBar.setVisibility(View.GONE);
        fetchAllBooks();
    }

    private void updateDownloadProgress(Map<String, Integer> newProgressMap) {
        downloadingProgressMap.clear();
        if (newProgressMap != null) {
            for (Map.Entry<String, Integer> entry : newProgressMap.entrySet()) {
                if (!downloadedBookIds.contains(entry.getKey())) {
                    downloadingProgressMap.put(entry.getKey(), entry.getValue());
                }
            }
        }
        filterBooks(getCurrentQuery());
    }

    private void fetchAllBooks() {
        FirebaseFirestore.getInstance().collection("books").get().addOnCompleteListener(task -> {
            if (!isAdded()) return;
            if (task.isSuccessful() && task.getResult() != null) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Book book = document.toObject(Book.class);
                    if (book != null) {
                        book.setId(document.getId());
                        globalCatalogBooksMap.put(book.getId(), book);
                    }
                }
                filterBooks(getCurrentQuery());
            } else {
                emptyText.setText("Unable to load books");
                emptyText.setVisibility(View.VISIBLE);
            }
        });
    }

    private void updateBooksSource(List<Book> books) {
        if (books == null) return;
        boolean changed = false;
        for (Book book : books) {
            if (book != null && book.getId() != null && !allBooksMap.containsKey(book.getId())) {
                allBooksMap.put(book.getId(), book);
                changed = true;
            }
        }
        if (changed) filterBooks(getCurrentQuery());
    }

    private void cancelDownload(String bookId) {
        libraryViewModel.cancelDownload(bookId);
        downloadingProgressMap.remove(bookId);
        Toast.makeText(requireContext(), "Download cancelled", Toast.LENGTH_SHORT).show();
        filterBooks(getCurrentQuery());
    }

    private void filterBooks(String query) {
        String normalizedQuery = query.trim().toLowerCase(Locale.ROOT);
        filteredBooks.clear();

        List<Book> downloadingList = new ArrayList<>();
        List<Book> historyList = new ArrayList<>();
        List<Book> availableList = new ArrayList<>();

        Iterable<Book> sourceBooks = globalCatalogBooksMap.isEmpty()
                ? allBooksMap.values()
                : globalCatalogBooksMap.values();

        for (Book book : sourceBooks) {
            if (book == null || book.getId() == null) continue;
            if (normalizedQuery.isEmpty()
                    || containsIgnoreCase(book.getTitle(), normalizedQuery)
                    || containsIgnoreCase(book.getAuthor(), normalizedQuery)
                    || containsIgnoreCase(book.getGenre(), normalizedQuery)) {
                if (isTrackedDownload(book.getId())) {
                    downloadingList.add(book);
                } else if (downloadedBookIds.contains(book.getId())) {
                    historyList.add(book);
                } else {
                    availableList.add(book);
                }
            }
        }

        filteredBooks.addAll(downloadingList);
        filteredBooks.addAll(historyList);
        filteredBooks.addAll(availableList);

        adapter.notifyDataSetChanged();
        emptyText.setText("No books found");
        emptyText.setVisibility(filteredBooks.isEmpty() && loadingBar.getVisibility() != View.VISIBLE
                ? View.VISIBLE : View.GONE);
        updateSelectionControls();
    }

    private boolean containsIgnoreCase(String value, String normalizedQuery) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(normalizedQuery);
    }

    private void selectAllVisible(boolean selected) {
        for (Book book : filteredBooks) {
            if (book.getId() == null || downloadedBookIds.contains(book.getId()) || isActiveDownload(book.getId())) continue;
            if (selected) selectedBookIds.add(book.getId());
            else selectedBookIds.remove(book.getId());
        }
        adapter.notifyDataSetChanged();
        updateSelectionControls();
    }

    private void onSelectionChanged() {
        updateSelectionControls();
    }

    private void updateSelectionControls() {
        int selectableVisibleCount = 0;
        int selectedVisibleCount = 0;
        for (Book book : filteredBooks) {
            if (book.getId() == null || downloadedBookIds.contains(book.getId()) || isActiveDownload(book.getId())) continue;
            selectableVisibleCount++;
            if (selectedBookIds.contains(book.getId())) selectedVisibleCount++;
        }

        selectAllCheckBox.setOnCheckedChangeListener(null);
        selectAllCheckBox.setChecked(selectableVisibleCount > 0
                && selectedVisibleCount == selectableVisibleCount);
        downloadButton.setEnabled(!selectedBookIds.isEmpty());
        downloadButton.setText(selectedBookIds.isEmpty()
                ? "Download"
                : "Download (" + selectedBookIds.size() + ")");
    }

    private void downloadSelectedBooks() {
        List<Book> booksToDownload = new ArrayList<>();
        for (String id : selectedBookIds) {
            Book book = globalCatalogBooksMap.containsKey(id) ? globalCatalogBooksMap.get(id) : allBooksMap.get(id);
            if (book != null && !downloadedBookIds.contains(id) && !isActiveDownload(id)) {
                booksToDownload.add(book);
            }
        }

        if (booksToDownload.isEmpty()) {
            Toast.makeText(requireContext(), "Please select books to download", Toast.LENGTH_SHORT).show();
            return;
        }

        for (Book book : booksToDownload) {
            downloadingProgressMap.put(book.getId(), 0);
        }
        libraryViewModel.enqueueSequentialDownloads(booksToDownload);
        Toast.makeText(requireContext(), "Downloads added to queue", Toast.LENGTH_SHORT).show();
        selectedBookIds.clear();
        filterBooks(getCurrentQuery());
    }

    private String getCurrentQuery() {
        if (getView() == null) return "";
        TextInputEditText searchInput = getView().findViewById(R.id.input_search_download_books);
        return searchInput != null && searchInput.getText() != null ? searchInput.getText().toString() : "";
    }

    private boolean isTrackedDownload(String bookId) {
        return downloadingProgressMap.containsKey(bookId);
    }

    private boolean isActiveDownload(String bookId) {
        Integer progress = downloadingProgressMap.get(bookId);
        return progress != null && progress >= 0;
    }

    private static class DownloadBookAdapter
            extends RecyclerView.Adapter<DownloadBookAdapter.ViewHolder> {

        private final List<Book> books;
        private final Set<String> selectedBookIds;
        private final Set<String> downloadedBookIds;
        private final Map<String, Integer> downloadingProgressMap;
        private final Runnable selectionChanged;
        private final OnCancelListener cancelListener;

        interface OnCancelListener {
            void onCancel(String bookId);
        }

        DownloadBookAdapter(List<Book> books, Set<String> selectedBookIds,
                            Set<String> downloadedBookIds, Map<String, Integer> downloadingProgressMap,
                            Runnable selectionChanged, OnCancelListener cancelListener) {
            this.books = books;
            this.selectedBookIds = selectedBookIds;
            this.downloadedBookIds = downloadedBookIds;
            this.downloadingProgressMap = downloadingProgressMap;
            this.selectionChanged = selectionChanged;
            this.cancelListener = cancelListener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_download_book_selection, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Book book = books.get(position);
            Integer trackedProgress = downloadingProgressMap.get(book.getId());
            boolean downloaded = downloadedBookIds.contains(book.getId());
            boolean failed = trackedProgress != null && trackedProgress < 0;
            boolean downloading = trackedProgress != null && trackedProgress >= 0;
            int progress = trackedProgress == null ? 0 : trackedProgress;

            holder.title.setText(book.getTitle());
            holder.author.setText(book.getAuthor());
            holder.meta.setText(buildMeta(book));

            holder.status.setVisibility(downloaded || failed ? View.VISIBLE : View.GONE);
            holder.status.setText(downloaded ? "Downloaded" : "Failed");
            holder.checkBox.setVisibility((downloaded || downloading) ? View.GONE : View.VISIBLE);
            holder.layoutDownloading.setVisibility(downloading ? View.VISIBLE : View.GONE);

            if (downloading) {
                holder.progressBar.setProgress(progress);
                holder.textProgress.setText(progress + "%");
                holder.buttonCancel.setOnClickListener(v -> cancelListener.onCancel(book.getId()));
            } else {
                holder.buttonCancel.setOnClickListener(null);
            }

            holder.checkBox.setEnabled(!downloaded && !downloading);
            holder.itemView.setAlpha((downloaded || downloading) ? 0.7f : 1f);

            holder.checkBox.setOnCheckedChangeListener(null);
            holder.checkBox.setChecked(!downloaded && !downloading && selectedBookIds.contains(book.getId()));
            holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (downloaded || downloading || book.getId() == null) return;
                if (isChecked) selectedBookIds.add(book.getId());
                else selectedBookIds.remove(book.getId());
                selectionChanged.run();
            });

            holder.itemView.setOnClickListener(v -> {
                if (!downloaded && !downloading) holder.checkBox.setChecked(!holder.checkBox.isChecked());
            });

            Glide.with(holder.itemView.getContext())
                    .load(book.getCoverUrl())
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(holder.cover);
        }

        private String buildMeta(Book book) {
            List<String> parts = new ArrayList<>();
            if (!TextUtils.isEmpty(book.getGenre())) parts.add(book.getGenre());
            if (!TextUtils.isEmpty(book.getDuration())) parts.add(book.getDuration());
            if (!TextUtils.isEmpty(book.getPages())) parts.add(book.getPages());
            return TextUtils.join(" | ", parts);
        }

        @Override
        public int getItemCount() {
            return books.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final ImageView cover;
            final TextView title;
            final TextView author;
            final TextView meta;
            final TextView status;
            final CheckBox checkBox;
            final LinearLayout layoutDownloading;
            final ProgressBar progressBar;
            final TextView textProgress;
            final ImageButton buttonCancel;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                cover = itemView.findViewById(R.id.image_download_book_cover);
                title = itemView.findViewById(R.id.text_download_book_title);
                author = itemView.findViewById(R.id.text_download_book_author);
                meta = itemView.findViewById(R.id.text_download_book_meta);
                status = itemView.findViewById(R.id.text_downloaded_status);
                checkBox = itemView.findViewById(R.id.checkbox_download_book);
                layoutDownloading = itemView.findViewById(R.id.layout_downloading_state);
                progressBar = itemView.findViewById(R.id.progress_downloading);
                textProgress = itemView.findViewById(R.id.text_downloading_progress);
                buttonCancel = itemView.findViewById(R.id.button_cancel_download);
            }
        }
    }
}



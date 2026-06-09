package com.example.fonoss;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class DownloadBookDialog extends BottomSheetDialogFragment {

    private final List<Book> allBooks = new ArrayList<>();
    private final List<Book> filteredBooks = new ArrayList<>();
    private final Set<String> selectedBookIds = new HashSet<>();
    private final Set<String> downloadedBookIds = new HashSet<>();

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
        adapter = new DownloadBookAdapter(filteredBooks, selectedBookIds, downloadedBookIds,
                this::onSelectionChanged);
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

        libraryViewModel.getDownloadedBooks().observe(getViewLifecycleOwner(), books -> {
            downloadedBookIds.clear();
            if (books != null) {
                for (Book book : books) {
                    if (book != null && book.getId() != null) downloadedBookIds.add(book.getId());
                }
            }
            selectedBookIds.removeAll(downloadedBookIds);
            adapter.notifyDataSetChanged();
            updateSelectionControls();
        });

        fetchAllBooks();
    }

    private void fetchAllBooks() {
        loadingBar.setVisibility(View.VISIBLE);
        emptyText.setVisibility(View.GONE);

        FirebaseFirestore.getInstance().collection("books").get().addOnCompleteListener(task -> {
            if (!isAdded()) return;
            loadingBar.setVisibility(View.GONE);

            if (!task.isSuccessful() || task.getResult() == null) {
                emptyText.setText("Unable to load books");
                emptyText.setVisibility(View.VISIBLE);
                Toast.makeText(requireContext(), "Error fetching books", Toast.LENGTH_SHORT).show();
                return;
            }

            allBooks.clear();
            for (QueryDocumentSnapshot document : task.getResult()) {
                Book book = document.toObject(Book.class);
                if (book == null) continue;
                book.setId(document.getId());
                allBooks.add(book);
            }
            filterBooks("");
        });
    }

    private void filterBooks(String query) {
        String normalizedQuery = query.trim().toLowerCase(Locale.ROOT);
        filteredBooks.clear();

        for (Book book : allBooks) {
            if (normalizedQuery.isEmpty()
                    || containsIgnoreCase(book.getTitle(), normalizedQuery)
                    || containsIgnoreCase(book.getAuthor(), normalizedQuery)
                    || containsIgnoreCase(book.getGenre(), normalizedQuery)) {
                filteredBooks.add(book);
            }
        }

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
            if (book.getId() == null || downloadedBookIds.contains(book.getId())) continue;
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
            if (book.getId() == null || downloadedBookIds.contains(book.getId())) continue;
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
        for (Book book : allBooks) {
            if (book.getId() != null
                    && selectedBookIds.contains(book.getId())
                    && !downloadedBookIds.contains(book.getId())) {
                booksToDownload.add(book);
            }
        }

        if (booksToDownload.isEmpty()) {
            Toast.makeText(requireContext(), "Please select books to download", Toast.LENGTH_SHORT).show();
            return;
        }

        libraryViewModel.enqueueSequentialDownloads(booksToDownload);
        Toast.makeText(requireContext(), "Downloads added to queue", Toast.LENGTH_SHORT).show();
        dismiss();
    }

    private static class DownloadBookAdapter
            extends RecyclerView.Adapter<DownloadBookAdapter.ViewHolder> {

        private final List<Book> books;
        private final Set<String> selectedBookIds;
        private final Set<String> downloadedBookIds;
        private final Runnable selectionChanged;

        DownloadBookAdapter(List<Book> books, Set<String> selectedBookIds,
                            Set<String> downloadedBookIds, Runnable selectionChanged) {
            this.books = books;
            this.selectedBookIds = selectedBookIds;
            this.downloadedBookIds = downloadedBookIds;
            this.selectionChanged = selectionChanged;
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
            boolean downloaded = downloadedBookIds.contains(book.getId());

            holder.title.setText(book.getTitle());
            holder.author.setText(book.getAuthor());
            holder.meta.setText(buildMeta(book));
            holder.status.setVisibility(downloaded ? View.VISIBLE : View.GONE);
            holder.checkBox.setEnabled(!downloaded);
            holder.itemView.setAlpha(downloaded ? 0.55f : 1f);

            holder.checkBox.setOnCheckedChangeListener(null);
            holder.checkBox.setChecked(!downloaded && selectedBookIds.contains(book.getId()));
            holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (downloaded || book.getId() == null) return;
                if (isChecked) selectedBookIds.add(book.getId());
                else selectedBookIds.remove(book.getId());
                selectionChanged.run();
            });

            holder.itemView.setOnClickListener(v -> {
                if (!downloaded) holder.checkBox.setChecked(!holder.checkBox.isChecked());
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

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                cover = itemView.findViewById(R.id.image_download_book_cover);
                title = itemView.findViewById(R.id.text_download_book_title);
                author = itemView.findViewById(R.id.text_download_book_author);
                meta = itemView.findViewById(R.id.text_download_book_meta);
                status = itemView.findViewById(R.id.text_downloaded_status);
                checkBox = itemView.findViewById(R.id.checkbox_download_book);
            }
        }
    }
}

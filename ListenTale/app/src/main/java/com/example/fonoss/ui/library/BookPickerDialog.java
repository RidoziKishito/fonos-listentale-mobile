package com.example.fonoss.ui.library;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import com.example.fonoss.R;
import com.example.fonoss.adapter.BookAdapter;
import com.example.fonoss.data.model.Book;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BookPickerDialog extends BottomSheetDialogFragment {

    private List<Book> allBooks = new ArrayList<>();
    private BookAdapter adapter;
    private final Set<String> selectedIds = new HashSet<>();
    private OnBooksSelectedListener listener;

    public interface OnBooksSelectedListener {
        void onBooksSelected(List<Book> selectedBooks);
    }

    public void setListener(OnBooksSelectedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_book_picker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_book_picker);
        adapter = new BookAdapter(allBooks, new BookAdapter.OnBookClickListener() {
            @Override public void onBookClick(Book book) {}
            @Override public void onSelectionChanged(int count) {}
        }, true);
        adapter.setSelectionMode(true);
        adapter.setSelectedBookIds(selectedIds);
        recyclerView.setAdapter(adapter);

        view.findViewById(R.id.button_confirm_selection).setOnClickListener(v -> {
            if (listener != null) {
                List<Book> selected = new ArrayList<>();
                for (Book b : allBooks) {
                    if (selectedIds.contains(b.getId())) {
                        selected.add(b);
                    }
                }
                listener.onBooksSelected(selected);
            }
            dismiss();
        });

        fetchBooks();
    }

    private void fetchBooks() {
        FirebaseFirestore.getInstance().collection("books").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                allBooks.clear();
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    Book book = doc.toObject(Book.class);
                    book.setId(doc.getId());
                    allBooks.add(book);
                }
                adapter.notifyDataSetChanged();
            }
        });
    }
}

package com.example.fonoss.ui.chat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fonoss.R;
import com.example.fonoss.data.model.Book;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

public class BookPickerBottomSheet extends BottomSheetDialogFragment {

    public interface OnBookPickedListener {
        void onBookPicked(Book book);
    }

    private final List<Book> books;
    private final String currentSelectedBookId;
    private final OnBookPickedListener listener;

    public BookPickerBottomSheet(List<Book> books, String currentSelectedBookId, OnBookPickedListener listener) {
        this.books = books;
        this.currentSelectedBookId = currentSelectedBookId;
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_select_chat_book, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_books);
        TextView textEmpty = view.findViewById(R.id.text_empty_books);

        if (books == null || books.isEmpty()) {
            textEmpty.setVisibility(View.VISIBLE);
            textEmpty.setText("No books found in library.");
            recyclerView.setVisibility(View.GONE);
        } else {
            textEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);

            BookSelectionAdapter adapter = new BookSelectionAdapter(book -> {
                dismiss();
                if (listener != null) listener.onBookPicked(book);
            });
            recyclerView.setAdapter(adapter);
            adapter.setBooks(books, currentSelectedBookId);
        }
    }
}

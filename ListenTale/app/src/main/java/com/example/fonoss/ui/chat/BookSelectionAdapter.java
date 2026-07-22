package com.example.fonoss.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.fonoss.R;
import com.example.fonoss.data.model.Book;

import java.util.ArrayList;
import java.util.List;

public class BookSelectionAdapter extends RecyclerView.Adapter<BookSelectionAdapter.BookViewHolder> {

    public interface OnBookSelectedListener {
        void onBookSelected(Book book);
    }

    private List<Book> books = new ArrayList<>();
    private String selectedBookId;
    private final OnBookSelectedListener listener;

    public BookSelectionAdapter(OnBookSelectedListener listener) {
        this.listener = listener;
    }

    public void setBooks(List<Book> books, String selectedBookId) {
        this.books = (books != null) ? books : new ArrayList<>();
        this.selectedBookId = selectedBookId;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_book_selection, parent, false);
        return new BookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
        Book book = books.get(position);
        holder.textTitle.setText(book.getTitle());
        holder.textAuthor.setText(book.getAuthor());

        boolean isSelected = book.getId() != null && book.getId().equals(selectedBookId);
        holder.radioButton.setChecked(isSelected);

        Glide.with(holder.itemView.getContext())
                .load(book.getCoverUrl())
                .circleCrop()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(holder.imageCover);

        holder.itemView.setOnClickListener(v -> {
            selectedBookId = book.getId();
            notifyDataSetChanged();
            if (listener != null) {
                listener.onBookSelected(book);
            }
        });
    }

    @Override
    public int getItemCount() {
        return books.size();
    }

    static class BookViewHolder extends RecyclerView.ViewHolder {
        ImageView imageCover;
        TextView textTitle, textAuthor;
        RadioButton radioButton;

        public BookViewHolder(@NonNull View itemView) {
            super(itemView);
            imageCover = itemView.findViewById(R.id.image_book_cover);
            textTitle = itemView.findViewById(R.id.text_book_title);
            textAuthor = itemView.findViewById(R.id.text_book_author);
            radioButton = itemView.findViewById(R.id.radio_selected);
        }
    }
}

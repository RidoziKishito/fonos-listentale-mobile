package com.example.fonoss;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BookAdapter extends RecyclerView.Adapter<BookAdapter.BookViewHolder> {

    private List<Book> books;
    private OnBookClickListener listener;
    private boolean isHorizontal;
    private boolean isSelectionMode = false;
    private Set<String> selectedBookIds = new HashSet<>();

    public interface OnBookClickListener {
        void onBookClick(Book book);
        default void onSelectionChanged(int count) {}
    }

    public BookAdapter(List<Book> books, OnBookClickListener listener) {
        this(books, listener, false);
    }

    public BookAdapter(List<Book> books, OnBookClickListener listener, boolean isHorizontal) {
        this.books = books;
        this.listener = listener;
        this.isHorizontal = isHorizontal;
    }

    @NonNull
    @Override
    public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = isHorizontal ? R.layout.item_book_horizontal : R.layout.item_book_card;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new BookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
        Book book = books.get(position);
        holder.title.setText(book.getTitle());
        holder.author.setText(book.getAuthor());
        if (holder.category != null) holder.category.setText(book.getGenre());
        
        Glide.with(holder.itemView.getContext())
                .load(book.getCoverUrl())
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .into(holder.cover);

        if (holder.checkBox != null) {
            holder.checkBox.setVisibility(isSelectionMode ? View.VISIBLE : View.GONE);
            holder.checkBox.setOnCheckedChangeListener(null);
            holder.checkBox.setChecked(selectedBookIds.contains(book.getId()));
            holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) selectedBookIds.add(book.getId());
                else selectedBookIds.remove(book.getId());
                listener.onSelectionChanged(selectedBookIds.size());
            });
        }

        holder.itemView.setOnClickListener(v -> {
            if (isSelectionMode && holder.checkBox != null) {
                holder.checkBox.setChecked(!holder.checkBox.isChecked());
            } else {
                listener.onBookClick(book);
            }
        });
    }

    public void setSelectionMode(boolean enabled) {
        this.isSelectionMode = enabled;
        if (!enabled) selectedBookIds.clear();
        notifyDataSetChanged();
    }

    public boolean isSelectionMode() { return isSelectionMode; }

    public Set<String> getSelectedBookIds() { return selectedBookIds; }

    public void setSelectedBookIds(Set<String> ids) {
        this.selectedBookIds = ids;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() { return books.size(); }

    public void updateList(List<Book> newList) {
        this.books = newList;
        notifyDataSetChanged();
    }

    static class BookViewHolder extends RecyclerView.ViewHolder {
        TextView title, author, category;
        ImageView cover;
        CheckBox checkBox;

        public BookViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.text_book_title);
            author = itemView.findViewById(R.id.text_book_author);
            cover = itemView.findViewById(R.id.image_book_cover);
            category = itemView.findViewById(R.id.text_book_category);
            checkBox = itemView.findViewById(R.id.checkbox_select);
        }
    }
}

package com.example.fonoss.adapter;

import com.example.fonoss.R;

import com.example.fonoss.data.model.Book;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
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
    private Set<String> downloadedBookIds = new HashSet<>();
    private String userAccountType = "FREE";

    public interface OnBookClickListener {
        void onBookClick(Book book);
        default void onPlayClick(Book book) {}
        default void onReadClick(Book book) {}
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
        if (holder.duration != null) holder.duration.setText(book.getDuration());
        if (holder.pages != null) holder.pages.setText(book.getPages());
        
        Glide.with(holder.itemView.getContext())
                .load(book.getCoverUrl())
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .into(holder.cover);

        boolean isDownloaded = downloadedBookIds.contains(book.getId());
        boolean isLocked = book.getIsPremium() && "FREE".equals(userAccountType);

        if (isSelectionMode) {
            holder.itemView.setEnabled(!isDownloaded && !isLocked);
            holder.itemView.setAlpha((isDownloaded || isLocked) ? 0.5f : 1.0f);
            if (holder.checkBox != null) {
                holder.checkBox.setEnabled(!isDownloaded && !isLocked);
                holder.checkBox.setVisibility(View.VISIBLE);
                holder.checkBox.setOnCheckedChangeListener(null);
                holder.checkBox.setChecked(selectedBookIds.contains(book.getId()) && !isDownloaded && !isLocked);
                holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) selectedBookIds.add(book.getId());
                    else selectedBookIds.remove(book.getId());
                    listener.onSelectionChanged(selectedBookIds.size());
                });
            }
        } else {
            holder.itemView.setEnabled(true);
            holder.itemView.setAlpha(isLocked ? 0.5f : 1.0f);
            if (holder.checkBox != null) holder.checkBox.setVisibility(View.GONE);
        }

        if (holder.lockIcon != null) {
            holder.lockIcon.setVisibility(isLocked ? View.VISIBLE : View.GONE);
        }

        if (holder.buttonPlay != null) {
            holder.buttonPlay.setOnClickListener(v -> {
                if (isLocked) showUpgradeToast(v.getContext());
                else listener.onPlayClick(book);
            });
            holder.buttonPlay.setAlpha(isLocked ? 0.5f : 1.0f);
            holder.buttonPlay.setEnabled(!isLocked);
        }
        if (holder.buttonRead != null) {
            holder.buttonRead.setOnClickListener(v -> {
                if (isLocked) showUpgradeToast(v.getContext());
                else listener.onReadClick(book);
            });
            holder.buttonRead.setAlpha(isLocked ? 0.5f : 1.0f);
            holder.buttonRead.setEnabled(!isLocked);
        }

        holder.itemView.setOnClickListener(v -> {
            if (isSelectionMode && holder.checkBox != null && !isDownloaded && !isLocked) {
                holder.checkBox.setChecked(!holder.checkBox.isChecked());
            } else if (!isSelectionMode) {
                if (isLocked) showUpgradeToast(v.getContext());
                else listener.onBookClick(book);
            }
        });
    }

    private void showUpgradeToast(android.content.Context context) {
        android.widget.Toast.makeText(context, "Premium account required to access this book", android.widget.Toast.LENGTH_SHORT).show();
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

    public void setDownloadedBookIds(Set<String> ids) {
        this.downloadedBookIds = ids;
        notifyDataSetChanged();
    }

    public void setUserAccountType(String type) {
        this.userAccountType = type != null ? type : "FREE";
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() { return books.size(); }

    public void updateList(List<Book> newList) {
        this.books = newList;
        notifyDataSetChanged();
    }

    static class BookViewHolder extends RecyclerView.ViewHolder {
        TextView title, author, category, duration, pages;
        ImageView cover, lockIcon;
        CheckBox checkBox;
        ImageButton buttonPlay, buttonRead;

        public BookViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.text_book_title);
            author = itemView.findViewById(R.id.text_book_author);
            cover = itemView.findViewById(R.id.image_book_cover);
            lockIcon = itemView.findViewById(R.id.image_premium_lock);
            category = itemView.findViewById(R.id.text_book_category);
            checkBox = itemView.findViewById(R.id.checkbox_select);
            duration = itemView.findViewById(R.id.text_book_duration);
            pages = itemView.findViewById(R.id.text_book_pages);
            buttonPlay = itemView.findViewById(R.id.button_play_item);
            buttonRead = itemView.findViewById(R.id.button_read_item);
        }
    }
}

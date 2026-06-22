package com.example.fonoss.adapter;

import com.example.fonoss.R;

import com.example.fonoss.data.model.Bookmark;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BookmarkAdapter extends RecyclerView.Adapter<BookmarkAdapter.ViewHolder> {

    private List<Bookmark> bookmarks;
    private OnBookmarkClickListener listener;
    private SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

    public interface OnBookmarkClickListener {
        void onBookmarkClick(Bookmark bookmark);
        void onDeleteClick(Bookmark bookmark);
    }

    public BookmarkAdapter(List<Bookmark> bookmarks, OnBookmarkClickListener listener) {
        this.bookmarks = bookmarks;
        this.listener = listener;
    }

    public void setBookmarks(List<Bookmark> bookmarks) {
        this.bookmarks = bookmarks;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_bookmark_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Bookmark bookmark = bookmarks.get(position);
        holder.textChapter.setText("Chapter " + (bookmark.getChapterIndex() + 1));
        holder.textContent.setText(bookmark.getSelectedText());
        holder.textTime.setText(sdf.format(new Date(bookmark.getTimestamp())));

        holder.itemView.setOnClickListener(v -> listener.onBookmarkClick(bookmark));
        holder.buttonDelete.setOnClickListener(v -> listener.onDeleteClick(bookmark));
    }

    @Override
    public int getItemCount() {
        return bookmarks != null ? bookmarks.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textChapter, textContent, textTime;
        ImageButton buttonDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textChapter = itemView.findViewById(R.id.text_bookmark_chapter);
            textContent = itemView.findViewById(R.id.text_bookmark_content);
            textTime = itemView.findViewById(R.id.text_bookmark_time);
            buttonDelete = itemView.findViewById(R.id.button_delete_bookmark);
        }
    }
}



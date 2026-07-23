package com.example.fonoss.adapter;

import com.example.fonoss.R;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ChapterAdapter extends RecyclerView.Adapter<ChapterAdapter.ViewHolder> {

    private List<String> chapters;
    private int currentChapterIndex = -1;
    private OnChapterClickListener listener;

    public interface OnChapterClickListener {
        void onChapterClick(int chapterIndex);
    }

    public ChapterAdapter(List<String> chapters, int currentChapterIndex, OnChapterClickListener listener) {
        this.chapters = chapters;
        this.currentChapterIndex = currentChapterIndex;
        this.listener = listener;
    }

    public void setChapters(List<String> chapters, int currentChapterIndex) {
        this.chapters = chapters;
        this.currentChapterIndex = currentChapterIndex;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_chapter_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String chapterName = "Chapter " + (position + 1);
        holder.textChapterTitle.setText(chapterName);

        if (position == currentChapterIndex) {
            holder.textChapterStatus.setVisibility(View.VISIBLE);
            holder.textChapterStatus.setText("Currently Reading");
        } else {
            holder.textChapterStatus.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onChapterClick(position);
        });
    }

    @Override
    public int getItemCount() {
        return chapters != null ? chapters.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textChapterTitle, textChapterStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textChapterTitle = itemView.findViewById(R.id.text_chapter_title);
            textChapterStatus = itemView.findViewById(R.id.text_chapter_status);
        }
    }
}

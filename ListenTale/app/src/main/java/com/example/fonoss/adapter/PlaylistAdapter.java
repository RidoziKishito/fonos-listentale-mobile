package com.example.fonoss.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.fonoss.R;
import com.example.fonoss.data.model.Playlist;
import java.util.ArrayList;
import java.util.List;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder> {

    private List<Playlist> playlists = new ArrayList<>();
    private final OnPlaylistClickListener listener;

    public interface OnPlaylistClickListener {
        void onPlaylistClick(Playlist playlist);
        void onMoreClick(Playlist playlist, View anchor);
    }

    public PlaylistAdapter(OnPlaylistClickListener listener) {
        this.listener = listener;
    }

    public void updateList(List<Playlist> newList) {
        this.playlists = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_playlist, parent, false);
        return new PlaylistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        Playlist playlist = playlists.get(position);
        holder.textName.setText(playlist.getName());
        int count = playlist.getBooks() != null ? playlist.getBooks().size() : 0;
        holder.textCount.setText(count + (count == 1 ? " book" : " books"));

        holder.itemView.setOnClickListener(v -> listener.onPlaylistClick(playlist));
        holder.buttonMore.setOnClickListener(v -> listener.onMoreClick(playlist, v));
    }

    @Override
    public int getItemCount() {
        return playlists.size();
    }

    static class PlaylistViewHolder extends RecyclerView.ViewHolder {
        TextView textName, textCount;
        ImageButton buttonMore;

        public PlaylistViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.text_playlist_name);
            textCount = itemView.findViewById(R.id.text_playlist_count);
            buttonMore = itemView.findViewById(R.id.button_playlist_more);
        }
    }
}

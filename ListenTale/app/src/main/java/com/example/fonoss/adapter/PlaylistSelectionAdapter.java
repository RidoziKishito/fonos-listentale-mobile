package com.example.fonoss.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.fonoss.R;
import com.example.fonoss.data.model.Playlist;
import java.util.ArrayList;
import java.util.List;

public class PlaylistSelectionAdapter extends RecyclerView.Adapter<PlaylistSelectionAdapter.ViewHolder> {

    private List<Playlist> list = new ArrayList<>();
    private final OnPlaylistSelectedListener listener;

    public interface OnPlaylistSelectedListener {
        void onPlaylistSelected(Playlist playlist);
    }

    public PlaylistSelectionAdapter(OnPlaylistSelectedListener listener) {
        this.listener = listener;
    }

    public void updateList(List<Playlist> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Playlist playlist = list.get(position);
        holder.textView.setText(playlist.getName());
        holder.itemView.setOnClickListener(v -> listener.onPlaylistSelected(playlist));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        ViewHolder(View v) {
            super(v);
            textView = v.findViewById(android.R.id.text1);
        }
    }
}

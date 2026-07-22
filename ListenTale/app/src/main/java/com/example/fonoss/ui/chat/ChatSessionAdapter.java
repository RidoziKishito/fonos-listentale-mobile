package com.example.fonoss.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fonoss.R;
import com.example.fonoss.data.model.ChatSession;

import java.util.ArrayList;
import java.util.List;

public class ChatSessionAdapter extends RecyclerView.Adapter<ChatSessionAdapter.SessionViewHolder> {

    public interface OnSessionClickListener {
        void onSessionClick(ChatSession session);
        void onSessionDelete(ChatSession session);
    }

    private List<ChatSession> sessions = new ArrayList<>();
    private final OnSessionClickListener listener;

    public ChatSessionAdapter(OnSessionClickListener listener) {
        this.listener = listener;
    }

    public void setSessions(List<ChatSession> sessions) {
        this.sessions = (sessions != null) ? sessions : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_session, parent, false);
        return new SessionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        ChatSession session = sessions.get(position);
        holder.textTitle.setText(session.getTitle());

        String lastMsg = session.getLastMessage();
        if (lastMsg == null || lastMsg.isEmpty()) {
            lastMsg = "No messages yet";
        }
        holder.textLastMsg.setText(lastMsg);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onSessionClick(session);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onSessionDelete(session);
        });
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    static class SessionViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle, textLastMsg;
        ImageView btnDelete;

        public SessionViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.text_session_title);
            textLastMsg = itemView.findViewById(R.id.text_session_last_msg);
            btnDelete = itemView.findViewById(R.id.button_delete_session);
        }
    }
}

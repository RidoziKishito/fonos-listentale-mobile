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
import com.example.fonoss.data.model.ChatSession;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

public class ChatSessionPickerDialog extends BottomSheetDialogFragment {

    public interface OnSessionSelectedListener {
        void onSessionSelected(ChatSession session);
        void onCreateNewSessionClicked();
        void onSessionDeleted(ChatSession session);
    }

    private final List<ChatSession> sessions;
    private final OnSessionSelectedListener listener;

    public ChatSessionPickerDialog(List<ChatSession> sessions, OnSessionSelectedListener listener) {
        this.sessions = sessions;
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_chat_sessions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_sessions);
        TextView textEmpty = view.findViewById(R.id.text_empty_sessions);
        View btnNew = view.findViewById(R.id.button_new_session);

        btnNew.setOnClickListener(v -> {
            dismiss();
            if (listener != null) listener.onCreateNewSessionClicked();
        });

        if (sessions == null || sessions.isEmpty()) {
            textEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            textEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);

            ChatSessionAdapter adapter = new ChatSessionAdapter(new ChatSessionAdapter.OnSessionClickListener() {
                @Override
                public void onSessionClick(ChatSession session) {
                    dismiss();
                    if (listener != null) listener.onSessionSelected(session);
                }

                @Override
                public void onSessionDelete(ChatSession session) {
                    if (listener != null) listener.onSessionDeleted(session);
                    dismiss();
                }
            });
            recyclerView.setAdapter(adapter);
            adapter.setSessions(sessions);
        }
    }
}

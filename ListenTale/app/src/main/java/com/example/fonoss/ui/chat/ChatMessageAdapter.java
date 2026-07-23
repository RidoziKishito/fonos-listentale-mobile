package com.example.fonoss.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fonoss.R;
import com.example.fonoss.data.model.ChatMessage;

import io.noties.markwon.Markwon;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_USER = 1;
    private static final int TYPE_AI = 2;
    private static final int TYPE_SYSTEM = 3;

    private List<ChatMessage> messageList = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private Markwon markwon;

    public void setMessages(List<ChatMessage> messages) {
        this.messageList = (messages != null) ? messages : new ArrayList<>();
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messageList.get(position);
        if (message.isSystem()) {
            return TYPE_SYSTEM;
        }
        return message.isUser() ? TYPE_USER : TYPE_AI;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (markwon == null) {
            markwon = Markwon.create(parent.getContext());
        }
        if (viewType == TYPE_USER) {
            View view = inflater.inflate(R.layout.item_chat_message_user, parent, false);
            return new UserMessageViewHolder(view);
        } else if (viewType == TYPE_SYSTEM) {
            View view = inflater.inflate(R.layout.item_chat_divider, parent, false);
            return new SystemDividerViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_chat_message_ai, parent, false);
            return new AiMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messageList.get(position);
        String timeStr = timeFormat.format(new Date(message.getTimestamp()));

        if (holder instanceof UserMessageViewHolder) {
            UserMessageViewHolder uHolder = (UserMessageViewHolder) holder;
            uHolder.textBody.setText(message.getContent());
            uHolder.textTime.setText(timeStr);
        } else if (holder instanceof AiMessageViewHolder) {
            AiMessageViewHolder aiHolder = (AiMessageViewHolder) holder;
            if (markwon != null) {
                markwon.setMarkdown(aiHolder.textBody, message.getContent());
            } else {
                aiHolder.textBody.setText(message.getContent());
            }
            aiHolder.textTime.setText(timeStr);
        } else if (holder instanceof SystemDividerViewHolder) {
            SystemDividerViewHolder sysHolder = (SystemDividerViewHolder) holder;
            sysHolder.textContent.setText(message.getContent());
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class UserMessageViewHolder extends RecyclerView.ViewHolder {
        TextView textBody, textTime;

        public UserMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textBody = itemView.findViewById(R.id.text_message_body);
            textTime = itemView.findViewById(R.id.text_message_time);
        }
    }

    static class AiMessageViewHolder extends RecyclerView.ViewHolder {
        TextView textBody, textTime;

        public AiMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textBody = itemView.findViewById(R.id.text_message_body);
            textTime = itemView.findViewById(R.id.text_message_time);
        }
    }

    static class SystemDividerViewHolder extends RecyclerView.ViewHolder {
        TextView textContent;

        public SystemDividerViewHolder(@NonNull View itemView) {
            super(itemView);
            textContent = itemView.findViewById(R.id.text_divider_content);
        }
    }
}

package com.example.fonoss.data.model;

import java.io.Serializable;
import java.util.UUID;

public class ChatMessage implements Serializable {
    public static final String SENDER_USER = "USER";
    public static final String SENDER_AI = "AI";
    public static final String SENDER_SYSTEM = "SYSTEM";

    private String messageId;
    private String sessionId;
    private String bookId;
    private String sender;
    private String content;
    private long timestamp;

    public ChatMessage() {
        this.messageId = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
    }

    public ChatMessage(String sessionId, String bookId, String sender, String content) {
        this();
        this.sessionId = sessionId;
        this.bookId = bookId;
        this.sender = sender;
        this.content = content;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isUser() {
        return SENDER_USER.equalsIgnoreCase(sender);
    }

    public boolean isSystem() {
        return SENDER_SYSTEM.equalsIgnoreCase(sender);
    }
}

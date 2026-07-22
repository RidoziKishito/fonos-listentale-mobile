package com.example.fonoss.data.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChatSession implements Serializable {
    private String sessionId;
    private String title;
    private long createdAt;
    private long updatedAt;
    private List<String> bookIds;
    private String activeBookId;
    private String lastMessage;

    public ChatSession() {
        this.sessionId = UUID.randomUUID().toString();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.bookIds = new ArrayList<>();
        this.title = "New Conversation";
        this.lastMessage = "";
    }

    public ChatSession(String title, String initialBookId) {
        this();
        if (title != null && !title.isEmpty()) {
            this.title = title;
        }
        if (initialBookId != null && !initialBookId.isEmpty()) {
            this.bookIds.add(initialBookId);
            this.activeBookId = initialBookId;
        }
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<String> getBookIds() {
        if (bookIds == null) {
            bookIds = new ArrayList<>();
        }
        return bookIds;
    }

    public void setBookIds(List<String> bookIds) {
        this.bookIds = bookIds;
    }

    public String getActiveBookId() {
        return activeBookId;
    }

    public void setActiveBookId(String activeBookId) {
        this.activeBookId = activeBookId;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public void addBookId(String bookId) {
        if (bookIds == null) {
            bookIds = new ArrayList<>();
        }
        if (!bookIds.contains(bookId)) {
            bookIds.add(bookId);
        }
        if (activeBookId == null || activeBookId.isEmpty()) {
            activeBookId = bookId;
        }
    }
}

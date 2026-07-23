package com.example.fonoss.data.model;

import java.io.Serializable;

public class Bookmark implements Serializable {
    private String id;
    private String bookId;
    private int chapterIndex;
    private int scrollY;
    private String selectedText;
    private int startOffset = -1;
    private int endOffset = -1;
    private int audioPosition = -1;
    private String note;
    private long timestamp;

    public Bookmark() {
        // Required for Firebase
    }

    public Bookmark(String id, String bookId, int chapterIndex, int scrollY, String selectedText, long timestamp) {
        this.id = id;
        this.bookId = bookId;
        this.chapterIndex = chapterIndex;
        this.scrollY = scrollY;
        this.selectedText = selectedText;
        this.timestamp = timestamp;
    }

    public Bookmark(String id, String bookId, int chapterIndex, int scrollY, String selectedText, int startOffset, int endOffset, long timestamp) {
        this(id, bookId, chapterIndex, scrollY, selectedText, timestamp);
        this.startOffset = startOffset;
        this.endOffset = endOffset;
    }

    public Bookmark(String id, String bookId, int audioPosition, String note, long timestamp) {
        this.id = id;
        this.bookId = bookId;
        this.audioPosition = audioPosition;
        this.note = note;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBookId() { return bookId; }
    public void setBookId(String bookId) { this.bookId = bookId; }

    public int getChapterIndex() { return chapterIndex; }
    public void setChapterIndex(int chapterIndex) { this.chapterIndex = chapterIndex; }

    public int getScrollY() { return scrollY; }
    public void setScrollY(int scrollY) { this.scrollY = scrollY; }

    public String getSelectedText() { return selectedText; }
    public void setSelectedText(String selectedText) { this.selectedText = selectedText; }

    public int getStartOffset() { return startOffset; }
    public void setStartOffset(int startOffset) { this.startOffset = startOffset; }

    public int getEndOffset() { return endOffset; }
    public void setEndOffset(int endOffset) { this.endOffset = endOffset; }

    public int getAudioPosition() { return audioPosition; }
    public void setAudioPosition(int audioPosition) { this.audioPosition = audioPosition; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public boolean isAudioBookmark() { return audioPosition >= 0; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}


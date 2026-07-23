package com.example.fonoss.manager;

import com.example.fonoss.data.model.Book;
import com.example.fonoss.data.recommendation.RecommendationEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QueueManager {

    public enum QueueMode {
        PLAYLIST,
        SERIES,
        LIBRARY,
        RECOMMENDED
    }

    private final List<Book> queueList = new ArrayList<>();
    private int currentIndex = 0;
    private QueueMode mode = QueueMode.RECOMMENDED;
    private boolean isShuffle = false;
    private boolean isRepeat = false;

    public synchronized void setQueue(List<Book> books, int startIndex, QueueMode mode) {
        queueList.clear();
        if (books != null && !books.isEmpty()) {
            queueList.addAll(books);
        }
        if (startIndex >= 0 && startIndex < queueList.size()) {
            this.currentIndex = startIndex;
        } else {
            this.currentIndex = 0;
        }
        this.mode = mode != null ? mode : QueueMode.RECOMMENDED;
    }

    public synchronized void setQueueFromBook(Book targetBook, List<Book> allBooks, RecommendationEngine.UserProfileContext context) {
        queueList.clear();
        this.currentIndex = 0;
        if (targetBook == null) return;

        queueList.add(targetBook);

        // 1. Check if book is part of a series
        if (allBooks != null && !allBooks.isEmpty()) {
            List<Book> seriesBooks = RecommendationEngine.getSeriesBooks(targetBook, allBooks);
            if (seriesBooks != null && !seriesBooks.isEmpty()) {
                for (Book b : seriesBooks) {
                    if (b != null && b.getId() != null && !b.getId().equals(targetBook.getId())) {
                        queueList.add(b);
                    }
                }
                this.mode = QueueMode.SERIES;
                return;
            }
        }

        // 2. Fallback to recommendation engine
        if (allBooks != null && !allBooks.isEmpty() && context != null) {
            RecommendationEngine.RecommendationResult result = RecommendationEngine.getTopRecommendations(allBooks, context);
            if (result != null && result.getTopRecommended() != null) {
                for (Book b : result.getTopRecommended()) {
                    if (b != null && b.getId() != null && !b.getId().equals(targetBook.getId())) {
                        queueList.add(b);
                    }
                }
            }
        }
        this.mode = QueueMode.RECOMMENDED;
    }

    public synchronized Book getCurrentBook() {
        if (queueList.isEmpty() || currentIndex < 0 || currentIndex >= queueList.size()) {
            return null;
        }
        return queueList.get(currentIndex);
    }

    public synchronized Book getNextBook(RecommendationEngine.UserProfileContext context, List<Book> allBooks) {
        if (queueList.isEmpty()) return null;

        if (currentIndex < queueList.size() - 1) {
            currentIndex++;
            return queueList.get(currentIndex);
        }

        if (isRepeat) {
            currentIndex = 0;
            return queueList.get(0);
        }

        // Strict Playlist Mode: Stop playback at end of playlist
        if (mode == QueueMode.PLAYLIST) {
            return null;
        }

        // Auto-queue recommendations when reaching end of list (for RECOMMENDED / SERIES mode)
        if (allBooks != null && !allBooks.isEmpty()) {
            Book lastBook = getCurrentBook();
            if (context == null) context = new RecommendationEngine.UserProfileContext();
            if (lastBook != null) context.setLastListenedBook(lastBook);

            RecommendationEngine.RecommendationResult result = RecommendationEngine.getTopRecommendations(allBooks, context);
            if (result != null && result.getTopRecommended() != null) {
                List<Book> added = new ArrayList<>();
                for (Book b : result.getTopRecommended()) {
                    if (b != null && b.getId() != null && !containsBook(b.getId())) {
                        added.add(b);
                    }
                }
                if (!added.isEmpty()) {
                    queueList.addAll(added);
                    currentIndex++;
                    return queueList.get(currentIndex);
                }
            }
        }

        return null;
    }

    public synchronized Book getPreviousBook(int currentPositionSeconds) {
        if (queueList.isEmpty()) return null;

        // If played for more than 5 seconds, restart current book
        if (currentPositionSeconds > 5) {
            return getCurrentBook();
        }

        if (currentIndex > 0) {
            currentIndex--;
            return queueList.get(currentIndex);
        }

        return getCurrentBook();
    }

    public synchronized void addToQueue(Book book) {
        if (book == null || book.getId() == null) return;
        if (!containsBook(book.getId())) {
            queueList.add(book);
        }
    }

    public synchronized void removeFromQueue(int index) {
        if (index >= 0 && index < queueList.size()) {
            queueList.remove(index);
            if (currentIndex >= queueList.size()) {
                currentIndex = Math.max(0, queueList.size() - 1);
            }
        }
    }

    private boolean containsBook(String bookId) {
        if (bookId == null) return false;
        for (Book b : queueList) {
            if (b != null && bookId.equals(b.getId())) return true;
        }
        return false;
    }

    public synchronized List<Book> getQueueList() {
        return new ArrayList<>(queueList);
    }

    public synchronized int getCurrentIndex() {
        return currentIndex;
    }

    public synchronized void setCurrentIndex(int index) {
        if (index >= 0 && index < queueList.size()) {
            this.currentIndex = index;
        }
    }

    public synchronized QueueMode getMode() { return mode; }
    public synchronized void setMode(QueueMode mode) { this.mode = mode; }

    public synchronized boolean isShuffle() { return isShuffle; }
    public synchronized void setShuffle(boolean shuffle) {
        this.isShuffle = shuffle;
        if (shuffle && queueList.size() > 1) {
            Book current = getCurrentBook();
            queueList.remove(current);
            Collections.shuffle(queueList);
            if (current != null) {
                queueList.add(0, current);
                currentIndex = 0;
            }
        }
    }

    public synchronized boolean isRepeat() { return isRepeat; }
    public synchronized void setRepeat(boolean repeat) { this.isRepeat = repeat; }
}

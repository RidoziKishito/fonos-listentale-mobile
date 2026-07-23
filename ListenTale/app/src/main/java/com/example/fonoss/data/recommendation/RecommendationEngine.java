package com.example.fonoss.data.recommendation;

import com.example.fonoss.data.model.Book;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * RecommendationEngine: Core logic utility for calculating book recommendation scores
 * and retrieving same-series books for ListenTale.
 */
public class RecommendationEngine {

    private static final Random random = new Random();

    // Base score weights
    public static final double WEIGHT_GENRE = 35.0;
    public static final double WEIGHT_AUTHOR = 25.0;
    public static final double WEIGHT_SAVED = 20.0;
    public static final double WEIGHT_UNREAD = 15.0;
    public static final double PENALTY_COMPLETED = -100.0;

    // 25% Noise Factor Range for Dynamic Randomness (Method A)
    public static final double MAX_NOISE_RANGE = 25.0;

    public static class UserProfileContext {
        private List<String> favoriteGenres = new ArrayList<>();
        private Set<String> savedBookIds = new HashSet<>();
        private Set<String> inProgressBookIds = new HashSet<>();
        private Set<String> completedBookIds = new HashSet<>();
        private Book lastListenedBook;

        public UserProfileContext() {}

        public UserProfileContext(List<String> favoriteGenres,
                                 Set<String> savedBookIds,
                                 Set<String> inProgressBookIds,
                                 Set<String> completedBookIds,
                                 Book lastListenedBook) {
            if (favoriteGenres != null) this.favoriteGenres = favoriteGenres;
            if (savedBookIds != null) this.savedBookIds = savedBookIds;
            if (inProgressBookIds != null) this.inProgressBookIds = inProgressBookIds;
            if (completedBookIds != null) this.completedBookIds = completedBookIds;
            this.lastListenedBook = lastListenedBook;
        }

        public List<String> getFavoriteGenres() { return favoriteGenres; }
        public void setFavoriteGenres(List<String> favoriteGenres) { this.favoriteGenres = favoriteGenres; }

        public Set<String> getSavedBookIds() { return savedBookIds; }
        public void setSavedBookIds(Set<String> savedBookIds) { this.savedBookIds = savedBookIds; }

        public Set<String> getInProgressBookIds() { return inProgressBookIds; }
        public void setInProgressBookIds(Set<String> inProgressBookIds) { this.inProgressBookIds = inProgressBookIds; }

        public Set<String> getCompletedBookIds() { return completedBookIds; }
        public void setCompletedBookIds(Set<String> completedBookIds) { this.completedBookIds = completedBookIds; }

        public Book getLastListenedBook() { return lastListenedBook; }
        public void setLastListenedBook(Book lastListenedBook) { this.lastListenedBook = lastListenedBook; }
    }

    public static class RecommendationResult {
        private final List<Book> topRecommended;
        private final List<Book> inProgressBooks;
        private final List<Book> unreadDiscoveryBooks;

        public RecommendationResult(List<Book> topRecommended,
                                    List<Book> inProgressBooks,
                                    List<Book> unreadDiscoveryBooks) {
            this.topRecommended = topRecommended != null ? topRecommended : new ArrayList<>();
            this.inProgressBooks = inProgressBooks != null ? inProgressBooks : new ArrayList<>();
            this.unreadDiscoveryBooks = unreadDiscoveryBooks != null ? unreadDiscoveryBooks : new ArrayList<>();
        }

        public List<Book> getTopRecommended() { return topRecommended; }
        public List<Book> getInProgressBooks() { return inProgressBooks; }
        public List<Book> getUnreadDiscoveryBooks() { return unreadDiscoveryBooks; }
    }

    /**
     * Computes Top 15 recommended candidate books using Rule-Based Scoring + 25% Dynamic Noise Factor.
     */
    public static RecommendationResult getTopRecommendations(List<Book> allBooks, UserProfileContext context) {
        if (allBooks == null || allBooks.isEmpty()) {
            return new RecommendationResult(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }

        if (context == null) {
            context = new UserProfileContext();
        }

        List<BookScored> scoredBooks = new ArrayList<>();
        List<Book> inProgressList = new ArrayList<>();
        List<Book> unreadList = new ArrayList<>();

        for (Book book : allBooks) {
            if (book == null || book.getId() == null) continue;

            String bookId = book.getId();

            // 1. Separate in-progress books
            if (context.getInProgressBookIds().contains(bookId)) {
                inProgressList.add(book);
            }

            // 2. Calculate Base Score
            double baseScore = calculateBaseScore(book, context);

            // 3. Apply 25% Dynamic Noise Factor (Method A)
            // Adding a random noise in range [0.0, 25.0]
            double noise = random.nextDouble() * MAX_NOISE_RANGE;
            double finalScore = baseScore + noise;

            scoredBooks.add(new BookScored(book, finalScore));

            // Separate unread discovery books
            if (!context.getInProgressBookIds().contains(bookId) && !context.getCompletedBookIds().contains(bookId)) {
                unreadList.add(book);
            }
        }

        // Sort books by final score descending
        Collections.sort(scoredBooks, (b1, b2) -> Double.compare(b2.finalScore, b1.finalScore));

        // Extract Top 15 candidate books
        List<Book> top15 = new ArrayList<>();
        int limit = Math.min(15, scoredBooks.size());
        for (int i = 0; i < limit; i++) {
            top15.add(scoredBooks.get(i).book);
        }

        return new RecommendationResult(top15, inProgressList, unreadList);
    }

    /**
     * Calculates Base Score for a book based on user profile context.
     */
    public static double calculateBaseScore(Book book, UserProfileContext context) {
        double score = 0.0;
        String bookId = book.getId();

        // Penalty if already completed
        if (context.getCompletedBookIds().contains(bookId)) {
            return PENALTY_COMPLETED;
        }

        // Match Genre (with last listened book or favorite genres)
        boolean genreMatched = false;
        Book lastBook = context.getLastListenedBook();

        if (lastBook != null) {
            if (isSameGenre(book, lastBook)) {
                score += WEIGHT_GENRE;
                genreMatched = true;
            }
        }

        if (!genreMatched && context.getFavoriteGenres() != null && !context.getFavoriteGenres().isEmpty()) {
            if (matchesUserFavoriteGenres(book, context.getFavoriteGenres())) {
                score += WEIGHT_GENRE;
            }
        }

        // Match Author (with last listened book)
        if (lastBook != null && book.getAuthor() != null && lastBook.getAuthor() != null) {
            if (book.getAuthor().equalsIgnoreCase(lastBook.getAuthor())) {
                score += WEIGHT_AUTHOR;
            }
        }

        // Match Favorite (saved list)
        if (context.getSavedBookIds().contains(bookId)) {
            score += WEIGHT_SAVED;
        }

        // Unread bonus
        if (!context.getInProgressBookIds().contains(bookId) && !context.getCompletedBookIds().contains(bookId)) {
            score += WEIGHT_UNREAD;
        }

        // Bonus rating weight (0 - 5 stars)
        score += book.getRating();

        return score;
    }

    /**
     * Retrieves books in the SAME SERIES for a given target book.
     * Fallback to same Author or same Genre if series has fewer than 3 books.
     */
    public static List<Book> getSeriesBooks(Book targetBook, List<Book> allBooks) {
        if (targetBook == null || allBooks == null || allBooks.isEmpty()) {
            return new ArrayList<>();
        }

        List<Book> seriesBooks = new ArrayList<>();
        List<Book> authorOrGenreFallback = new ArrayList<>();

        String targetSeries = targetBook.getSeries();
        String targetAuthor = targetBook.getAuthor();

        for (Book book : allBooks) {
            if (book == null || book.getId() == null || book.getId().equals(targetBook.getId())) {
                continue;
            }

            // Check Series match
            if (targetSeries != null && !targetSeries.trim().isEmpty() && book.getSeries() != null) {
                if (book.getSeries().equalsIgnoreCase(targetSeries.trim())) {
                    seriesBooks.add(book);
                    continue;
                }
            }

            // Check Fallback (Same Author or Same Genre)
            if (targetAuthor != null && book.getAuthor() != null && book.getAuthor().equalsIgnoreCase(targetAuthor)) {
                authorOrGenreFallback.add(book);
            } else if (isSameGenre(book, targetBook)) {
                authorOrGenreFallback.add(book);
            }
        }

        // Fallback if series list is too short (< 3 books)
        if (seriesBooks.size() < 3) {
            for (Book fallback : authorOrGenreFallback) {
                if (!seriesBooks.contains(fallback)) {
                    seriesBooks.add(fallback);
                }
                if (seriesBooks.size() >= 6) break;
            }
        }

        return seriesBooks;
    }

    private static boolean isSameGenre(Book b1, Book b2) {
        if (b1.getGenre() != null && b2.getGenre() != null && b1.getGenre().equalsIgnoreCase(b2.getGenre())) {
            return true;
        }

        if (b1.getGenres() != null && b2.getGenres() != null) {
            for (String g1 : b1.getGenres()) {
                for (String g2 : b2.getGenres()) {
                    if (g1 != null && g1.equalsIgnoreCase(g2)) return true;
                }
            }
        }
        return false;
    }

    private static boolean matchesUserFavoriteGenres(Book book, List<String> favoriteGenres) {
        if (book.getGenre() != null) {
            for (String fav : favoriteGenres) {
                if (fav != null && fav.equalsIgnoreCase(book.getGenre())) return true;
            }
        }
        if (book.getGenres() != null) {
            for (String bg : book.getGenres()) {
                for (String fav : favoriteGenres) {
                    if (bg != null && bg.equalsIgnoreCase(fav)) return true;
                }
            }
        }
        return false;
    }

    private static class BookScored {
        final Book book;
        final double finalScore;

        BookScored(Book book, double finalScore) {
            this.book = book;
            this.finalScore = finalScore;
        }
    }
}

package com.example.fonoss;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.google.firebase.firestore.DocumentReference;

public class LibraryViewModel extends ViewModel {
    private final MutableLiveData<List<Book>> savedBooks = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Book>> downloadedBooks = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Book>> inProgressBooks = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Book>> completedBooks = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Map<String, Long>> bookProgressPos = new MutableLiveData<>(new HashMap<>());
    private final MutableLiveData<Map<String, Long>> bookProgressChapter = new MutableLiveData<>(new HashMap<>());
    private final MutableLiveData<List<Bookmark>> bookmarks = new MutableLiveData<>(new ArrayList<>());
    private final Set<String> pendingDeletedDownloadIds = new HashSet<>();

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();

    public LiveData<List<Book>> getSavedBooks() { return savedBooks; }
    public LiveData<List<Book>> getDownloadedBooks() { return downloadedBooks; }
    public LiveData<List<Book>> getInProgressBooks() { return inProgressBooks; }
    public LiveData<List<Book>> getCompletedBooks() { return completedBooks; }
    public LiveData<Map<String, Long>> getBookProgressPos() { return bookProgressPos; }
    public LiveData<Map<String, Long>> getBookProgressChapter() { return bookProgressChapter; }
    public LiveData<List<Bookmark>> getBookmarks() { return bookmarks; }
    public LiveData<Map<String, Integer>> getDownloadProgress() {
        return DownloadQueueManager.getInstance(mAuth.getApp().getApplicationContext()).getProgressMap();
    }

    public void fetchLibraryData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("users").document(user.getUid())
                .addSnapshotListener((snapshot, e) -> {
                    if (snapshot != null && snapshot.exists()) {
                        updateListFromMap(snapshot, "saved", savedBooks);
                        updateListFromMap(snapshot, "downloaded", downloadedBooks);
                        updateListFromMap(snapshot, "inProgress", inProgressBooks);
                        updateListFromMap(snapshot, "completed", completedBooks);
                        reconcilePendingDeletedDownloads(snapshot);
                        
                        Object posObj = snapshot.get("progressPositions");
                        if (posObj instanceof Map) bookProgressPos.setValue((Map<String, Long>) posObj);
                        
                        Object chapterObj = snapshot.get("progressChapters");
                        if (chapterObj instanceof Map) bookProgressChapter.setValue((Map<String, Long>) chapterObj);

                        autoSyncDownloads();
                    }
                });
    }

    private void autoSyncDownloads() {
        List<Book> remoteDownloaded = downloadedBooks.getValue();
        if (remoteDownloaded == null) return;
        for (Book book : remoteDownloaded) {
            if (book == null || book.getId() == null || pendingDeletedDownloadIds.contains(book.getId())) continue;
            String fileName = "book_" + book.getId() + ".dat";
            java.io.File file = mAuth.getApp().getApplicationContext().getFileStreamPath(fileName);
            if (!file.exists()) {
                db.collection("books").document(book.getId()).get().addOnSuccessListener(doc -> {
                    Book fullBook = doc.toObject(Book.class);
                    if (fullBook != null) {
                        fullBook.setId(doc.getId());
                        saveBookToInternalStorage(fullBook);
                    }
                });
            }
        }
    }

    public void clearLocalDownloads() {
        android.content.Context context = mAuth.getApp().getApplicationContext();
        String[] files = context.fileList();
        if (files != null) {
            for (String fileName : files) {
                if (fileName.startsWith("book_") && fileName.endsWith(".dat")) {
                    context.deleteFile(fileName);
                }
            }
        }
        
        java.io.File musicDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC);
        if (musicDir != null && musicDir.exists()) {
            java.io.File[] mp3Files = musicDir.listFiles();
            if (mp3Files != null) {
                for (java.io.File file : mp3Files) {
                    if (file.getName().startsWith("audio_") && file.getName().endsWith(".mp3")) {
                        file.delete();
                    }
                }
            }
        }
    }

    private void updateListFromMap(com.google.firebase.firestore.DocumentSnapshot snapshot, String key, MutableLiveData<List<Book>> liveData) {
        Object data = snapshot.get(key);
        List<Book> books = new ArrayList<>();
        if (data instanceof List) {
            List<Map<String, Object>> list = (List<Map<String, Object>>) data;
            for (Map<String, Object> map : list) {
                Object rating = map.get("rating");
                double dRating = 0.0;
                if (rating instanceof Double) dRating = (Double) rating;
                else if (rating instanceof Long) dRating = ((Long) rating).doubleValue();

                String genre = (String) map.get("genre");
                if (genre == null) genre = (String) map.get("gender");

                books.add(new Book(
                        (String) map.get("id"), (String) map.get("title"), (String) map.get("author"),
                        genre, (String) map.get("description"), (String) map.get("duration"),
                        (String) map.get("pages"), dRating, (String) map.get("coverUrl"),
                        (List<String>) map.get("chapters")
                ));
            }
        }
        if ("downloaded".equals(key) && !pendingDeletedDownloadIds.isEmpty()) {
            List<Book> filteredBooks = new ArrayList<>();
            for (Book book : books) {
                if (book != null && !pendingDeletedDownloadIds.contains(book.getId())) {
                    filteredBooks.add(book);
                }
            }
            books = filteredBooks;
        }
        liveData.setValue(books);
    }

    public void toggleFavorite(Book book) { updateLibraryItem("saved", book, !isFavorite(book.getId())); }
    public void addDownload(Book book) { updateLibraryItem("downloaded", book, true); }

    public void enqueueSequentialDownloads(List<Book> books) {
        if (books == null || books.isEmpty() || mAuth.getCurrentUser() == null) return;
        for (Book book : books) {
            if (book != null && book.getId() != null) pendingDeletedDownloadIds.remove(book.getId());
        }
        DownloadQueueManager.getInstance(mAuth.getApp().getApplicationContext())
                .enqueue(books, mAuth.getCurrentUser().getUid());
    }

    public void cancelDownload(String bookId) {
        if (bookId == null) return;
        DownloadQueueManager.getInstance(mAuth.getApp().getApplicationContext()).cancel(bookId);
    }

    public void removeDownload(String bookId) {
        if (bookId == null) return;
        Book book = findInList(downloadedBooks.getValue(), bookId);
        pendingDeletedDownloadIds.add(bookId);
        cancelDownload(bookId);
        deleteBookFromInternalStorage(bookId);
        removeBookFromDownloadedLiveData(bookId);
        if (book != null) updateLibraryItem("downloaded", book, false);
        else removeFromCollectionManual("downloaded", bookId);
    }

    public void savePlaybackPosition(String bookId, int seconds) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        Map<String, Object> update = new HashMap<>();
        update.put("progressPositions." + bookId, (long)seconds);
        db.collection("users").document(user.getUid()).set(update, SetOptions.merge());
    }

    public void saveReadingChapter(String bookId, int chapter) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        Map<String, Object> update = new HashMap<>();
        update.put("progressChapters." + bookId, (long)chapter);
        db.collection("users").document(user.getUid()).set(update, SetOptions.merge());
    }

    public void fetchBookmarks(String bookId) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        bookmarks.setValue(new ArrayList<>());

        db.collection("users").document(user.getUid())
                .collection("bookmarks")
                .whereEqualTo("bookId", bookId)
                .addSnapshotListener((snapshot, e) -> {
                    if (snapshot != null) {
                        List<Bookmark> list = new ArrayList<>();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                            Bookmark bookmark = doc.toObject(Bookmark.class);
                            if (bookmark != null) {
                                bookmark.setId(doc.getId());
                                list.add(bookmark);
                            }
                        }
                        bookmarks.setValue(list);
                    }
                });
    }

    public void saveBookmark(Bookmark bookmark) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        DocumentReference ref = db.collection("users").document(user.getUid())
                .collection("bookmarks")
                .document();
        bookmark.setId(ref.getId());
        upsertLocalBookmark(bookmark);
        ref.set(bookmark);
    }

    public void deleteBookmark(String bookmarkId) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        removeLocalBookmark(bookmarkId);

        db.collection("users").document(user.getUid())
                .collection("bookmarks")
                .document(bookmarkId)
                .delete();
    }

    private void upsertLocalBookmark(Bookmark bookmark) {
        List<Bookmark> current = bookmarks.getValue();
        List<Bookmark> updated = current == null ? new ArrayList<>() : new ArrayList<>(current);
        for (int i = 0; i < updated.size(); i++) {
            if (bookmark.getId() != null && bookmark.getId().equals(updated.get(i).getId())) {
                updated.set(i, bookmark);
                bookmarks.setValue(updated);
                return;
            }
        }
        updated.add(bookmark);
        bookmarks.setValue(updated);
    }

    private void removeLocalBookmark(String bookmarkId) {
        if (bookmarkId == null) return;
        List<Bookmark> current = bookmarks.getValue();
        if (current == null) return;

        List<Bookmark> updated = new ArrayList<>();
        for (Bookmark bookmark : current) {
            if (!bookmarkId.equals(bookmark.getId())) {
                updated.add(bookmark);
            }
        }
        bookmarks.setValue(updated);
    }

    public void markAsInProgress(Book book) {
        if (book == null) return;
        if (isInList(completedBooks.getValue(), book.getId())) return;
        if (!isInList(inProgressBooks.getValue(), book.getId())) {
            updateLibraryItem("inProgress", book, true);
        }
    }

    public void markAsCompleted(Book book) {
        if (book == null) return;
        if (isInList(completedBooks.getValue(), book.getId())) return;
        removeFromCollectionManual("inProgress", book.getId());
        updateLibraryItem("completed", book, true);
    }

    public void deleteSpecificBooks(Map<Integer, List<String>> selections) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || selections == null) return;

        db.collection("users").document(user.getUid()).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                Map<String, Object> updates = new HashMap<>();
                String[] collectionKeys = {"saved", "inProgress", "downloaded", "completed"};

                for (Map.Entry<Integer, List<String>> entry : selections.entrySet()) {
                    int tabIndex = entry.getKey();
                    List<String> idsToRemove = entry.getValue();
                    if (tabIndex < 0 || tabIndex >= collectionKeys.length || idsToRemove.isEmpty()) continue;

                    String colName = collectionKeys[tabIndex];
                    List<Map<String, Object>> currentList = (List<Map<String, Object>>) snapshot.get(colName);
                    
                    if (currentList != null) {
                        List<Map<String, Object>> newList = new ArrayList<>();
                        for (Map<String, Object> item : currentList) {
                            String bookId = (String) item.get("id");
                            if (!idsToRemove.contains(bookId)) {
                                newList.add(item);
                            } else if (colName.equals("downloaded")) {
                                pendingDeletedDownloadIds.add(bookId);
                                cancelDownload(bookId);
                                // Chỉ xóa file nếu xóa từ mục Downloaded
                                deleteBookFromInternalStorage(bookId);
                                removeBookFromDownloadedLiveData(bookId);
                            }
                        }
                        updates.put(colName, newList);
                    }
                }

                if (!updates.isEmpty()) {
                    db.collection("users").document(user.getUid()).update(updates);
                }
            }
        });
    }

    private void removeFromCollectionManual(String collectionName, String bookId) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        db.collection("users").document(user.getUid()).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                List<Map<String, Object>> list = (List<Map<String, Object>>) snapshot.get(collectionName);
                if (list != null) {
                    List<Map<String, Object>> newList = new ArrayList<>();
                    for (Map<String, Object> item : list) {
                        if (!bookId.equals(item.get("id"))) newList.add(item);
                    }
                    db.collection("users").document(user.getUid()).update(collectionName, newList);
                }
            }
        });
    }

    private void updateLibraryItem(String collection, Book book, boolean add) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        if (collection.equals("downloaded")) {
            if (add) saveBookToInternalStorage(book);
            else {
                pendingDeletedDownloadIds.add(book.getId());
                cancelDownload(book.getId());
                deleteBookFromInternalStorage(book.getId());
                removeBookFromDownloadedLiveData(book.getId());
            }
        }
        if (add) {
            Map<String, Object> bookMap = new HashMap<>();
            bookMap.put("id", book.getId()); bookMap.put("title", book.getTitle());
            bookMap.put("author", book.getAuthor()); bookMap.put("genre", book.getGenre());
            bookMap.put("description", book.getDescription()); bookMap.put("duration", book.getDuration());
            bookMap.put("pages", book.getPages()); bookMap.put("rating", book.getRating());
            bookMap.put("coverUrl", book.getCoverUrl());
            db.collection("users").document(user.getUid()).update(collection, FieldValue.arrayUnion(bookMap));
        } else {
            removeFromCollectionManual(collection, book.getId());
        }
    }

    private void saveBookToInternalStorage(Book book) {
        try {
            LocalBookStorage.save(mAuth.getApp().getApplicationContext(), book);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void deleteBookFromInternalStorage(String bookId) {
        if (bookId == null) return;
        android.content.Context context = mAuth.getApp().getApplicationContext();
        context.deleteFile("book_" + bookId + ".dat");
        
        java.io.File musicDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC);
        if (musicDir != null) {
            java.io.File mp3File = new java.io.File(musicDir, "audio_" + bookId + ".mp3");
            if (mp3File.exists()) {
                mp3File.delete();
            }
            java.io.File tempFile = new java.io.File(musicDir, "audio_" + bookId + ".tmp");
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    private void removeBookFromDownloadedLiveData(String bookId) {
        List<Book> current = downloadedBooks.getValue();
        if (current == null) return;

        List<Book> updated = new ArrayList<>();
        for (Book book : current) {
            if (book != null && !bookId.equals(book.getId())) {
                updated.add(book);
            }
        }
        downloadedBooks.setValue(updated);
    }

    private void reconcilePendingDeletedDownloads(com.google.firebase.firestore.DocumentSnapshot snapshot) {
        if (pendingDeletedDownloadIds.isEmpty()) return;

        Object data = snapshot.get("downloaded");
        Set<String> remoteDownloadedIds = new HashSet<>();
        if (data instanceof List) {
            List<Map<String, Object>> list = (List<Map<String, Object>>) data;
            for (Map<String, Object> item : list) {
                Object id = item.get("id");
                if (id instanceof String) remoteDownloadedIds.add((String) id);
            }
        }

        List<String> completedDeletes = new ArrayList<>();
        for (String bookId : pendingDeletedDownloadIds) {
            if (!remoteDownloadedIds.contains(bookId)) {
                completedDeletes.add(bookId);
            }
        }
        pendingDeletedDownloadIds.removeAll(completedDeletes);
    }

    public Book loadBookLocally(String bookId) {
        try {
            return LocalBookStorage.load(mAuth.getApp().getApplicationContext(), bookId);
        } catch (Exception e) { return null; }
    }

    public boolean isFavorite(String bookId) { return isInList(savedBooks.getValue(), bookId); }
    public boolean isDownloaded(String bookId) { return isInList(downloadedBooks.getValue(), bookId); }
    private boolean isInList(List<Book> list, String id) {
        if (list == null) return false;
        for (Book b : list) if (b.getId().equals(id)) return true;
        return false;
    }
    private Book findInList(List<Book> list, String id) {
        if (list == null) return null;
        for (Book b : list) if (b.getId().equals(id)) return b;
        return null;
    }
}

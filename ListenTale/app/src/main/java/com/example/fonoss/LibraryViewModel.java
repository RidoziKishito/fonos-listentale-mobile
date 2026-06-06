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
import java.util.List;
import java.util.Map;

public class LibraryViewModel extends ViewModel {
    private final MutableLiveData<List<Book>> savedBooks = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Book>> downloadedBooks = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Book>> inProgressBooks = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Book>> completedBooks = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Map<String, Long>> bookProgressPos = new MutableLiveData<>(new HashMap<>());
    private final MutableLiveData<Map<String, Long>> bookProgressChapter = new MutableLiveData<>(new HashMap<>());

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();

    public LiveData<List<Book>> getSavedBooks() { return savedBooks; }
    public LiveData<List<Book>> getDownloadedBooks() { return downloadedBooks; }
    public LiveData<List<Book>> getInProgressBooks() { return inProgressBooks; }
    public LiveData<List<Book>> getCompletedBooks() { return completedBooks; }
    public LiveData<Map<String, Long>> getBookProgressPos() { return bookProgressPos; }
    public LiveData<Map<String, Long>> getBookProgressChapter() { return bookProgressChapter; }

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
        liveData.setValue(books);
    }

    public void toggleFavorite(Book book) { updateLibraryItem("saved", book, !isFavorite(book.getId())); }
    public void addDownload(Book book) { updateLibraryItem("downloaded", book, true); }
    public void removeDownload(String bookId) {
        Book book = findInList(downloadedBooks.getValue(), bookId);
        if (book != null) updateLibraryItem("downloaded", book, false);
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
                                // Chỉ xóa file nếu xóa từ mục Downloaded
                                deleteBookFromInternalStorage(bookId);
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
            else deleteBookFromInternalStorage(book.getId());
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
            java.io.FileOutputStream fos = mAuth.getApp().getApplicationContext().openFileOutput("book_" + book.getId() + ".dat", android.content.Context.MODE_PRIVATE);
            java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(fos);
            oos.writeObject(book);
            oos.close();
            fos.close();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void deleteBookFromInternalStorage(String bookId) {
        mAuth.getApp().getApplicationContext().deleteFile("book_" + bookId + ".dat");
    }

    public Book loadBookLocally(String bookId) {
        try {
            java.io.FileInputStream fis = mAuth.getApp().getApplicationContext().openFileInput("book_" + bookId + ".dat");
            java.io.ObjectInputStream ois = new java.io.ObjectInputStream(fis);
            Book book = (Book) ois.readObject();
            ois.close();
            fis.close();
            return book;
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

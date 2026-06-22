package com.example.fonoss.data.repository;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class BookRepository {
    private final FirebaseFirestore db;

    @Inject
    public BookRepository(FirebaseFirestore db) {
        this.db = db;
    }

    public DocumentReference getUserDocument(String uid) {
        return db.collection("users").document(uid);
    }

    public FirebaseFirestore getDb() {
        return db;
    }

    public DocumentReference getBookDocument(String bookId) {
        return db.collection("books").document(bookId);
    }

    public CollectionReference getBookmarksCollection(String uid) {
        return db.collection("users").document(uid).collection("bookmarks");
    }

    public Query getBookmarksForBook(String uid, String bookId) {
        return getBookmarksCollection(uid).whereEqualTo("bookId", bookId);
    }
}

package com.example.fonoss.data.repository;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class UserRepository {
    private final FirebaseAuth mAuth;
    private final FirebaseFirestore db;

    @Inject
    public UserRepository(FirebaseAuth mAuth, FirebaseFirestore db) {
        this.mAuth = mAuth;
        this.db = db;
    }

    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }

    public FirebaseAuth getAuth() {
        return mAuth;
    }

    public Task<DocumentSnapshot> getUserDocument(String uid) {
        return db.collection("users").document(uid).get();
    }

    public Task<Void> updateUserName(String uid, String newName) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", newName);
        return db.collection("users").document(uid).update(data);
    }

    public Task<Void> updateUserAvatar(String uid, String avatarBase64) {
        Map<String, Object> data = new HashMap<>();
        data.put("avatarUrl", avatarBase64);
        return db.collection("users").document(uid).update(data);
    }
}

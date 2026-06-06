package com.example.fonoss;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class UserViewModel extends ViewModel {
    private final MutableLiveData<String> userName = new MutableLiveData<>("Loading...");
    private final MutableLiveData<String> userEmail = new MutableLiveData<>("");
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();

    public LiveData<String> getUserName() {
        return userName;
    }

    public LiveData<String> getUserEmail() {
        return userEmail;
    }

    public void fetchUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            userEmail.setValue(user.getEmail());
            db.collection("users").document(user.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            userName.setValue(documentSnapshot.getString("name"));
                        }
                    });
        }
    }

    public void updateUserName(String newName) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            Map<String, Object> data = new HashMap<>();
            data.put("name", newName);
            db.collection("users").document(user.getUid())
                    .update(data)
                    .addOnSuccessListener(aVoid -> userName.setValue(newName));
        }
    }

    public void clearData() {
        userName.setValue("");
        userEmail.setValue("");
    }
}

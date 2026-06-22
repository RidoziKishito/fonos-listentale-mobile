package com.example.fonoss.ui.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;
import com.example.fonoss.data.repository.UserRepository;

@HiltViewModel
public class UserViewModel extends ViewModel {
    private final MutableLiveData<String> userName = new MutableLiveData<>("Loading...");
    private final MutableLiveData<String> userEmail = new MutableLiveData<>("");
    private final UserRepository userRepository;

    @Inject
    public UserViewModel(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public LiveData<String> getUserName() {
        return userName;
    }

    public LiveData<String> getUserEmail() {
        return userEmail;
    }

    public void fetchUserData() {
        FirebaseUser user = userRepository.getCurrentUser();
        if (user != null) {
            userEmail.setValue(user.getEmail());
            userRepository.getUserDocument(user.getUid())
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            userName.setValue(documentSnapshot.getString("name"));
                        }
                    });
        }
    }

    public void updateUserName(String newName) {
        FirebaseUser user = userRepository.getCurrentUser();
        if (user != null) {
            userRepository.updateUserName(user.getUid(), newName)
                    .addOnSuccessListener(aVoid -> userName.setValue(newName));
        }
    }

    public void clearData() {
        userName.setValue("");
        userEmail.setValue("");
    }
}


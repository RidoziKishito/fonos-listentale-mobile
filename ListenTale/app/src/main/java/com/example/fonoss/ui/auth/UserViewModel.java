package com.example.fonoss.ui.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import android.graphics.Bitmap;
import android.util.Base64;
import java.io.ByteArrayOutputStream;

import java.util.HashMap;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;
import com.example.fonoss.data.repository.UserRepository;
import org.json.JSONObject;

@HiltViewModel
public class UserViewModel extends ViewModel {
    private final MutableLiveData<String> userName = new MutableLiveData<>("Loading...");
    private final MutableLiveData<String> userEmail = new MutableLiveData<>("");
    private final MutableLiveData<String> userAvatar = new MutableLiveData<>("");
    private final MutableLiveData<String> accountType = new MutableLiveData<>("FREE");
    private final MutableLiveData<Long> upgradeDate = new MutableLiveData<>(null);
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

    public LiveData<String> getUserAvatar() {
        return userAvatar;
    }

    public LiveData<String> getAccountType() {
        return accountType;
    }

    public LiveData<Long> getUpgradeDate() {
        return upgradeDate;
    }

    public void fetchUserData() {
        FirebaseUser user = userRepository.getCurrentUser();
        if (user != null) {
            userEmail.setValue(user.getEmail());
            userRepository.getUserDocument(user.getUid())
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            userName.setValue(documentSnapshot.getString("name"));
                            if (documentSnapshot.contains("avatarUrl")) {
                                userAvatar.setValue(documentSnapshot.getString("avatarUrl"));
                            }
                            if (documentSnapshot.contains("accountType")) {
                                accountType.setValue(documentSnapshot.getString("accountType"));
                            }
                            if (documentSnapshot.contains("upgradeDate")) {
                                upgradeDate.setValue(documentSnapshot.getLong("upgradeDate"));
                            }
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

    public void uploadAvatar(Bitmap bitmap) {
        FirebaseUser user = userRepository.getCurrentUser();
        if (user == null || bitmap == null) return;

        // Compress image to Base64
        int MAX_SIZE = 256;
        float ratio = Math.min(
                (float) MAX_SIZE / bitmap.getWidth(),
                (float) MAX_SIZE / bitmap.getHeight()
        );
        int width = Math.round(ratio * bitmap.getWidth());
        int height = Math.round(ratio * bitmap.getHeight());
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, width, height, false);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        byte[] imageBytes = baos.toByteArray();
        String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);

        userRepository.updateUserAvatar(user.getUid(), base64Image)
                .addOnSuccessListener(aVoid -> userAvatar.setValue(base64Image));
    }

    public void removeAvatar() {
        FirebaseUser user = userRepository.getCurrentUser();
        if (user != null) {
            userRepository.updateUserAvatar(user.getUid(), "")
                    .addOnSuccessListener(aVoid -> userAvatar.setValue(""));
        }
    }

    public void startUpgradeRequest(UserRepository.UpgradeRequestCallback callback) {
        FirebaseUser user = userRepository.getCurrentUser();
        if (user == null) return;
        userRepository.createUpgradeRequest(user.getUid(), callback);
    }

    public com.google.firebase.firestore.ListenerRegistration listenToPaymentStatus(String paymentCode, com.google.android.gms.tasks.OnSuccessListener<Void> successListener) {
        return FirebaseFirestore.getInstance()
                .collection("upgrade_transactions")
                .document(paymentCode)
                .addSnapshotListener((snapshot, e) -> {
                    if (snapshot != null && snapshot.exists()) {
                        String status = snapshot.getString("status");
                        if ("SUCCESS".equals(status)) {
                            // Account update should be handled by backend, but we refresh local state
                            accountType.setValue("PREMIUM");
                            upgradeDate.setValue(System.currentTimeMillis());
                            successListener.onSuccess(null);
                        }
                    }
                });
    }

    public void verifyAndUpgrade(com.google.android.gms.tasks.OnSuccessListener<Void> successListener, 
                                 com.google.android.gms.tasks.OnFailureListener failureListener) {
        FirebaseUser user = userRepository.getCurrentUser();
        if (user == null) return;

        // Simulate backend verification delay
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            userRepository.upgradeToPremium(user.getUid())
                    .addOnSuccessListener(aVoid -> {
                        accountType.setValue("PREMIUM");
                        upgradeDate.setValue(System.currentTimeMillis());
                        successListener.onSuccess(null);
                    })
                    .addOnFailureListener(failureListener);
        }, 2000);
    }

    public void clearData() {
        userName.setValue("");
        userEmail.setValue("");
        userAvatar.setValue("");
        accountType.setValue("FREE");
        upgradeDate.setValue(null);
    }
}


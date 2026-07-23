package com.example.fonoss.ui.book;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.fonoss.data.repository.RatingRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class RatingViewModel extends ViewModel {

    private final RatingRepository ratingRepository;
    private final FirebaseAuth auth;

    private final MutableLiveData<Double> userRating = new MutableLiveData<>(null);
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isOperationSuccess = new MutableLiveData<>(false);

    @Inject
    public RatingViewModel(RatingRepository ratingRepository) {
        this.ratingRepository = ratingRepository;
        this.auth = FirebaseAuth.getInstance();
    }

    public LiveData<Double> getUserRating() {
        return userRating;
    }

    public LiveData<String> getToastMessage() {
        return toastMessage;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<Boolean> getIsOperationSuccess() {
        return isOperationSuccess;
    }

    public void clearToastMessage() {
        toastMessage.setValue(null);
    }

    public void resetOperationState() {
        isOperationSuccess.setValue(false);
    }

    public String getCurrentUserId() {
        FirebaseUser currentUser = auth.getCurrentUser();
        return currentUser != null ? currentUser.getUid() : null;
    }

    public void loadUserRating(String bookId) {
        String userId = getCurrentUserId();
        if (userId == null || bookId == null) {
            userRating.setValue(null);
            return;
        }

        ratingRepository.getUserRating(userId, bookId)
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Double val = snapshot.getDouble("rating");
                        userRating.setValue(val);
                    } else {
                        userRating.setValue(null);
                    }
                })
                .addOnFailureListener(e -> userRating.setValue(null));
    }

    public void submitRating(String bookId, double ratingValue) {
        String userId = getCurrentUserId();
        if (userId == null) {
            toastMessage.setValue("Please log in to rate this book.");
            return;
        }

        if (ratingValue < 1.0 || ratingValue > 5.0) {
            toastMessage.setValue("Please select a rating between 1 and 5 stars.");
            return;
        }

        isLoading.setValue(true);
        Double existingRating = userRating.getValue();

        if (existingRating != null) {
            // Update existing rating
            ratingRepository.updateRating(userId, bookId, ratingValue)
                    .addOnSuccessListener(aVoid -> {
                        isLoading.setValue(false);
                        userRating.setValue(ratingValue);
                        toastMessage.setValue("Rating updated successfully!");
                        isOperationSuccess.setValue(true);
                    })
                    .addOnFailureListener(e -> {
                        isLoading.setValue(false);
                        toastMessage.setValue("Failed to update rating: " + e.getMessage());
                    });
        } else {
            // Add new rating
            ratingRepository.addRating(userId, bookId, ratingValue)
                    .addOnSuccessListener(aVoid -> {
                        isLoading.setValue(false);
                        userRating.setValue(ratingValue);
                        toastMessage.setValue("Rating submitted successfully!");
                        isOperationSuccess.setValue(true);
                    })
                    .addOnFailureListener(e -> {
                        isLoading.setValue(false);
                        toastMessage.setValue("Failed to submit rating: " + e.getMessage());
                    });
        }
    }

    public void deleteRating(String bookId) {
        String userId = getCurrentUserId();
        if (userId == null) {
            toastMessage.setValue("Please log in to rate this book.");
            return;
        }

        isLoading.setValue(true);
        ratingRepository.deleteRating(userId, bookId)
                .addOnSuccessListener(aVoid -> {
                    isLoading.setValue(false);
                    userRating.setValue(null);
                    toastMessage.setValue("Rating removed successfully!");
                    isOperationSuccess.setValue(true);
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    toastMessage.setValue("Failed to remove rating: " + e.getMessage());
                });
    }
}

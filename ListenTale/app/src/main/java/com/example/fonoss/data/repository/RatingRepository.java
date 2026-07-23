package com.example.fonoss.data.repository;

import android.util.Log;

import com.example.fonoss.data.model.Rating;
import com.example.fonoss.data.network.SupabaseApiService;
import com.example.fonoss.data.network.SupabaseConfig;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Singleton
public class RatingRepository {

    private static final String TAG = "RatingRepository";
    private final FirebaseFirestore db;

    @Inject
    public RatingRepository(FirebaseFirestore db) {
        this.db = db;
    }

    /**
     * Lấy đánh giá của một User cho một cuốn sách cụ thể
     */
    public Task<DocumentSnapshot> getUserRating(String userId, String bookId) {
        String docId = Rating.generateDocumentId(userId, bookId);
        return db.collection("ratings").document(docId).get();
    }

    /**
     * ADD: Thêm mới đánh giá của user đối với cuốn sách (dùng Transaction chống Race Condition)
     */
    public Task<Void> addRating(String userId, String bookId, double ratingValue) {
        String docId = Rating.generateDocumentId(userId, bookId);
        DocumentReference ratingRef = db.collection("ratings").document(docId);
        DocumentReference bookRef = db.collection("books").document(bookId);

        Task<Void> task = db.runTransaction((com.google.firebase.firestore.Transaction.Function<Void>) transaction -> {
            DocumentSnapshot ratingSnapshot = transaction.get(ratingRef);
            if (ratingSnapshot.exists()) {
                throw new FirebaseFirestoreException("User đã đánh giá sách này rồi.",
                        FirebaseFirestoreException.Code.ALREADY_EXISTS);
            }

            DocumentSnapshot bookSnapshot = transaction.get(bookRef);
            if (!bookSnapshot.exists()) {
                throw new FirebaseFirestoreException("Sách không tồn tại.",
                        FirebaseFirestoreException.Code.NOT_FOUND);
            }

            Double currentRatingObj = bookSnapshot.getDouble("rating");
            double oldRating = currentRatingObj != null ? currentRatingObj : 0.0;

            Long currentCountObj = bookSnapshot.getLong("rating_count");
            if (currentCountObj == null) {
                currentCountObj = bookSnapshot.getLong("ratingCount");
            }
            long oldCount = currentCountObj != null ? currentCountObj : 0;

            long newCount = oldCount + 1;
            double newRating = ((oldRating * oldCount) + ratingValue) / newCount;
            newRating = Math.round(newRating * 10.0) / 10.0;

            Map<String, Object> ratingData = new HashMap<>();
            ratingData.put("userId", userId);
            ratingData.put("bookId", bookId);
            ratingData.put("rating", ratingValue);
            ratingData.put("createdAt", FieldValue.serverTimestamp());
            ratingData.put("updatedAt", FieldValue.serverTimestamp());

            transaction.set(ratingRef, ratingData);

            Map<String, Object> bookUpdates = new HashMap<>();
            bookUpdates.put("rating", newRating);
            bookUpdates.put("rating_count", newCount);
            transaction.update(bookRef, bookUpdates);

            return null;
        });

        task.addOnSuccessListener(aVoid -> syncBookToSupabase(bookId));
        return task;
    }

    /**
     * UPDATE: Cập nhật lại số điểm đánh giá (dùng Transaction chống Race Condition)
     */
    public Task<Void> updateRating(String userId, String bookId, double newRatingValue) {
        String docId = Rating.generateDocumentId(userId, bookId);
        DocumentReference ratingRef = db.collection("ratings").document(docId);
        DocumentReference bookRef = db.collection("books").document(bookId);

        Task<Void> task = db.runTransaction((com.google.firebase.firestore.Transaction.Function<Void>) transaction -> {
            DocumentSnapshot ratingSnapshot = transaction.get(ratingRef);
            if (!ratingSnapshot.exists()) {
                throw new FirebaseFirestoreException("Chưa có đánh giá nào để cập nhật.",
                        FirebaseFirestoreException.Code.NOT_FOUND);
            }

            Double oldRatingValueObj = ratingSnapshot.getDouble("rating");
            double oldRatingValue = oldRatingValueObj != null ? oldRatingValueObj : 0.0;

            DocumentSnapshot bookSnapshot = transaction.get(bookRef);
            if (!bookSnapshot.exists()) {
                throw new FirebaseFirestoreException("Sách không tồn tại.",
                        FirebaseFirestoreException.Code.NOT_FOUND);
            }

            Double currentBookRatingObj = bookSnapshot.getDouble("rating");
            double oldBookRating = currentBookRatingObj != null ? currentBookRatingObj : 0.0;

            Long currentCountObj = bookSnapshot.getLong("rating_count");
            if (currentCountObj == null) {
                currentCountObj = bookSnapshot.getLong("ratingCount");
            }
            long oldCount = currentCountObj != null ? currentCountObj : 0;

            double newRating = oldCount > 0 ? ((oldBookRating * oldCount) - oldRatingValue + newRatingValue) / oldCount : newRatingValue;
            newRating = Math.round(newRating * 10.0) / 10.0;

            Map<String, Object> ratingUpdates = new HashMap<>();
            ratingUpdates.put("rating", newRatingValue);
            ratingUpdates.put("updatedAt", FieldValue.serverTimestamp());

            transaction.update(ratingRef, ratingUpdates);

            Map<String, Object> bookUpdates = new HashMap<>();
            bookUpdates.put("rating", newRating);
            transaction.update(bookRef, bookUpdates);

            return null;
        });

        task.addOnSuccessListener(aVoid -> syncBookToSupabase(bookId));
        return task;
    }

    /**
     * DELETE: Xóa đánh giá (dùng Transaction chống Race Condition)
     */
    public Task<Void> deleteRating(String userId, String bookId) {
        String docId = Rating.generateDocumentId(userId, bookId);
        DocumentReference ratingRef = db.collection("ratings").document(docId);
        DocumentReference bookRef = db.collection("books").document(bookId);

        Task<Void> task = db.runTransaction((com.google.firebase.firestore.Transaction.Function<Void>) transaction -> {
            DocumentSnapshot ratingSnapshot = transaction.get(ratingRef);
            if (!ratingSnapshot.exists()) {
                throw new FirebaseFirestoreException("Không tìm thấy đánh giá để xóa.",
                        FirebaseFirestoreException.Code.NOT_FOUND);
            }

            Double oldRatingValueObj = ratingSnapshot.getDouble("rating");
            double oldRatingValue = oldRatingValueObj != null ? oldRatingValueObj : 0.0;

            DocumentSnapshot bookSnapshot = transaction.get(bookRef);
            if (!bookSnapshot.exists()) {
                throw new FirebaseFirestoreException("Sách không tồn tại.",
                        FirebaseFirestoreException.Code.NOT_FOUND);
            }

            Double currentBookRatingObj = bookSnapshot.getDouble("rating");
            double oldBookRating = currentBookRatingObj != null ? currentBookRatingObj : 0.0;

            Long currentCountObj = bookSnapshot.getLong("rating_count");
            if (currentCountObj == null) {
                currentCountObj = bookSnapshot.getLong("ratingCount");
            }
            long oldCount = currentCountObj != null ? currentCountObj : 0;

            long newCount = Math.max(0, oldCount - 1);
            double newRating = 0.0;
            if (newCount > 0) {
                newRating = ((oldBookRating * oldCount) - oldRatingValue) / newCount;
                newRating = Math.round(newRating * 10.0) / 10.0;
            }

            transaction.delete(ratingRef);

            Map<String, Object> bookUpdates = new HashMap<>();
            bookUpdates.put("rating", newRating);
            bookUpdates.put("rating_count", newCount);
            transaction.update(bookRef, bookUpdates);

            return null;
        });

        task.addOnSuccessListener(aVoid -> syncBookToSupabase(bookId));
        return task;
    }

    /**
     * Đồng bộ thông tin sách mới nhất từ Firestore sang Supabase REST API (RAG System)
     */
    public void syncBookToSupabase(String bookId) {
        String supabaseUrl = SupabaseConfig.getSupabaseUrl();
        String supabaseKey = SupabaseConfig.getSupabaseKey();

        if (supabaseUrl.isEmpty() || supabaseKey.isEmpty()) {
            Log.w(TAG, "Supabase URL/Key trống. Bỏ qua đồng bộ Supabase.");
            return;
        }

        db.collection("books").document(bookId).get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) return;

            Map<String, Object> payload = new HashMap<>();
            payload.put("id", bookId);
            payload.put("title", snapshot.getString("title"));
            payload.put("author", snapshot.getString("author"));
            payload.put("description", snapshot.getString("description"));

            String coverUrl = snapshot.getString("coverUrl");
            if (coverUrl == null) coverUrl = snapshot.getString("cover_url");
            if (coverUrl == null) coverUrl = snapshot.getString("imageUrl");
            payload.put("cover_url", coverUrl);

            String audioUrl = snapshot.getString("audio_link");
            if (audioUrl == null) audioUrl = snapshot.getString("audioUrl");
            payload.put("audio_link", audioUrl);

            payload.put("genre", snapshot.getString("genre"));
            payload.put("genres", snapshot.get("genres"));

            Double rating = snapshot.getDouble("rating");
            payload.put("rating", rating != null ? rating : 0.0);

            Long ratingCount = snapshot.getLong("rating_count");
            if (ratingCount == null) ratingCount = snapshot.getLong("ratingCount");
            payload.put("rating_count", ratingCount != null ? ratingCount : 0);

            Object pages = snapshot.get("pages");
            if (pages instanceof Number) {
                payload.put("pages", ((Number) pages).intValue());
            } else if (pages instanceof String) {
                try {
                    payload.put("pages", Integer.parseInt((String) pages));
                } catch (Exception ignored) {}
            }

            payload.put("duration", snapshot.getString("duration"));
            payload.put("chapters", snapshot.get("chapters"));

            try {
                OkHttpClient client = new OkHttpClient.Builder().build();
                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl(supabaseUrl)
                        .client(client)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();

                SupabaseApiService apiService = retrofit.create(SupabaseApiService.class);
                String bearerToken = "Bearer " + supabaseKey;

                apiService.upsertBook(supabaseKey, bearerToken, "id", payload).enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            Log.d(TAG, "Sync thành công book " + bookId + " sang Supabase!");
                        } else {
                            try {
                                String errBody = response.errorBody() != null ? response.errorBody().string() : "Empty body";
                                Log.e(TAG, "Lỗi sync book sang Supabase. HTTP " + response.code() + ": " + errBody);
                            } catch (Exception e) {
                                Log.e(TAG, "Lỗi sync book sang Supabase. HTTP Code: " + response.code());
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        Log.e(TAG, "Thất bại khi gọi API sync Supabase: " + t.getMessage(), t);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Lỗi tạo Supabase Retrofit client: " + e.getMessage(), e);
            }
        });
    }
}

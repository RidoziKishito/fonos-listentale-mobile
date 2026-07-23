package com.example.fonoss.data.repository;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;

import java.io.IOException;
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

    public Task<Void> upgradeToPremium(String uid) {
        Map<String, Object> data = new HashMap<>();
        data.put("accountType", "PREMIUM");
        data.put("upgradeDate", System.currentTimeMillis());
        return db.collection("users").document(uid).update(data);
    }

    public void createUpgradeRequest(String uid, UpgradeRequestCallback callback) {
        OkHttpClient client = new OkHttpClient();
        // Use 10.0.2.2 for Android Emulator to access host machine localhost
        String url = "https://rendition-mangy-amid.ngrok-free.dev/api/upgrade/create";

        JSONObject json = new JSONObject();
        try {
            json.put("uid", uid);
        } catch (Exception e) {
            callback.onError(e.getMessage());
            return;
        }

        RequestBody body = RequestBody.create(
                json.toString(), MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseData = response.body().string();
                        JSONObject result = new JSONObject(responseData);
                        callback.onSuccess(result);
                    } catch (Exception e) {
                        callback.onError("Failed to parse response");
                    }
                } else {
                    callback.onError("Server returned error: " + response.code());
                }
            }
        });
    }

    public interface UpgradeRequestCallback {
        void onSuccess(JSONObject result);
        void onError(String message);
    }
}

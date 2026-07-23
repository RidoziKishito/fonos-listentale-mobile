package com.example.fonoss.manager;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.fonoss.data.local.LocalBookStorage;
import com.example.fonoss.data.model.Book;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class DownloadWorker extends Worker {
    private static final String TAG = "DownloadWorker";
    public static final String KEY_BOOK_ID = "book_id";
    public static final String KEY_USER_ID = "user_id";
    public static final String KEY_PROGRESS = "progress";

    private static final int BUFFER_SIZE = 64 * 1024;

    public DownloadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String bookId = getInputData().getString(KEY_BOOK_ID);
        String userId = getInputData().getString(KEY_USER_ID);

        if (bookId == null || userId == null) {
            Log.e(TAG, "Missing bookId or userId in worker input data");
            return Result.failure();
        }

        setProgress(0, bookId);

        try {
            // 1. Fetch book data from Firestore
            DocumentSnapshot doc = Tasks.await(FirebaseFirestore.getInstance()
                    .collection("books")
                    .document(bookId)
                    .get());

            if (!doc.exists()) {
                Log.e(TAG, "Book not found in Firestore: " + bookId);
                cleanupFiles(bookId);
                return Result.failure();
            }

            Book book = doc.toObject(Book.class);
            if (book == null) {
                Log.e(TAG, "Failed to parse Book object for " + bookId);
                cleanupFiles(bookId);
                return Result.failure();
            }
            book.setId(doc.getId());

            try {
                List<String> cloudChapters = (List<String>) doc.get("chapters");
                if (cloudChapters != null && !cloudChapters.isEmpty()) {
                    book.setChapters(cloudChapters);
                }
            } catch (Exception ignored) {}

            if (isStopped()) {
                cleanupFiles(bookId);
                return Result.failure();
            }

            // 2. Save text & book metadata to local storage
            LocalBookStorage.save(getApplicationContext(), book);

            if (isStopped()) {
                cleanupFiles(bookId);
                return Result.failure();
            }

            // 3. Download MP3 Audio using OkHttp3 if available
            if (book.getAudio_link() != null && !book.getAudio_link().trim().isEmpty()) {
                try {
                    downloadAudioWithOkHttp(book.getAudio_link().trim(), bookId);
                } catch (Exception audioEx) {
                    Log.w(TAG, "MP3 download skipped/failed for " + bookId + ": " + audioEx.getMessage());
                }
            }

            if (isStopped()) {
                cleanupFiles(bookId);
                return Result.failure();
            }

            // 4. Update downloaded record in user's profile on Firestore
            Tasks.await(FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)
                    .update("downloaded", FieldValue.arrayUnion(toBookMap(book))));

            setProgress(100, bookId);
            return Result.success(new Data.Builder()
                    .putString(KEY_BOOK_ID, bookId)
                    .build());

        } catch (Exception e) {
            Log.e(TAG, "Error downloading book " + bookId, e);
            cleanupFiles(bookId);
            return Result.failure();
        }
    }

    private void downloadAudioWithOkHttp(String audioUrl, String bookId) throws IOException {
        File musicDir = getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        if (musicDir == null) throw new IOException("Music directory unavailable");
        if (!musicDir.exists() && !musicDir.mkdirs()) throw new IOException("Cannot create music directory");

        File finalFile = new File(musicDir, "audio_" + bookId + ".mp3");
        File tempFile = new File(musicDir, "audio_" + bookId + ".tmp");

        if (tempFile.exists() && !tempFile.delete()) throw new IOException("Cannot reset temp file");
        if (finalFile.exists() && !finalFile.delete()) throw new IOException("Cannot replace old audio file");

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build();

        Request request = new Request.Builder()
                .url(audioUrl)
                .header("User-Agent", "ListenTale/1.0")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code() + ": " + response.message());
            }

            ResponseBody body = response.body();
            if (body == null) throw new IOException("Empty response body");

            long totalBytes = body.contentLength();
            long downloadedBytes = 0;
            int lastProgress = -1;
            long lastProgressAt = 0;

            try (InputStream is = body.byteStream();
                 FileOutputStream os = new FileOutputStream(tempFile)) {

                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;

                while ((bytesRead = is.read(buffer)) != -1) {
                    if (isStopped()) {
                        throw new IOException("Download worker stopped");
                    }

                    os.write(buffer, 0, bytesRead);
                    downloadedBytes += bytesRead;

                    int progress = totalBytes > 0
                            ? Math.min(99, (int) ((downloadedBytes * 100L) / totalBytes))
                            : 0;

                    long now = System.currentTimeMillis();
                    if (progress != lastProgress && now - lastProgressAt > 300) {
                        setProgress(progress, bookId);
                        lastProgress = progress;
                        lastProgressAt = now;
                    }
                }
            }

            if (totalBytes > 0 && downloadedBytes < totalBytes) {
                throw new IOException("Incomplete audio download");
            }

            if (!tempFile.renameTo(finalFile)) {
                throw new IOException("Cannot finalize audio file");
            }
        } finally {
            if (tempFile.exists()) tempFile.delete();
        }
    }

    private void setProgress(int progress, String bookId) {
        setProgressAsync(new Data.Builder()
                .putInt(KEY_PROGRESS, progress)
                .putString(KEY_BOOK_ID, bookId)
                .build());
    }

    private void cleanupFiles(String bookId) {
        getApplicationContext().deleteFile("book_" + bookId + ".dat");
        File musicDir = getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        if (musicDir != null) {
            File mp3File = new File(musicDir, "audio_" + bookId + ".mp3");
            if (mp3File.exists()) mp3File.delete();
            File tempFile = new File(musicDir, "audio_" + bookId + ".tmp");
            if (tempFile.exists()) tempFile.delete();
        }
    }

    private Map<String, Object> toBookMap(Book book) {
        Map<String, Object> bookMap = new HashMap<>();
        if (book.getId() != null) bookMap.put("id", book.getId());
        if (book.getTitle() != null) bookMap.put("title", book.getTitle());
        if (book.getAuthor() != null) bookMap.put("author", book.getAuthor());
        if (book.getGenre() != null) bookMap.put("genre", book.getGenre());
        if (book.getGenres() != null) bookMap.put("genres", book.getGenres());
        if (book.getDescription() != null) bookMap.put("description", book.getDescription());
        if (book.getDuration() != null) bookMap.put("duration", book.getDuration());
        if (book.getPages() != null) bookMap.put("pages", book.getPages());
        bookMap.put("rating", book.getRating());
        if (book.getCoverUrl() != null) bookMap.put("coverUrl", book.getCoverUrl());
        if (book.getSeries() != null) bookMap.put("series", book.getSeries());
        return bookMap;
    }
}

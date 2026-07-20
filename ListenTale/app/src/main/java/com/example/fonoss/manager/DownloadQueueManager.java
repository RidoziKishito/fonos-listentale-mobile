package com.example.fonoss.manager;

import com.example.fonoss.data.local.LocalBookStorage;
import com.example.fonoss.data.model.Book;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Environment;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DownloadQueueManager {
    private static final int BUFFER_SIZE = 64 * 1024;
    private static final int CONNECT_TIMEOUT_MS = 15000;
    private static final int READ_TIMEOUT_MS = 30000;
    private static final long IDLE_CLOSE_DELAY_MS = 10000L;

    private static DownloadQueueManager instance;

    private final Context appContext;
    private final Object lock = new Object();
    private final List<DownloadJob> queue = new ArrayList<>();
    private final Set<String> queuedIds = new HashSet<>();
    private final Map<String, Integer> progressValues = new HashMap<>();
    private final MutableLiveData<Map<String, Integer>> progressMap = new MutableLiveData<>(new HashMap<>());

    private Thread workerThread;
    private DownloadJob currentJob;
    private boolean cancelCurrent;

    private final BroadcastReceiver networkReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!isOnline()) cancelCurrentDownload();
        }
    };

    public static synchronized DownloadQueueManager getInstance(Context context) {
        if (instance == null) {
            instance = new DownloadQueueManager(context.getApplicationContext());
        }
        return instance;
    }

    private DownloadQueueManager(Context context) {
        appContext = context;
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(networkReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            appContext.registerReceiver(networkReceiver, filter);
        }
    }

    public LiveData<Map<String, Integer>> getProgressMap() {
        return progressMap;
    }

    public void enqueue(List<Book> books, String userId) {
        if (books == null || books.isEmpty() || userId == null) return;
        synchronized (lock) {
            for (Book book : books) {
                if (book == null || book.getId() == null || queuedIds.contains(book.getId())) continue;
                queue.add(new DownloadJob(book.getId(), book.getTitle(), userId));
                queuedIds.add(book.getId());
                setProgressLocked(book.getId(), 0);
            }
            startWorkerLocked();
            lock.notifyAll();
        }
        publishProgress();
    }

    public void cancel(String bookId) {
        if (bookId == null) return;
        synchronized (lock) {
            if (currentJob != null && bookId.equals(currentJob.bookId)) {
                cancelCurrentDownloadLocked();
            }
            for (int i = queue.size() - 1; i >= 0; i--) {
                DownloadJob job = queue.get(i);
                if (bookId.equals(job.bookId)) {
                    queue.remove(i);
                    queuedIds.remove(bookId);
                }
            }
            removeProgressLocked(bookId);
        }
        cleanupFiles(bookId);
        publishProgress();
    }

    private void cancelCurrentDownload() {
        synchronized (lock) {
            cancelCurrentDownloadLocked();
        }
        publishProgress();
    }

    private void cancelCurrentDownloadLocked() {
        cancelCurrent = true;
        if (currentJob != null) {
            removeProgressLocked(currentJob.bookId);
            cleanupFiles(currentJob.bookId);
        }
        if (workerThread != null) workerThread.interrupt();
    }

    private void startWorkerLocked() {
        if (workerThread != null && workerThread.isAlive()) return;
        workerThread = new Thread(this::runQueue, "ListenTaleDownloadQueue");
        workerThread.start();
    }

    private void runQueue() {
        while (true) {
            DownloadJob job;
            synchronized (lock) {
                while (queue.isEmpty()) {
                    try {
                        lock.wait(IDLE_CLOSE_DELAY_MS);
                    } catch (InterruptedException ignored) {
                    }
                    if (queue.isEmpty()) {
                        workerThread = null;
                        return;
                    }
                }
                job = queue.get(0);
                currentJob = job;
                cancelCurrent = false;
                setProgressLocked(job.bookId, 0);
            }
            publishProgress();

            boolean success = false;
            try {
                if (!isOnline()) throw new InterruptedIOException("Network unavailable");
                download(job);
                success = true;
            } catch (Exception e) {
                cleanupFiles(job.bookId);
                publishProgress();
            } finally {
                synchronized (lock) {
                    if (!queue.isEmpty() && queue.get(0) == job) queue.remove(0);
                    queuedIds.remove(job.bookId);
                    currentJob = null;
                    cancelCurrent = false;
                    removeProgressLocked(job.bookId);
                }
                publishProgress();
            }
        }
    }

    private void download(DownloadJob job) throws Exception {
        DocumentSnapshot doc = Tasks.await(FirebaseFirestore.getInstance()
                .collection("books")
                .document(job.bookId)
                .get());
        if (!doc.exists()) throw new IOException("Book not found");

        Book book = null;
        try {
            book = doc.toObject(Book.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (book == null) throw new IOException("Book data invalid");
        book.setId(doc.getId());

        if (book.getAudio_link() != null && !book.getAudio_link().isEmpty()) {
            downloadAudio(book.getAudio_link(), job.bookId);
        }

        throwIfCancelled();
        LocalBookStorage.save(appContext, book);
        throwIfCancelled();
        Tasks.await(FirebaseFirestore.getInstance()
                .collection("users")
                .document(job.userId)
                .update("downloaded", FieldValue.arrayUnion(toBookMap(book))));
    }

    private void downloadAudio(String audioUrl, String bookId) throws IOException {
        File musicDir = appContext.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        if (musicDir == null) throw new IOException("Music directory unavailable");
        if (!musicDir.exists() && !musicDir.mkdirs()) throw new IOException("Cannot create music directory");

        File finalFile = new File(musicDir, "audio_" + bookId + ".mp3");
        File tempFile = new File(musicDir, "audio_" + bookId + ".tmp");
        if (tempFile.exists() && !tempFile.delete()) throw new IOException("Cannot reset temp download");
        if (finalFile.exists() && !finalFile.delete()) throw new IOException("Cannot replace old audio");

        HttpURLConnection connection = openConnection(audioUrl);
        try {
            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) throw new IOException("HTTP " + responseCode);

            long totalBytes = connection.getContentLengthLong();
            long downloadedBytes = 0;
            int lastProgress = -1;
            long lastProgressAt = 0;

            try (BufferedInputStream input = new BufferedInputStream(connection.getInputStream());
                 FileOutputStream output = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    throwIfCancelled();
                    if (!isOnline()) throw new InterruptedIOException("Network unavailable");
                    output.write(buffer, 0, read);
                    downloadedBytes += read;

                    int progress = totalBytes > 0
                            ? Math.min(99, (int) ((downloadedBytes * 100L) / totalBytes))
                            : 0;
                    long now = System.currentTimeMillis();
                    if (progress != lastProgress && now - lastProgressAt > 400) {
                        synchronized (lock) {
                            setProgressLocked(bookId, progress);
                        }
                        publishProgress();
                        lastProgress = progress;
                        lastProgressAt = now;
                    }
                }
            }

            if (totalBytes > 0 && downloadedBytes < totalBytes) throw new IOException("Incomplete download");
            if (!tempFile.renameTo(finalFile)) throw new IOException("Cannot finalize audio file");
            synchronized (lock) {
                setProgressLocked(bookId, 100);
            }
            publishProgress();
        } finally {
            connection.disconnect();
            if (tempFile.exists()) tempFile.delete();
        }
    }

    private HttpURLConnection openConnection(String audioUrl) throws IOException {
        URL url = new URL(audioUrl);
        for (int redirects = 0; redirects < 5; redirects++) {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestProperty("Accept-Encoding", "identity");
            connection.setRequestProperty("User-Agent", "ListenTale/1.0");

            int code = connection.getResponseCode();
            if (code == HttpURLConnection.HTTP_MOVED_PERM
                    || code == HttpURLConnection.HTTP_MOVED_TEMP
                    || code == HttpURLConnection.HTTP_SEE_OTHER
                    || code == 307
                    || code == 308) {
                String location = connection.getHeaderField("Location");
                connection.disconnect();
                if (location == null || location.isEmpty()) throw new IOException("Redirect without location");
                url = new URL(url, location);
                continue;
            }
            return connection;
        }
        throw new IOException("Too many redirects");
    }

    private void throwIfCancelled() throws InterruptedIOException {
        synchronized (lock) {
            if (cancelCurrent || Thread.currentThread().isInterrupted()) {
                throw new InterruptedIOException("Download cancelled");
            }
        }
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private void cleanupFiles(String bookId) {
        appContext.deleteFile("book_" + bookId + ".dat");
        File musicDir = appContext.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        if (musicDir == null) return;
        File mp3File = new File(musicDir, "audio_" + bookId + ".mp3");
        if (mp3File.exists()) mp3File.delete();
        File tempFile = new File(musicDir, "audio_" + bookId + ".tmp");
        if (tempFile.exists()) tempFile.delete();
    }

    private Map<String, Object> toBookMap(Book book) {
        Map<String, Object> bookMap = new HashMap<>();
        bookMap.put("id", book.getId());
        bookMap.put("title", book.getTitle());
        bookMap.put("author", book.getAuthor());
        bookMap.put("genre", book.getGenre());
        bookMap.put("description", book.getDescription());
        bookMap.put("duration", book.getDuration());
        bookMap.put("pages", book.getPages());
        bookMap.put("rating", book.getRating());
        bookMap.put("coverUrl", book.getCoverUrl());
        bookMap.put("series", book.getSeries());
        return bookMap;
    }

    private void setProgressLocked(String bookId, int progress) {
        progressValues.put(bookId, progress);
    }

    private void removeProgressLocked(String bookId) {
        progressValues.remove(bookId);
    }

    private void publishProgress() {
        synchronized (lock) {
            progressMap.postValue(new HashMap<>(progressValues));
        }
    }

    private static final class DownloadJob {
        final String bookId;
        final String title;
        final String userId;

        DownloadJob(String bookId, String title, String userId) {
            this.bookId = bookId;
            this.title = title;
            this.userId = userId;
        }
    }
}



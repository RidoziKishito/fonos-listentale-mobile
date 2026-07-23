package com.example.fonoss.manager;

import android.content.Context;
import android.os.Environment;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.example.fonoss.data.model.Book;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DownloadQueueManager {
    private static final String TAG_DOWNLOAD_JOB = "download_job";
    private static DownloadQueueManager instance;

    private final Context appContext;
    private final Object lock = new Object();
    private final Map<String, Integer> progressValues = new HashMap<>();
    private final MutableLiveData<Map<String, Integer>> progressMap = new MutableLiveData<>(new HashMap<>());

    public static synchronized DownloadQueueManager getInstance(Context context) {
        if (instance == null) {
            instance = new DownloadQueueManager(context.getApplicationContext());
        }
        return instance;
    }

    private DownloadQueueManager(Context context) {
        appContext = context;

        // Observe WorkManager progress and sync with internal progressMap LiveData
        WorkManager.getInstance(appContext)
                .getWorkInfosByTagLiveData(TAG_DOWNLOAD_JOB)
                .observeForever(workInfos -> {
                    if (workInfos == null) return;
                    synchronized (lock) {
                        for (WorkInfo info : workInfos) {
                            String bookId = info.getProgress().getString(DownloadWorker.KEY_BOOK_ID);
                            if (bookId == null) {
                                for (String tag : info.getTags()) {
                                    if (tag.startsWith("download_") && !TAG_DOWNLOAD_JOB.equals(tag)) {
                                        bookId = tag.substring("download_".length());
                                        break;
                                    }
                                }
                            }
                            if (bookId != null) {
                                WorkInfo.State state = info.getState();
                                if (state == WorkInfo.State.RUNNING) {
                                    int progress = info.getProgress().getInt(DownloadWorker.KEY_PROGRESS, 0);
                                    progressValues.put(bookId, progress);
                                } else if (state == WorkInfo.State.SUCCEEDED
                                        || state == WorkInfo.State.FAILED
                                        || state == WorkInfo.State.CANCELLED) {
                                    progressValues.remove(bookId);
                                } else if (state == WorkInfo.State.ENQUEUED) {
                                    if (!progressValues.containsKey(bookId)) {
                                        progressValues.put(bookId, 0);
                                    }
                                }
                            }
                        }
                    }
                    publishProgress();
                });
    }

    public LiveData<Map<String, Integer>> getProgressMap() {
        return progressMap;
    }

    public void enqueue(List<Book> books, String userId) {
        if (books == null || books.isEmpty() || userId == null) return;

        WorkManager workManager = WorkManager.getInstance(appContext);
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        for (Book book : books) {
            if (book == null || book.getId() == null) continue;

            Data inputData = new Data.Builder()
                    .putString(DownloadWorker.KEY_BOOK_ID, book.getId())
                    .putString(DownloadWorker.KEY_USER_ID, userId)
                    .build();

            OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(DownloadWorker.class)
                    .setInputData(inputData)
                    .setConstraints(constraints)
                    .addTag(TAG_DOWNLOAD_JOB)
                    .addTag("download_" + book.getId())
                    .build();

            synchronized (lock) {
                progressValues.put(book.getId(), 0);
            }

            workManager.enqueueUniqueWork(
                    "download_work_" + book.getId(),
                    ExistingWorkPolicy.REPLACE,
                    workRequest
            );
        }
        publishProgress();
    }

    public void cancel(String bookId) {
        if (bookId == null) return;
        WorkManager.getInstance(appContext).cancelAllWorkByTag("download_" + bookId);
        synchronized (lock) {
            progressValues.remove(bookId);
        }
        cleanupFiles(bookId);
        publishProgress();
    }

    private void cleanupFiles(String bookId) {
        appContext.deleteFile("book_" + bookId + ".dat");
        File musicDir = appContext.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        if (musicDir != null) {
            File mp3File = new File(musicDir, "audio_" + bookId + ".mp3");
            if (mp3File.exists()) mp3File.delete();
            File tempFile = new File(musicDir, "audio_" + bookId + ".tmp");
            if (tempFile.exists()) tempFile.delete();
        }
    }

    private void publishProgress() {
        synchronized (lock) {
            Map<String, Integer> snapshot = new HashMap<>(progressValues);
            if (Looper.myLooper() == Looper.getMainLooper()) {
                progressMap.setValue(snapshot);
            } else {
                progressMap.postValue(snapshot);
            }
        }
    }
}

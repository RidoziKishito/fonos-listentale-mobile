package com.example.fonoss;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.Data;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

public class DownloadWorker extends Worker {
    public DownloadWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String bookId = getInputData().getString("BOOK_ID");
        String bookTitle = getInputData().getString("BOOK_TITLE");
        String userId = getInputData().getString("USER_ID");
        if (bookId == null || userId == null) return Result.failure();

        setProgressAsync(new Data.Builder()
                .putString("BOOK_ID", bookId)
                .putString("TITLE", bookTitle)
                .putInt("PROGRESS", 10)
                .build());

        try {
            DocumentSnapshot doc = Tasks.await(FirebaseFirestore.getInstance().collection("books").document(bookId).get());
            if (doc.exists()) {
                Book book = doc.toObject(Book.class);
                if (book != null) {
                    book.setId(doc.getId());
                    saveBookLocally(book);
                    
                    setProgressAsync(new Data.Builder()
                            .putString("BOOK_ID", bookId)
                            .putString("TITLE", bookTitle)
                            .putInt("PROGRESS", 50)
                            .build());

                    Map<String, Object> bookMap = new HashMap<>();
                    bookMap.put("id", book.getId()); bookMap.put("title", book.getTitle());
                    bookMap.put("author", book.getAuthor()); bookMap.put("genre", book.getGenre());
                    bookMap.put("description", book.getDescription()); bookMap.put("duration", book.getDuration());
                    bookMap.put("pages", book.getPages()); bookMap.put("rating", book.getRating());
                    bookMap.put("coverUrl", book.getCoverUrl());

                    Tasks.await(FirebaseFirestore.getInstance().collection("users").document(userId)
                            .update("downloaded", FieldValue.arrayUnion(bookMap)));

                    setProgressAsync(new Data.Builder()
                            .putString("BOOK_ID", bookId)
                            .putString("TITLE", bookTitle)
                            .putInt("PROGRESS", 100)
                            .build());
                    
                    return Result.success(new Data.Builder()
                            .putString("BOOK_ID", bookId)
                            .putString("TITLE", bookTitle)
                            .build());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Result.failure();
    }

    private void saveBookLocally(Book book) {
        try {
            FileOutputStream fos = getApplicationContext().openFileOutput("book_" + book.getId() + ".dat", Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(book);
            oos.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
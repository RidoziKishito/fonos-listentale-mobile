package com.example.fonoss.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.example.fonoss.R;
import com.example.fonoss.ui.main.MainActivity;
import com.example.fonoss.utils.NotificationHelper;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class NotificationWorker extends Worker {

    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return Result.success();

        String userId = auth.getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        try {
            DocumentSnapshot snapshot = Tasks.await(db.collection("users").document(userId).get());
            if (snapshot.exists()) {
                Object inProgressObj = snapshot.get("inProgress");
                List<Map<String, Object>> inProgress = null;
                if (inProgressObj instanceof List) {
                    inProgress = (List<Map<String, Object>>) inProgressObj;
                }

                String message;
                String title = "Time to read!";

                if (inProgress != null && !inProgress.isEmpty()) {
                    Map<String, Object> randomBook = inProgress.get(new Random().nextInt(inProgress.size()));
                    String bookTitle = (String) randomBook.get("title");
                    message = "Don't forget to continue reading '" + bookTitle + "'!";
                } else {
                    message = "It's a great time to start a new book!";
                }

                sendNotification(title, message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Result.success();
    }

    private void sendNotification(String title, String message) {
        Context context = getApplicationContext();
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_nav_books)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(2024, notification);
        }
    }
}

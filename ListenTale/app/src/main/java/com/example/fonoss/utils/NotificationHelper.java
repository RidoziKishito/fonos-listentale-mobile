package com.example.fonoss.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.example.fonoss.service.NotificationWorker;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class NotificationHelper {

    public static final String CHANNEL_ID = "reading_reminders_channel";
    public static final String CHANNEL_NAME = "Reading Reminders";
    public static final String WORK_TAG = "daily_reading_reminder";

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Channel for daily reading reminder notifications");
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public static void cancelReminder(Context context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(WORK_TAG);
    }

    public static void scheduleDailyReminder(Context context, int hour, int minute) {
        cancelReminder(context);

        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.set(Calendar.HOUR_OF_DAY, hour);
        target.set(Calendar.MINUTE, minute);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);

        if (target.before(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1);
        }

        long initialDelay = target.getTimeInMillis() - now.getTimeInMillis();

        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                NotificationWorker.class,
                24, TimeUnit.HOURS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .addTag(WORK_TAG)
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_TAG,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
        );
    }
}

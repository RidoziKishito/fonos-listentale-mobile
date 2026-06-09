package com.example.fonoss;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.media.app.NotificationCompat.MediaStyle;

public class AudioService extends Service {

    private static final String CHANNEL_ID = "AudioPlayerChannel";
    private static final int NOTIFICATION_ID = 1;

    private final IBinder binder = new LocalBinder();
    private boolean isPlaying = false;
    private int currentPosition = 0;
    private final int totalDuration = 1800; 
    private float playbackSpeed = 1.0f;
    private Book currentBook;
    private MediaSessionCompat mediaSession;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            if (isPlaying && currentPosition < totalDuration) {
                currentPosition++;
                updatePlaybackState();
                handler.postDelayed(this, (long) (1000 / playbackSpeed));
            }
        }
    };

    public class LocalBinder extends Binder {
        AudioService getService() { return AudioService.this; }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        mediaSession = new MediaSessionCompat(this, "ListenTaleSession");
        setupMediaSession();
        
        startForeground(NOTIFICATION_ID, getNotification());
    }

    private void setupMediaSession() {
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override public void onPlay() { play(); }
            @Override public void onPause() { pause(); }
            @Override public void onSeekTo(long pos) { seekTo((int) (pos / 1000)); }
        });
        mediaSession.setActive(true);
    }

    private void updatePlaybackState() {
        if (mediaSession == null) return;
        int state = isPlaying ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED;
        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE | 
                            PlaybackStateCompat.ACTION_SEEK_TO | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | 
                            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                .setState(state, currentPosition * 1000L, playbackSpeed)
                .build());
    }

    private void updateMetadata() {
        if (currentBook == null || mediaSession == null) return;
        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentBook.getTitle())
                .putString(MediaMetadataCompat.METADATA_KEY_AUTHOR, currentBook.getAuthor())
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, totalDuration * 1000L)
                .build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "PLAY_PAUSE".equals(intent.getAction())) togglePlayPause();
        return START_STICKY; 
    }

    @Override
    public IBinder onBind(Intent intent) { return binder; }

    public void playBook(Book book) {
        if (currentBook == null || !currentBook.getId().equals(book.getId())) {
            this.currentBook = book;
            this.currentPosition = 0;
            updateMetadata();
        }
        play();
    }

    public void play() {
        if (currentBook == null) return;
        isPlaying = true;
        handler.removeCallbacks(progressRunnable);
        handler.post(progressRunnable);
        updatePlaybackState();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, getNotification(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(NOTIFICATION_ID, getNotification());
        }
    }

    public void pause() {
        if (currentBook == null) return;
        isPlaying = false;
        handler.removeCallbacks(progressRunnable);
        updatePlaybackState();
        
        // Dừng trạng thái foreground nhưng vẫn giữ thông báo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_DETACH);
        } else {
            stopForeground(false);
        }
        
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) manager.notify(NOTIFICATION_ID, getNotification());
    }

    public void togglePlayPause() {
        if (isPlaying) pause();
        else play();
    }

    public void seekTo(int seconds) {
        currentPosition = Math.max(0, Math.min(seconds, totalDuration));
        updatePlaybackState();
        startForeground(NOTIFICATION_ID, getNotification());
    }

    public void setPlaybackSpeed(float speed) {
        this.playbackSpeed = speed;
        if (isPlaying) {
            handler.removeCallbacks(progressRunnable);
            handler.post(progressRunnable);
        }
        updatePlaybackState();
    }

    public boolean isPlaying() { return isPlaying; }
    public int getCurrentPosition() { return currentPosition; }
    public int getTotalDuration() { return totalDuration; }
    public float getPlaybackSpeed() { return playbackSpeed; }
    public Book getCurrentBook() { return currentBook; }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "ListenTale Playback", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Audio controls while listening");
            channel.setShowBadge(false);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private Notification getNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.putExtra("OPEN_PLAYER", true);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent playIntent = new Intent(this, AudioService.class);
        playIntent.setAction("PLAY_PAUSE");
        PendingIntent playPendingIntent = PendingIntent.getService(this, 0, playIntent, PendingIntent.FLAG_IMMUTABLE);

        int playIcon = isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play;

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_nav_books)
                .setContentTitle(currentBook != null ? currentBook.getTitle() : "ListenTale")
                .setContentText(currentBook != null ? "by " + currentBook.getAuthor() : "Ready to listen")
                .setSubText("ListenTale")
                .setContentIntent(pendingIntent)
                .addAction(android.R.drawable.ic_media_previous, "Previous", null)
                .addAction(playIcon, "Play/Pause", playPendingIntent)
                .addAction(android.R.drawable.ic_media_next, "Next", null)
                .setStyle(new MediaStyle().setShowActionsInCompactView(1).setMediaSession(mediaSession.getSessionToken()))
                .setColor(ContextCompat.getColor(this, R.color.primary_600))
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build();
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(progressRunnable);
        if (mediaSession != null) mediaSession.release();
        super.onDestroy();
    }
}

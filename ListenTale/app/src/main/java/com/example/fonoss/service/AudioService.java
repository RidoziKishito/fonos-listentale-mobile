package com.example.fonoss.service;

import com.example.fonoss.R;

import com.example.fonoss.data.model.Book;
import com.example.fonoss.ui.main.MainActivity;

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
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.widget.Toast;

public class AudioService extends Service {

    private static final String CHANNEL_ID = "AudioPlayerChannel";
    private static final int NOTIFICATION_ID = 1;

    private final IBinder binder = new LocalBinder();
    private boolean isPlaying = false;
    private int currentPosition = 0;
    private int totalDuration = 0;
    private float playbackSpeed = 1.0f;
    private Book currentBook;
    private MediaSessionCompat mediaSession;
    private MediaPlayer mediaPlayer;
    private NetworkChangeReceiver networkChangeReceiver;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            if (isPlaying && mediaPlayer != null) {
                currentPosition = mediaPlayer.getCurrentPosition() / 1000;
                updatePlaybackState();
                handler.postDelayed(this, 1000);
            }
        }
    };

    public interface PlaybackStateListener {
        void onStateChanged(boolean isPlaying);
    }
    private PlaybackStateListener listener;
    public void setListener(PlaybackStateListener listener) { this.listener = listener; }

    private class NetworkChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                if (cm != null) {
                    boolean isMobile = false;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        Network network = cm.getActiveNetwork();
                        if (network != null) {
                            NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
                            if (capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                                isMobile = true;
                            }
                        }
                    } else {
                        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                        if (activeNetwork != null && activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                            isMobile = true;
                        }
                    }

                    if (isMobile && isPlaying) {
                        pause();
                        Intent alertIntent = new Intent("com.example.fonoss.NETWORK_WARNING");
                        alertIntent.putExtra("message", "You are using Mobile Data. Playback has been paused to prevent unexpected data costs.");
                        alertIntent.setPackage(context.getPackageName());
                        context.sendBroadcast(alertIntent);
                    }
                }
            }
        }
    }

    public class LocalBinder extends Binder {
        public AudioService getService() { return AudioService.this; }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        mediaSession = new MediaSessionCompat(this, "ListenTaleSession");
        setupMediaSession();
        
        networkChangeReceiver = new NetworkChangeReceiver();
        ContextCompat.registerReceiver(this, networkChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION), ContextCompat.RECEIVER_NOT_EXPORTED);

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
        if (listener != null) listener.onStateChanged(isPlaying);
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
            this.totalDuration = 0;
            updateMetadata();

            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }

            java.io.File localAudio = new java.io.File(getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC), "audio_" + book.getId() + ".mp3");
            String dataSource = localAudio.exists() ? localAudio.getAbsolutePath() : book.getAudio_link();

            if (dataSource != null && !dataSource.isEmpty()) {
                try {
                    mediaPlayer = new MediaPlayer();
                    mediaPlayer.setAudioAttributes(
                        new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    );
                    mediaPlayer.setDataSource(dataSource);
                    mediaPlayer.setOnPreparedListener(mp -> {
                        totalDuration = mp.getDuration() / 1000;
                        updateMetadata();
                        play();
                    });
                    mediaPlayer.setOnCompletionListener(mp -> {
                        isPlaying = false;
                        updatePlaybackState();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            stopForeground(STOP_FOREGROUND_DETACH);
                        } else {
                            stopForeground(false);
                        }
                    });
                    mediaPlayer.prepareAsync();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            play();
        }
    }

    public void play() {
        if (currentBook == null || mediaPlayer == null) return;
        isPlaying = true;
        mediaPlayer.start();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                mediaPlayer.setPlaybackParams(mediaPlayer.getPlaybackParams().setSpeed(playbackSpeed));
            } catch (Exception e) { e.printStackTrace(); }
        }
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
        if (currentBook == null || mediaPlayer == null) return;
        isPlaying = false;
        if (mediaPlayer.isPlaying()) mediaPlayer.pause();
        handler.removeCallbacks(progressRunnable);
        updatePlaybackState();

        // Stop while in foreground but keep notification
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
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(seconds * 1000);
            currentPosition = seconds;
        } else {
            currentPosition = Math.max(0, Math.min(seconds, totalDuration));
        }
        updatePlaybackState();
        startForeground(NOTIFICATION_ID, getNotification());
    }

    public void setPlaybackSpeed(float speed) {
        this.playbackSpeed = speed;
        if (mediaPlayer != null && isPlaying) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    mediaPlayer.setPlaybackParams(mediaPlayer.getPlaybackParams().setSpeed(speed));
                } catch (Exception e) { e.printStackTrace(); }
            }
        }
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
                .setSmallIcon(R.drawable.ic_notification_transparent)
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
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (mediaSession != null) mediaSession.release();
        if (networkChangeReceiver != null) {
            unregisterReceiver(networkChangeReceiver);
        }
        super.onDestroy();
    }
}



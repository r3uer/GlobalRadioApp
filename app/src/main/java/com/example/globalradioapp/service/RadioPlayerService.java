package com.example.globalradioapp.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import com.example.globalradioapp.MainActivity;
import com.example.globalradioapp.R;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.audio.AudioAttributes;

public class RadioPlayerService extends Service {

    private static final String CHANNEL_ID = "RadioPlayerChannel";
    private static final int NOTIFICATION_ID = 1;
    private static final String TAG = "RadioService";
    private PlaybackCallback playbackCallback;

    private ExoPlayer exoPlayer;
    private String currentStationName = "";
    private String currentStreamUrl = "";
    private boolean isPlaying = false;

    private Handler sleepTimerHandler;
    private Runnable sleepTimerRunnable;
    private MediaSessionCompat mediaSession;

    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        public RadioPlayerService getService() {
            return RadioPlayerService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        initializePlayer();
        initializeMediaSession();
        sleepTimerHandler = new Handler(Looper.getMainLooper());
    }

    private void initializePlayer() {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(com.google.android.exoplayer2.C.USAGE_MEDIA)
                .setContentType(com.google.android.exoplayer2.C.AUDIO_CONTENT_TYPE_MUSIC)
                .build();

        exoPlayer = new ExoPlayer.Builder(this).build();
        exoPlayer.setAudioAttributes(audioAttributes, true);

        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                RadioPlayerService.this.isPlaying = isPlaying;
                updateNotification();
            }
        });
    }

    private void initializeMediaSession() {
        mediaSession = new MediaSessionCompat(this, "RadioPlayerService");
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setActive(true);

        PlaybackStateCompat state = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY |
                                PlaybackStateCompat.ACTION_PAUSE |
                                PlaybackStateCompat.ACTION_STOP
                ).build();
        mediaSession.setPlaybackState(state);

        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                resume();
            }

            @Override
            public void onPause() {
                pause();
            }

            @Override
            public void onStop() {
                stopRadio();
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Start foreground service first
        startForeground(NOTIFICATION_ID, createNotification());

        if (intent != null && intent.getAction() != null) {
            Log.d(TAG, "Received action: " + intent.getAction());
            switch (intent.getAction()) {
                case "ACTION_PLAY":
                    if (!currentStreamUrl.isEmpty()) {
                        resume();
                    }
                    break;
                case "ACTION_PAUSE":
                    pause();
                    break;
                case "ACTION_STOP":
                    stopRadio();
                    // Don't call stopSelf() immediately, let the UI handle it
                    break;
            }
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void playStation(String streamUrl, String stationName) {
        this.currentStreamUrl = streamUrl;
        this.currentStationName = stationName;

        MediaItem mediaItem = MediaItem.fromUri(Uri.parse(streamUrl));
        exoPlayer.setMediaItem(mediaItem);
        exoPlayer.prepare();
        exoPlayer.play();

        isPlaying = true;
        updateNotification();
    }

    public void pause() {
        if (exoPlayer != null) {
            exoPlayer.pause();
            isPlaying = false;
            updateNotification();
        }
    }

    public void resume() {
        if (exoPlayer != null && !currentStreamUrl.isEmpty()) {
            exoPlayer.play();
            isPlaying = true;
            updateNotification();
        }
    }

    public void stopRadio() {
        if (exoPlayer != null) {
            exoPlayer.stop();
            exoPlayer.clearMediaItems();
            isPlaying = false;
            updateNotification();
        }
    }

    public void setVolume(float volume) {
        if (exoPlayer != null) {
            exoPlayer.setVolume(volume);
        }
    }

    public void setSleepTimer(int minutes) {
        if (sleepTimerRunnable != null) {
            sleepTimerHandler.removeCallbacks(sleepTimerRunnable);
        }

        sleepTimerRunnable = () -> {
            pause();
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                stopRadio();
                stopSelf();
            }, 5000);
        };

        sleepTimerHandler.postDelayed(sleepTimerRunnable, minutes * 60 * 1000L);
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public String getCurrentStationName() {
        return currentStationName;
    }
    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Global Radio")
                .setContentText(isPlaying ? "Playing: " + currentStationName : "Paused")
                .setSmallIcon(R.drawable.ic_radio)
                .setContentIntent(pendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setOngoing(true);

        // Unique request codes for each action!
        if (isPlaying) {
            Intent pauseIntent = new Intent(this, RadioPlayerService.class);
            pauseIntent.setAction("ACTION_PAUSE");
            PendingIntent pausePendingIntent = PendingIntent.getService(
                    this, 1, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            builder.addAction(new NotificationCompat.Action(
                    R.drawable.ic_pause, "Pause", pausePendingIntent));
        } else {
            Intent playIntent = new Intent(this, RadioPlayerService.class);
            playIntent.setAction("ACTION_PLAY");
            PendingIntent playPendingIntent = PendingIntent.getService(
                    this, 2, playIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            builder.addAction(new NotificationCompat.Action(
                    R.drawable.ic_play_arrow, "Play", playPendingIntent));
        }

        Intent stopIntent = new Intent(this, RadioPlayerService.class);
        stopIntent.setAction("ACTION_STOP");
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this, 3, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        builder.addAction(new NotificationCompat.Action(
                R.drawable.ic_stop, "Stop", stopPendingIntent));

        try {
            androidx.media.app.NotificationCompat.MediaStyle mediaStyle =
                    new androidx.media.app.NotificationCompat.MediaStyle()
                            .setMediaSession(mediaSession.getSessionToken())
                            .setShowActionsInCompactView(0, 1);
            builder.setStyle(mediaStyle);
        } catch (Exception e) {
            Log.e(TAG, "Error setting MediaStyle", e);
        }

        return builder.build();
    }

    private void updateNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, createNotification());
        }
    }
    public interface PlaybackCallback {
        void onPlaybackStarted();
        void onPlaybackPaused();
        void onPlaybackStopped();
        void onPlaybackError(String error);
    }

    public void setPlaybackCallback(PlaybackCallback callback) {
        this.playbackCallback = callback;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Radio Playback";
            String description = "Channel for radio playback controls";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (exoPlayer != null) {
            exoPlayer.release();
        }
        if (sleepTimerHandler != null && sleepTimerRunnable != null) {
            sleepTimerHandler.removeCallbacks(sleepTimerRunnable);
        }
        if (mediaSession != null) {
            mediaSession.release();
        }
    }
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        stopRadio();      // Stop playback
        stopSelf();       // Stop the service
        super.onTaskRemoved(rootIntent);
    }
}
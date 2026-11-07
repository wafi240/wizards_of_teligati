package com.teligati.app.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.teligati.app.MainActivity;
import com.teligati.app.R;

import java.util.Locale;

/**
 * StudySessionService is a simple Android Foreground Service demonstrating:
 * 1. Promotion to foreground with startForeground(...)
 * 2. Persistent notification with stop action
 * 3. Notification channel management (Android 8.0+)
 * 4. Android 14 (API 34) Foreground Service type (dataSync) compliance
 * 5. Clean teardown with stopForeground(...) and stopSelf()
 */
public class StudySessionService extends Service {
    private static final String TAG = "StudySessionService";
    public static final String CHANNEL_ID = "teligati_study_session_channel";
    public static final int NOTIFICATION_ID = 3001;

    public static final String ACTION_START = "com.teligati.app.service.ACTION_START";
    public static final String ACTION_STOP = "com.teligati.app.service.ACTION_STOP";

    public interface SessionListener {
        void onTick(int totalSeconds);
        void onSessionEnded(int totalSeconds);
    }

    private static SessionListener activeListener;
    private static boolean isRunning = false;
    private static int elapsedSeconds = 0;

    private Handler timerHandler;
    private Runnable timerRunnable;

    public static boolean isServiceRunning() {
        return isRunning;
    }

    public static int getElapsedSeconds() {
        return elapsedSeconds;
    }

    public static void setSessionListener(SessionListener listener) {
        activeListener = listener;
        if (activeListener != null && isRunning) {
            activeListener.onTick(elapsedSeconds);
        }
    }

    public static void startSession(Context context) {
        Intent intent = new Intent(context, StudySessionService.class);
        intent.setAction(ACTION_START);
        ContextCompat.startForegroundService(context, intent);
    }

    public static void stopSession(Context context) {
        Intent intent = new Intent(context, StudySessionService.class);
        intent.setAction(ACTION_STOP);
        context.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ensureNotificationChannel(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopStudySession();
            return START_NOT_STICKY;
        }

        if (!isRunning) {
            startStudySession();
        }
        return START_STICKY;
    }

    private void startStudySession() {
        isRunning = true;
        elapsedSeconds = 0;

        Notification notification = buildNotification(0);

        // Android 14 (API 34) mandates passing foregroundServiceType
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        timerHandler = new Handler(Looper.getMainLooper());
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isRunning) return;
                elapsedSeconds++;
                if (activeListener != null) {
                    activeListener.onTick(elapsedSeconds);
                }
                // Update persistent notification every 5 seconds to conserve CPU/battery
                if (elapsedSeconds % 5 == 0) {
                    NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    if (manager != null) {
                        manager.notify(NOTIFICATION_ID, buildNotification(elapsedSeconds));
                    }
                }
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.postDelayed(timerRunnable, 1000);

        Toast.makeText(this, "▶️ KUET Study Session started in foreground", Toast.LENGTH_SHORT).show();
    }

    private void stopStudySession() {
        if (!isRunning) return;
        isRunning = false;

        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }

        int finalTime = elapsedSeconds;
        if (activeListener != null) {
            activeListener.onSessionEnded(finalTime);
        }

        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();

        String duration = formatTime(finalTime);
        Toast.makeText(this, "⏹️ Study Session ended. Completed focus time: " + duration, Toast.LENGTH_LONG).show();
    }

    private Notification buildNotification(int seconds) {
        String formatted = formatTime(seconds);

        Intent openAppIntent = new Intent(this, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent contentPendingIntent = PendingIntent.getActivity(
                this, 0, openAppIntent, pendingFlags
        );

        Intent stopIntent = new Intent(this, StudySessionService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this, 1, stopIntent, pendingFlags
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("⏱️ KUET Study Session Active")
                .setContentText("Focus Time: " + formatted + " • Foreground Service Running")
                .setSmallIcon(R.drawable.ic_nav_home)
                .setOngoing(true)
                .setContentIntent(contentPendingIntent)
                .addAction(android.R.drawable.ic_media_pause, "⏹️ Stop Session", stopPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOnlyAlertOnce(true)
                .build();
    }

    public static String formatTime(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    public static void ensureNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Study & Focus Sessions",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Persistent foreground notifications for active study and research sessions");
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (isRunning) {
            stopStudySession();
        }
        super.onDestroy();
    }
}

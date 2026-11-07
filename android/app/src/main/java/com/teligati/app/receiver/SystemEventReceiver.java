package com.teligati.app.receiver;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.teligati.app.R;

/**
 * SystemEventReceiver detects system broadcast events required for the university project:
 * 1. Airplane Mode ON / OFF changes (Intent.ACTION_AIRPLANE_MODE_CHANGED)
 * 2. Battery Low warnings (Intent.ACTION_BATTERY_LOW)
 * 3. Battery Recovery (Intent.ACTION_BATTERY_OKAY)
 *
 * Compliant with Android 8.0+ background broadcast restrictions (requires dynamic registration)
 * and Android 14 (API 34) ContextCompat.RECEIVER_EXPORTED requirements.
 */
public class SystemEventReceiver extends BroadcastReceiver {
    private static final String TAG = "SystemEventReceiver";
    public static final String CHANNEL_ID = "teligati_system_alerts";
    public static final int NOTIFICATION_ID_AIRPLANE = 2001;
    public static final int NOTIFICATION_ID_BATTERY = 2002;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        String action = intent.getAction();
        Log.i(TAG, "Broadcast received: " + action);

        if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
            boolean isAirplaneModeOn = intent.getBooleanExtra("state", false);
            // Fallback check from system settings if intent extra is not populated
            if (!intent.hasExtra("state")) {
                try {
                    isAirplaneModeOn = Settings.Global.getInt(
                            context.getContentResolver(),
                            Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
                } catch (Exception e) {
                    Log.w(TAG, "Could not read AIRPLANE_MODE_ON setting: " + e.getMessage());
                }
            }
            handleAirplaneMode(context, isAirplaneModeOn);
        } else if (Intent.ACTION_BATTERY_LOW.equals(action)) {
            handleBatteryLow(context);
        } else if (Intent.ACTION_BATTERY_OKAY.equals(action)) {
            handleBatteryOkay(context);
        }
    }

    public static void handleAirplaneMode(Context context, boolean isAirplaneModeOn) {
        String message = isAirplaneModeOn
                ? "✈️ Airplane Mode ON: Teligati offline mode active"
                : "📶 Airplane Mode OFF: Connected to Teligati network";

        Log.i(TAG, "Airplane mode state: " + isAirplaneModeOn + " -> " + message);
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        postNotification(context, NOTIFICATION_ID_AIRPLANE, "Network Status Change", message);
    }

    public static void handleBatteryLow(Context context) {
        String message = "🔋 Battery Low Warning: Connect charger for optimal app experience";
        Log.i(TAG, "Battery low warning triggered");
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        postNotification(context, NOTIFICATION_ID_BATTERY, "Power Warning", message);
    }

    public static void handleBatteryOkay(Context context) {
        String message = "⚡ Battery Recovered: Normal power level restored";
        Log.i(TAG, "Battery level recovered");
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        postNotification(context, NOTIFICATION_ID_BATTERY, "Power Restored", message);
    }

    private static void postNotification(Context context, int notificationId, String title, String body) {
        try {
            ensureNotificationChannel(context);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
            }

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_nav_home)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true);

            NotificationManagerCompat.from(context).notify(notificationId, builder.build());
        } catch (Exception e) {
            Log.w(TAG, "Notification post skipped or restricted: " + e.getMessage());
        }
    }

    public static void ensureNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "System Alerts",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("System state notifications (Airplane mode, Battery)");
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    // Direct simulation helpers for classroom/presentation demonstration
    public static void simulateAirplaneMode(Context context, boolean isAirplaneModeOn) {
        handleAirplaneMode(context, isAirplaneModeOn);
    }

    public static void simulateBatteryLow(Context context) {
        handleBatteryLow(context);
    }
}

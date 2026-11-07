package com.teligati.app;

import android.app.Application;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.core.content.ContextCompat;

import com.teligati.app.receiver.SystemEventReceiver;

/**
 * TeligatiApplication serves as the central Android Application class.
 * It manages application-wide lifecycle events and registers dynamic
 * BroadcastReceivers to comply with Android 8.0+ (API 26+) background limitations
 * and Android 14 (API 34) exported receiver standards.
 */
public class TeligatiApplication extends Application {
    private SystemEventReceiver systemEventReceiver;

    @Override
    public void onCreate() {
        super.onCreate();

        // Ensure system alert notification channel exists on Android O+
        SystemEventReceiver.ensureNotificationChannel(this);

        // Dynamically register system event receiver for Airplane Mode and Battery
        registerSystemReceivers();
    }

    private void registerSystemReceivers() {
        systemEventReceiver = new SystemEventReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        filter.addAction(Intent.ACTION_BATTERY_LOW);
        filter.addAction(Intent.ACTION_BATTERY_OKAY);

        // Android 14 (API 34) requires RECEIVER_EXPORTED for framework system broadcasts
        ContextCompat.registerReceiver(this, systemEventReceiver, filter, ContextCompat.RECEIVER_EXPORTED);
    }
}

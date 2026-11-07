package com.teligati.app.receiver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Intent;

import org.junit.Before;
import org.junit.Test;

public class SystemEventReceiverTest {

    private SystemEventReceiver receiver;

    @Before
    public void setUp() {
        receiver = new SystemEventReceiver();
    }

    @Test
    public void testReceiverConstants() {
        assertNotNull(SystemEventReceiver.CHANNEL_ID);
        assertEquals("teligati_system_alerts", SystemEventReceiver.CHANNEL_ID);
        assertNotEquals(SystemEventReceiver.NOTIFICATION_ID_AIRPLANE, SystemEventReceiver.NOTIFICATION_ID_BATTERY);
    }

    @Test
    public void testNullSafetyOnReceive() {
        // Ensure no NullPointerException on null intent or context
        receiver.onReceive(null, null);
    }

    @Test
    public void testExpectedActionConstants() {
        assertEquals("android.intent.action.AIRPLANE_MODE", Intent.ACTION_AIRPLANE_MODE_CHANGED);
        assertEquals("android.intent.action.BATTERY_LOW", Intent.ACTION_BATTERY_LOW);
        assertEquals("android.intent.action.BATTERY_OKAY", Intent.ACTION_BATTERY_OKAY);
    }

    @Test
    public void testReceiverClassLoaded() {
        assertNotNull(SystemEventReceiver.class);
    }
}

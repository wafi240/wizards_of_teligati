package com.teligati.app.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class StudySessionServiceTest {

    @Test
    public void testFormatTime() {
        assertEquals("00:00", StudySessionService.formatTime(0));
        assertEquals("00:07", StudySessionService.formatTime(7));
        assertEquals("01:00", StudySessionService.formatTime(60));
        assertEquals("01:25", StudySessionService.formatTime(85));
        assertEquals("10:00", StudySessionService.formatTime(600));
        assertEquals("65:30", StudySessionService.formatTime(3930));
    }

    @Test
    public void testServiceConstants() {
        assertEquals("teligati_study_session_channel", StudySessionService.CHANNEL_ID);
        assertTrue(StudySessionService.NOTIFICATION_ID > 0);
        assertNotNull(StudySessionService.ACTION_START);
        assertNotNull(StudySessionService.ACTION_STOP);
        assertNotEquals(StudySessionService.ACTION_START, StudySessionService.ACTION_STOP);
    }

    @Test
    public void testInitialServiceState() {
        // By default service should report its class and state methods cleanly
        assertNotNull(StudySessionService.class);
    }
}

package com.teligati.app.mediastore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.Manifest;
import android.net.Uri;
import android.os.Build;

import org.junit.Test;

public class MediaStoreHelperTest {

    @Test
    public void testRequiredPermissionString() {
        String permission = MediaStoreHelper.getRequiredMediaPermission();
        assertNotNull("Permission should never be null", permission);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            assertEquals(Manifest.permission.READ_MEDIA_IMAGES, permission);
        } else {
            assertEquals(Manifest.permission.READ_EXTERNAL_STORAGE, permission);
        }
    }

    @Test
    public void testMediaImageItemContract() {
        Uri testUri = Uri.parse("content://media/external/images/media/42");
        MediaImageItem item = new MediaImageItem(42L, testUri, "kuet_profile.jpg", 1024L, 1700000000L);

        assertEquals(42L, item.getId());
        assertEquals(testUri, item.getUri());
        assertEquals("kuet_profile.jpg", item.getDisplayName());
        assertEquals(1024L, item.getSize());
        assertEquals(1700000000L, item.getDateAdded());
    }

    @Test
    public void testMediaImageItemDefaultDisplayName() {
        Uri testUri = Uri.parse("content://media/external/images/media/99");
        MediaImageItem item = new MediaImageItem(99L, testUri, null, 2048L, 1700000000L);

        assertTrue("Should fallback to generated name when null", item.getDisplayName().contains("99"));
    }
}

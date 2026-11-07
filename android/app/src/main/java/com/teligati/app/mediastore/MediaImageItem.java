package com.teligati.app.mediastore;

import android.net.Uri;

/**
 * Represents an image item resolved from Android's MediaStore Content Provider.
 */
public class MediaImageItem {
    private final long id;
    private final Uri uri;
    private final String displayName;
    private final long size;
    private final long dateAdded;

    public MediaImageItem(long id, Uri uri, String displayName, long size, long dateAdded) {
        this.id = id;
        this.uri = uri;
        this.displayName = displayName != null ? displayName : "image_" + id + ".jpg";
        this.size = size;
        this.dateAdded = dateAdded;
    }

    public long getId() {
        return id;
    }

    public Uri getUri() {
        return uri;
    }

    public String getDisplayName() {
        return displayName;
    }

    public long getSize() {
        return size;
    }

    public long getDateAdded() {
        return dateAdded;
    }
}

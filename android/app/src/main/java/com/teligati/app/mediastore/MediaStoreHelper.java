package com.teligati.app.mediastore;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;

import androidx.core.content.ContextCompat;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * MediaStoreHelper manages interactions with Android's system Content Provider (MediaStore)
 * for querying and loading images from the device gallery.
 */
public class MediaStoreHelper {
    private static final String TAG = "MediaStoreHelper";

    public static String getRequiredMediaPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            return Manifest.permission.READ_EXTERNAL_STORAGE;
        }
    }

    public static boolean hasMediaPermission(Context context) {
        String permission = getRequiredMediaPermission();
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Queries the MediaStore Content Provider using ContentResolver.query(...)
     */
    public static List<MediaImageItem> fetchDeviceImages(Context context, int limit) {
        List<MediaImageItem> items = new ArrayList<>();
        if (context == null) return items;

        ContentResolver resolver = context.getContentResolver();
        Uri collection;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        } else {
            collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        }

        String[] projection = new String[]{
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.DATE_ADDED
        };

        String sortOrder = MediaStore.Images.Media.DATE_ADDED + " DESC";

        try (Cursor cursor = resolver.query(collection, projection, null, null, sortOrder)) {
            if (cursor != null) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
                int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE);
                int dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED);

                int count = 0;
                while (cursor.moveToNext() && count < limit) {
                    long id = cursor.getLong(idColumn);
                    String name = cursor.getString(nameColumn);
                    long size = cursor.getLong(sizeColumn);
                    long dateAdded = cursor.getLong(dateColumn);

                    Uri contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                    items.add(new MediaImageItem(id, contentUri, name, size, dateAdded));
                    count++;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying MediaStore: " + e.getMessage());
        }

        return items;
    }

    /**
     * Safely decodes a thumbnail bitmap from the MediaStore content Uri.
     */
    public static Bitmap loadThumbnail(Context context, Uri uri, int reqWidth, int reqHeight) {
        if (context == null || uri == null) return null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                return context.getContentResolver().loadThumbnail(uri, new Size(reqWidth, reqHeight), null);
            } else {
                InputStream input = context.getContentResolver().openInputStream(uri);
                if (input == null) return null;

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(input, null, options);
                input.close();

                options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
                options.inJustDecodeBounds = false;

                InputStream input2 = context.getContentResolver().openInputStream(uri);
                Bitmap bitmap = BitmapFactory.decodeStream(input2, null, options);
                if (input2 != null) input2.close();
                return bitmap;
            }
        } catch (Exception e) {
            Log.e(TAG, "Thumbnail decode exception: " + e.getMessage());
            return null;
        }
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    /**
     * Inserts a sample demo image into MediaStore Content Provider.
     * Useful when running in clean emulators that have 0 pre-loaded images.
     */
    public static Uri insertSampleTestImage(Context context, String label) {
        try {
            int size = 300;
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);

            // Background
            Paint bgPaint = new Paint();
            bgPaint.setColor(Color.parseColor("#1C2541"));
            canvas.drawRect(0, 0, size, size, bgPaint);

            // Border
            Paint border = new Paint();
            border.setColor(Color.parseColor("#00ADB5"));
            border.setStyle(Paint.Style.STROKE);
            border.setStrokeWidth(10);
            canvas.drawRect(5, 5, size - 5, size - 5, border);

            // Monogram text
            Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setColor(Color.parseColor("#00ADB5"));
            textPaint.setTextSize(50);
            textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            textPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(label != null ? label : "KUET", size / 2f, size / 2f + 16, textPaint);

            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "KUET_Demo_" + System.currentTimeMillis() + ".png");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.Images.Media.IS_PENDING, 1);
            }

            ContentResolver resolver = context.getContentResolver();
            Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            if (imageUri != null) {
                try (OutputStream out = resolver.openOutputStream(imageUri)) {
                    if (out != null) {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear();
                    values.put(MediaStore.Images.Media.IS_PENDING, 0);
                    resolver.update(imageUri, values, null, null);
                }
                return imageUri;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to insert sample test image to MediaStore: " + e.getMessage());
        }
        return null;
    }
}

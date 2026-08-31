package com.example.util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.OpenableColumns;
import androidx.exifinterface.media.ExifInterface;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileUtils {

    public static String getFileName(Context context, Uri uri) {
        String result = null;
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) {
                        result = cursor.getString(index);
                    }
                }
            } catch (Exception ignored) {}
        }
        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
        }
        return result != null ? result : "media_" + System.currentTimeMillis();
    }

    public static long getFileSize(Context context, Uri uri) {
        long size = 0;
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.SIZE);
                    if (index != -1) {
                        size = cursor.getLong(index);
                    }
                }
            } catch (Exception ignored) {}
        }
        if (size <= 0) {
            try (InputStream is = context.getContentResolver().openInputStream(uri)) {
                if (is != null) {
                    size = is.available();
                }
            } catch (Exception ignored) {}
        }
        return size;
    }

    public static String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        if (digitGroups >= units.length) digitGroups = units.length - 1;
        return new DecimalFormat("#,##0.#").format(bytes / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public static String formatDuration(long durationMs) {
        if (durationMs <= 0) return "0:00";
        long totalSeconds = durationMs / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }

    public static String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public static String formatChatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public static String extractExifSummary(Context context, Uri uri) {
        StringBuilder sb = new StringBuilder();
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            if (inputStream != null) {
                ExifInterface exif = new ExifInterface(inputStream);
                String make = exif.getAttribute(ExifInterface.TAG_MAKE);
                String model = exif.getAttribute(ExifInterface.TAG_MODEL);
                String iso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY);
                String fNumber = exif.getAttribute(ExifInterface.TAG_F_NUMBER);
                String exp = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME);

                if (make != null || model != null) {
                    sb.append("Camera: ").append(make != null ? make : "").append(" ").append(model != null ? model : "").append("\n");
                }
                if (iso != null) sb.append("ISO: ").append(iso).append(" • ");
                if (fNumber != null) sb.append("f/").append(fNumber).append(" • ");
                if (exp != null) sb.append("Exp: ").append(exp).append("s");
            }
        } catch (Exception ignored) {}
        return sb.toString().trim();
    }

    public static VideoDetails extractVideoDetails(Context context, Uri uri) {
        VideoDetails details = new VideoDetails();
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(context, uri);
            String durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            String widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            String heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            String bitrateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
            String fpsStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE);

            if (durStr != null) details.durationMs = Long.parseLong(durStr);
            if (widthStr != null) details.width = Integer.parseInt(widthStr);
            if (heightStr != null) details.height = Integer.parseInt(heightStr);
            if (bitrateStr != null) details.bitrate = Long.parseLong(bitrateStr);
            if (fpsStr != null) details.fps = Float.parseFloat(fpsStr);
            else details.fps = 30.0f;
        } catch (Exception ignored) {
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {}
        }
        return details;
    }

    public static class VideoDetails {
        public long durationMs = 0;
        public int width = 0;
        public int height = 0;
        public long bitrate = 0;
        public float fps = 30f;
    }

    /** Real, full-file SHA-256 checksum (not a truncated/partial approximation). */
    public static String calculateChecksum(Context context, Uri uri) {
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            if (is == null) return null;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[65536];
            int read;
            while ((read = is.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            byte[] hash = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString().toUpperCase(Locale.ROOT);
        } catch (Exception e) {
            return null;
        }
    }

    public static File saveStreamToCache(Context context, InputStream in, String fileName) {
        try {
            File cacheFile = new File(context.getExternalCacheDir(), fileName);
            try (OutputStream out = new FileOutputStream(cacheFile)) {
                byte[] buf = new byte[16384];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
            return cacheFile;
        } catch (Exception e) {
            return null;
        }
    }
}

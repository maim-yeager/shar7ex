package com.example.util;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import com.example.model.TransferItem;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Real sharing helpers - opens the actual Android Share Sheet instead of
 * silently copying a fabricated link to the clipboard, and really writes
 * files to the device instead of just showing a "saved" toast.
 */
public class ShareUtils {

    private static final ExecutorService IO_EXECUTOR = Executors.newSingleThreadExecutor();

    private ShareUtils() {}

    /** Opens the real system Share Sheet for a transfer: the cloud link if uploaded, otherwise the local file itself. */
    public static void shareTransferItem(Context context, TransferItem item) {
        if (item == null) {
            Toast.makeText(context, "Nothing to share yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (item.getDownloadUrl() != null && !item.getDownloadUrl().isEmpty()) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, item.getFileName());
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Sent via M.SHAREX (100% original quality): " + item.getDownloadUrl());
            context.startActivity(Intent.createChooser(shareIntent, "Share " + item.getFileName()));
            return;
        }

        if (item.getLocalUri() != null && !item.getLocalUri().isEmpty()) {
            shareLocalUri(context, Uri.parse(item.getLocalUri()), item.getMimeType(), item.getFileName());
            return;
        }

        Toast.makeText(context, "This file hasn't finished uploading yet.", Toast.LENGTH_SHORT).show();
    }

    /** Shares a local content:// or file:// uri through the real system Share Sheet. */
    public static void shareLocalUri(Context context, Uri uri, String mimeType, String fileName) {
        Uri shareableUri = uri;
        if ("file".equals(uri.getScheme())) {
            File file = new File(uri.getPath());
            shareableUri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
        }
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType(mimeType != null ? mimeType : "*/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, shareableUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(shareIntent, "Share " + (fileName != null ? fileName : "file")));
    }

    public interface DownloadCallback {
        void onComplete(boolean success, String message);
    }

    /**
     * Really saves a transfer's media into the device's public Downloads/Pictures/Movies
     * (via MediaStore) - either by copying the already-local file or by downloading the
     * uploaded copy from Firebase Storage.
     */
    public static void downloadTransferItem(Context context, TransferItem item, DownloadCallback callback) {
        IO_EXECUTOR.execute(() -> {
            try {
                InputStream input;
                if (item.getLocalUri() != null && !item.getLocalUri().isEmpty()) {
                    input = context.getContentResolver().openInputStream(Uri.parse(item.getLocalUri()));
                } else if (item.getDownloadUrl() != null && !item.getDownloadUrl().isEmpty()) {
                    HttpURLConnection connection = (HttpURLConnection) new URL(item.getDownloadUrl()).openConnection();
                    connection.connect();
                    input = connection.getInputStream();
                } else {
                    postResult(callback, false, "No file available to download.");
                    return;
                }

                boolean isVideo = item.isVideo();
                String fileName = item.getFileName() != null ? item.getFileName() : ("sharex_" + System.currentTimeMillis());
                String mime = item.getMimeType() != null ? item.getMimeType() : (isVideo ? "video/mp4" : "image/jpeg");

                Uri outUri;
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, mime);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.put(MediaStore.MediaColumns.RELATIVE_PATH, isVideo ? Environment.DIRECTORY_MOVIES + "/MSHAREX" : Environment.DIRECTORY_PICTURES + "/MSHAREX");
                    Uri collection = isVideo ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    outUri = context.getContentResolver().insert(collection, values);
                } else {
                    File dir = new File(Environment.getExternalStoragePublicDirectory(
                            isVideo ? Environment.DIRECTORY_MOVIES : Environment.DIRECTORY_PICTURES), "MSHAREX");
                    if (!dir.exists()) dir.mkdirs();
                    File outFile = new File(dir, fileName);
                    outUri = Uri.fromFile(outFile);
                }

                if (outUri == null) {
                    postResult(callback, false, "Could not create destination file.");
                    return;
                }

                try (OutputStream output = context.getContentResolver().openOutputStream(outUri)) {
                    byte[] buffer = new byte[65536];
                    int read;
                    while ((read = input.read(buffer)) != -1) {
                        output.write(buffer, 0, read);
                    }
                }
                input.close();

                postResult(callback, true, "Saved " + fileName + " to your device in original quality.");
            } catch (Exception e) {
                postResult(callback, false, "Download failed: " + e.getMessage());
            }
        });
    }

    /** Saves an already-local File (e.g. one just received over Nearby Connections) into public storage. */
    public static void saveLocalFileToPublicStorage(Context context, File sourceFile, String fileName, String mimeType, DownloadCallback callback) {
        IO_EXECUTOR.execute(() -> {
            try (InputStream input = new java.io.FileInputStream(sourceFile)) {
                boolean isVideo = mimeType != null && mimeType.startsWith("video");
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType != null ? mimeType : "application/octet-stream");

                Uri outUri;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.put(MediaStore.MediaColumns.RELATIVE_PATH, isVideo ? Environment.DIRECTORY_MOVIES + "/MSHAREX" : Environment.DIRECTORY_PICTURES + "/MSHAREX");
                    Uri collection = isVideo ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    outUri = context.getContentResolver().insert(collection, values);
                } else {
                    File dir = new File(Environment.getExternalStoragePublicDirectory(
                            isVideo ? Environment.DIRECTORY_MOVIES : Environment.DIRECTORY_PICTURES), "MSHAREX");
                    if (!dir.exists()) dir.mkdirs();
                    outUri = Uri.fromFile(new File(dir, fileName));
                }

                if (outUri == null) {
                    postResult(callback, false, "Could not create destination file.");
                    return;
                }

                try (OutputStream output = context.getContentResolver().openOutputStream(outUri)) {
                    byte[] buffer = new byte[65536];
                    int read;
                    while ((read = input.read(buffer)) != -1) {
                        output.write(buffer, 0, read);
                    }
                }
                postResult(callback, true, "Received " + fileName + " and saved to your gallery.");
            } catch (Exception e) {
                postResult(callback, false, "Failed to save received file: " + e.getMessage());
            }
        });
    }

    private static void postResult(DownloadCallback callback, boolean success, String message) {
        if (callback == null) return;
        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        mainHandler.post(() -> callback.onComplete(success, message));
    }
}

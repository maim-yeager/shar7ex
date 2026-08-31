package com.example.cloud;

import android.content.Context;
import com.example.BuildConfig;
import java.io.IOException;
import java.io.InputStream;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;
import org.json.JSONObject;

/**
 * Uploads real files to Cloudinary's free tier (no billing card required) using an
 * UNSIGNED upload preset, instead of Firebase Storage (which now requires the paid
 * Blaze plan). This talks directly to Cloudinary's public REST API over HTTPS - no
 * custom backend needed, same as how Firebase Storage was called directly before.
 */
public class CloudinaryUploader {

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder().build();

    public interface UploadCallback {
        void onProgress(long uploadedBytes, long totalBytes);
        void onSuccess(String secureUrl, long bytes);
        void onError(String message);
    }

    /** Returns null if the app isn't configured yet (see .env.example / README). */
    public static boolean isConfigured(Context context) {
        String cloudName = safeString(BuildConfig.CLOUDINARY_CLOUD_NAME);
        String preset = safeString(BuildConfig.CLOUDINARY_UPLOAD_PRESET);
        return !cloudName.isEmpty() && !preset.isEmpty();
    }

    /**
     * @param stream        already-prepared media stream (OriginalQualityEngine may have
     *                      compressed it - this class doesn't care, it just uploads bytes)
     * @param knownLength   total bytes if known (-1 for unknown/chunked upload)
     */
    public static Call upload(Context context, InputStream stream, long knownLength, String fileName, String mimeType, UploadCallback callback) {
        String cloudName = safeString(BuildConfig.CLOUDINARY_CLOUD_NAME);
        String preset = safeString(BuildConfig.CLOUDINARY_UPLOAD_PRESET);

        if (cloudName.isEmpty() || preset.isEmpty()) {
            callback.onError("Cloudinary isn't configured yet - add CLOUDINARY_CLOUD_NAME and CLOUDINARY_UPLOAD_PRESET to your .env file (see README).");
            return null;
        }

        RequestBody fileBody = new StreamingRequestBody(stream, knownLength, mimeType, callback);

        MultipartBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("upload_preset", preset)
                .addFormDataPart("file", fileName != null ? fileName : "shared_file", fileBody)
                .build();

        // "auto" lets Cloudinary detect image/video/raw automatically - handles both photos and videos.
        Request request = new Request.Builder()
                .url("https://api.cloudinary.com/v1_1/" + cloudName + "/auto/upload")
                .post(body)
                .build();

        Call call = CLIENT.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (!call.isCanceled()) callback.onError("Upload failed: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) {
                try (Response r = response) {
                    String bodyStr = r.body() != null ? r.body().string() : "";
                    if (!r.isSuccessful()) {
                        callback.onError("Cloudinary rejected the upload (HTTP " + r.code() + "): " + bodyStr);
                        return;
                    }
                    JSONObject json = new JSONObject(bodyStr);
                    String secureUrl = json.optString("secure_url", null);
                    long bytes = json.optLong("bytes", knownLength);
                    if (secureUrl == null) {
                        callback.onError("Cloudinary response didn't include a secure_url.");
                        return;
                    }
                    callback.onSuccess(secureUrl, bytes);
                } catch (Exception e) {
                    callback.onError("Couldn't read Cloudinary's response: " + e.getMessage());
                }
            }
        });
        return call;
    }

    private static String safeString(String value) {
        return value != null ? value.trim() : "";
    }

    /** Streams the file straight from its InputStream into the HTTP request body, reporting real progress. */
    private static class StreamingRequestBody extends RequestBody {
        private final InputStream stream;
        private final long length;
        private final String mimeType;
        private final UploadCallback callback;

        StreamingRequestBody(InputStream stream, long length, String mimeType, UploadCallback callback) {
            this.stream = stream;
            this.length = length;
            this.mimeType = mimeType;
            this.callback = callback;
        }

        @Override
        public MediaType contentType() {
            return MediaType.parse(mimeType != null ? mimeType : "application/octet-stream");
        }

        @Override
        public long contentLength() {
            return length; // -1 tells OkHttp to use chunked transfer encoding
        }

        @Override
        public void writeTo(BufferedSink sink) throws IOException {
            byte[] buffer = new byte[64 * 1024];
            long uploaded = 0;
            int read;
            try {
                while ((read = stream.read(buffer)) != -1) {
                    sink.write(buffer, 0, read);
                    uploaded += read;
                    if (callback != null) callback.onProgress(uploaded, length);
                }
            } finally {
                stream.close();
            }
        }
    }
}

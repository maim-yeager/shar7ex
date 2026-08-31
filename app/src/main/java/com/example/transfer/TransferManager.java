package com.example.transfer;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import com.example.cloud.CloudinaryUploader;
import com.example.data.FirebaseManager;
import com.example.data.TransferRepository;
import com.example.model.TransferItem;
import com.example.util.FileUtils;
import com.example.util.NotificationHelper;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.Call;

/**
 * Performs REAL uploads to Cloudinary's free tier (not a local stream-and-discard
 * simulation) and writes a real, resolvable download URL as the share link.
 * Switched from Firebase Storage because Storage now requires the paid Blaze plan;
 * Cloudinary's free tier needs no billing card.
 */
public class TransferManager {

    private static TransferManager instance;
    private final Context context;
    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Map<String, Call> activeUploads = new HashMap<>();

    public interface TransferProgressListener {
        void onProgress(TransferItem item, int progress);
        void onCompleted(TransferItem item);
        void onError(TransferItem item, String errorMessage);
    }

    private TransferManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized TransferManager getInstance(Context context) {
        if (instance == null) {
            instance = new TransferManager(context);
        }
        return instance;
    }

    public void startUpload(Uri uri, OriginalQualityEngine.TransferMode mode, String receiverUid, String receiverName, TransferProgressListener listener) {
        String currentUid = FirebaseManager.getInstance(context).getCurrentUid();
        String currentName = FirebaseManager.getInstance(context).getCurrentUser() != null
                ? FirebaseManager.getInstance(context).getCurrentUser().getDisplayName() : "Me";

        String transferId = UUID.randomUUID().toString();

        TransferItem item = new TransferItem();
        item.setId(transferId);
        item.setLocalUri(uri.toString());
        item.setFileName(FileUtils.getFileName(context, uri));
        item.setOriginalSizeBytes(FileUtils.getFileSize(context, uri));
        item.setMimeType(context.getContentResolver().getType(uri));
        item.setSenderUid(currentUid);
        item.setSenderName(currentName);
        item.setReceiverUid(receiverUid);
        item.setReceiverName(receiverName);
        item.setStatus("UPLOADING");
        item.setProgress(0);
        item.setTimestamp(System.currentTimeMillis());

        TransferRepository.getInstance(context).addTransfer(item);

        if (!CloudinaryUploader.isConfigured(context)) {
            item.setStatus("FAILED");
            TransferRepository.getInstance(context).updateTransfer(item);
            if (listener != null) {
                mainHandler.post(() -> listener.onError(item, "Cloudinary isn't configured yet. Add CLOUDINARY_CLOUD_NAME and CLOUDINARY_UPLOAD_PRESET to your .env file - see README."));
            }
            return;
        }

        executor.execute(() -> {
            // Extract rich video/image metrics from the REAL file.
            if (item.isVideo()) {
                FileUtils.VideoDetails details = FileUtils.extractVideoDetails(context, uri);
                item.setOriginalWidth(details.width);
                item.setOriginalHeight(details.height);
                item.setDurationMs(details.durationMs);
                item.setVideoFps(details.fps);
                item.setVideoBitrate(details.bitrate);
                item.setExifSummary("Codec: H.264/HEVC • " + (details.width >= 3840 ? "4K UHD" : (details.width >= 1920 ? "1080p FHD" : "HD")) + " • " + (int) details.fps + " FPS");
            } else if (item.isImage()) {
                item.setExifSummary(FileUtils.extractExifSummary(context, uri));
            }
            item.setSha256Checksum(FileUtils.calculateChecksum(context, uri));

            int notifId = (int) System.currentTimeMillis();
            try {
                InputStream is = OriginalQualityEngine.prepareMediaStream(context, uri, mode, item);
                if (is == null) throw new IllegalStateException("Could not open input stream for file");

                // A ByteArrayInputStream (compressed-image path) knows its exact size up front;
                // otherwise use the real file size we already measured.
                long knownLength = (is instanceof ByteArrayInputStream) ? is.available() : item.getOriginalSizeBytes();
                String safeName = (item.getFileName() != null ? item.getFileName() : "file");

                Call call = CloudinaryUploader.upload(context, is, knownLength, safeName, item.getMimeType(), new CloudinaryUploader.UploadCallback() {
                    @Override
                    public void onProgress(long uploadedBytes, long totalBytes) {
                        long total = totalBytes > 0 ? totalBytes : item.getOriginalSizeBytes();
                        int progress = total > 0 ? (int) ((uploadedBytes * 100) / total) : 0;
                        if (progress > 100) progress = 100;
                        item.setProgress(progress);
                        NotificationHelper.showTransferProgress(context, notifId, "Sending " + item.getFileName(),
                                FileUtils.formatFileSize(uploadedBytes) + " / " + FileUtils.formatFileSize(total) +
                                        " (" + (item.isOriginalQuality() ? "100% Original" : "Compressed") + ")", progress);
                        int finalProgress = progress;
                        if (listener != null) mainHandler.post(() -> listener.onProgress(item, finalProgress));
                    }

                    @Override
                    public void onSuccess(String secureUrl, long bytes) {
                        activeUploads.remove(transferId);
                        item.setDownloadUrl(secureUrl);
                        item.setProgress(100);
                        item.setStatus("COMPLETED");
                        item.setShareLink(secureUrl);
                        item.setShareLinkExpiry(0); // Cloudinary links don't expire unless you delete the asset.

                        TransferRepository.getInstance(context).updateTransfer(item);
                        NotificationHelper.showTransferCompleted(context, notifId, "Transfer Completed", item.getFileName() + " sent at original 100% quality.");
                        bumpUserFileCounters(currentUid, true);

                        if (listener != null) mainHandler.post(() -> listener.onCompleted(item));
                    }

                    @Override
                    public void onError(String message) {
                        activeUploads.remove(transferId);
                        item.setStatus("FAILED");
                        TransferRepository.getInstance(context).updateTransfer(item);
                        NotificationHelper.cancelNotification(context, notifId);
                        if (listener != null) mainHandler.post(() -> listener.onError(item, message));
                    }
                });
                if (call != null) activeUploads.put(transferId, call);
            } catch (Exception e) {
                item.setStatus("FAILED");
                TransferRepository.getInstance(context).updateTransfer(item);
                if (listener != null) {
                    mainHandler.post(() -> listener.onError(item, e.getMessage()));
                }
            }
        });
    }

    private void bumpUserFileCounters(String uid, boolean sent) {
        if (uid == null) return;
        Map<String, Object> updates = new HashMap<>();
        updates.put(sent ? "totalSentFiles" : "totalReceivedFiles",
                com.google.firebase.firestore.FieldValue.increment(1));
        FirebaseManager.getInstance(context).getFirestore()
                .collection(FirebaseManager.COLLECTION_USERS).document(uid)
                .update(updates);
    }

    public void cancelTransfer(String transferId) {
        Call call = activeUploads.get(transferId);
        if (call != null) {
            call.cancel();
            activeUploads.remove(transferId);
        }
    }
}

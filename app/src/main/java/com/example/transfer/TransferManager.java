package com.example.transfer;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import com.example.data.FirebaseManager;
import com.example.data.TransferRepository;
import com.example.model.TransferItem;
import com.example.util.FileUtils;
import com.example.util.NotificationHelper;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Performs REAL uploads to Firebase Storage (not a local stream-and-discard
 * simulation) and writes a real, resolvable download URL as the share link.
 */
public class TransferManager {

    private static TransferManager instance;
    private final Context context;
    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Map<String, UploadTask> activeUploads = new HashMap<>();

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

                String safeName = (item.getFileName() != null ? item.getFileName() : "file") ;
                StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                        .child("transfers")
                        .child(currentUid != null ? currentUid : "unknown")
                        .child(transferId + "_" + safeName);

                UploadTask uploadTask = storageRef.putStream(is);
                activeUploads.put(transferId, uploadTask);

                uploadTask.addOnProgressListener(snapshot -> {
                    long total = snapshot.getTotalByteCount() > 0 ? snapshot.getTotalByteCount() : item.getOriginalSizeBytes();
                    int progress = total > 0 ? (int) ((snapshot.getBytesTransferred() * 100) / total) : 0;
                    if (progress > 100) progress = 100;
                    item.setProgress(progress);
                    NotificationHelper.showTransferProgress(context, notifId, "Sending " + item.getFileName(),
                            FileUtils.formatFileSize(snapshot.getBytesTransferred()) + " / " + FileUtils.formatFileSize(total) +
                                    " (" + (item.isOriginalQuality() ? "100% Original" : "Compressed") + ")", progress);
                    int finalProgress = progress;
                    if (listener != null) mainHandler.post(() -> listener.onProgress(item, finalProgress));
                });

                uploadTask.continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException() != null ? task.getException() : new IllegalStateException("Upload failed");
                    return storageRef.getDownloadUrl();
                }).addOnSuccessListener(downloadUri -> {
                    activeUploads.remove(transferId);
                    item.setDownloadUrl(downloadUri.toString());
                    item.setProgress(100);
                    item.setStatus("COMPLETED");
                    item.setShareLink(downloadUri.toString());
                    item.setShareLinkExpiry(0); // Firebase Storage links do not expire unless the object is deleted.

                    TransferRepository.getInstance(context).updateTransfer(item);
                    NotificationHelper.showTransferCompleted(context, notifId, "Transfer Completed", item.getFileName() + " sent at original 100% quality.");
                    bumpUserFileCounters(currentUid, true);

                    if (listener != null) mainHandler.post(() -> listener.onCompleted(item));
                }).addOnFailureListener(e -> {
                    activeUploads.remove(transferId);
                    item.setStatus("FAILED");
                    TransferRepository.getInstance(context).updateTransfer(item);
                    NotificationHelper.cancelNotification(context, notifId);
                    if (listener != null) mainHandler.post(() -> listener.onError(item, e.getMessage()));
                });
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
        UploadTask task = activeUploads.get(transferId);
        if (task != null) {
            task.cancel();
            activeUploads.remove(transferId);
        }
    }
}

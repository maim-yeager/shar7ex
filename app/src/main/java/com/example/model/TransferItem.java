package com.example.model;

import java.io.Serializable;

public class TransferItem implements Serializable {
    private String id;
    private String senderUid;
    private String senderName;
    private String receiverUid;
    private String receiverName;
    private String fileName;
    private String fileExtension;
    private String mimeType;
    private long originalSizeBytes;
    private int originalWidth;
    private int originalHeight;
    private long durationMs;
    private float videoFps;
    private long videoBitrate;
    private boolean originalQuality; // true = 100% untouched
    private String compressionLevel; // "NONE", "LOW", "MEDIUM", "HIGH"
    private String downloadUrl;
    private String localUri;
    private String localPath;
    private String storagePath;
    private int progress;
    private String status; // "UPLOADING", "COMPLETED", "FAILED", "PAUSED", "DOWNLOADING"
    private String shareLink;
    private long shareLinkExpiry;
    private long timestamp;
    private String exifSummary;
    private String sha256Checksum;
    private java.util.List<String> participants;

    public TransferItem() {
        this.originalQuality = true;
        this.compressionLevel = "NONE";
        this.status = "COMPLETED";
        this.timestamp = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSenderUid() { return senderUid; }
    public void setSenderUid(String senderUid) { this.senderUid = senderUid; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getReceiverUid() { return receiverUid; }
    public void setReceiverUid(String receiverUid) { this.receiverUid = receiverUid; }

    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileExtension() { return fileExtension; }
    public void setFileExtension(String fileExtension) { this.fileExtension = fileExtension; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public long getOriginalSizeBytes() { return originalSizeBytes; }
    public void setOriginalSizeBytes(long originalSizeBytes) { this.originalSizeBytes = originalSizeBytes; }

    public int getOriginalWidth() { return originalWidth; }
    public void setOriginalWidth(int originalWidth) { this.originalWidth = originalWidth; }

    public int getOriginalHeight() { return originalHeight; }
    public void setOriginalHeight(int originalHeight) { this.originalHeight = originalHeight; }

    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }

    public float getVideoFps() { return videoFps; }
    public void setVideoFps(float videoFps) { this.videoFps = videoFps; }

    public long getVideoBitrate() { return videoBitrate; }
    public void setVideoBitrate(long videoBitrate) { this.videoBitrate = videoBitrate; }

    public boolean isOriginalQuality() { return originalQuality; }
    public void setOriginalQuality(boolean originalQuality) { this.originalQuality = originalQuality; }

    public String getCompressionLevel() { return compressionLevel; }
    public void setCompressionLevel(String compressionLevel) { this.compressionLevel = compressionLevel; }

    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }

    public String getLocalUri() { return localUri; }
    public void setLocalUri(String localUri) { this.localUri = localUri; }

    public String getLocalPath() { return localPath; }
    public void setLocalPath(String localPath) { this.localPath = localPath; }

    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }

    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getShareLink() { return shareLink; }
    public void setShareLink(String shareLink) { this.shareLink = shareLink; }

    public long getShareLinkExpiry() { return shareLinkExpiry; }
    public void setShareLinkExpiry(long shareLinkExpiry) { this.shareLinkExpiry = shareLinkExpiry; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getExifSummary() { return exifSummary; }
    public void setExifSummary(String exifSummary) { this.exifSummary = exifSummary; }

    public String getSha256Checksum() { return sha256Checksum; }
    public void setSha256Checksum(String sha256Checksum) { this.sha256Checksum = sha256Checksum; }

    public java.util.List<String> getParticipants() { return participants; }
    public void setParticipants(java.util.List<String> participants) { this.participants = participants; }

    public boolean isVideo() {
        return (mimeType != null && mimeType.startsWith("video")) ||
                (fileName != null && (fileName.endsWith(".mp4") || fileName.endsWith(".mov") || fileName.endsWith(".mkv") || fileName.endsWith(".webm") || fileName.endsWith(".3gp")));
    }

    public boolean isImage() {
        return (mimeType != null && mimeType.startsWith("image")) ||
                (fileName != null && (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png") || fileName.endsWith(".webp") || fileName.endsWith(".heic") || fileName.endsWith(".raw") || fileName.endsWith(".dng")));
    }
}

package com.example.model;

import java.io.Serializable;

public class ChatMessage implements Serializable {
    private String messageId;
    private String chatId;
    private String senderId;
    private String senderName;
    private String receiverId;
    private String messageType; // "TEXT", "PHOTO", "VIDEO", "FILE"
    private String text;
    private String mediaUrl;
    private String mediaThumbUrl;
    private String localUri;
    private String fileName;
    private long fileSize;
    private int width;
    private int height;
    private long durationMs;
    private boolean originalQuality;
    private String status; // "SENDING", "SENT", "DELIVERED", "DOWNLOADED", "READ"
    private long timestamp;

    public ChatMessage() {
        this.originalQuality = true;
        this.status = "SENT";
        this.timestamp = System.currentTimeMillis();
    }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public String getMessageType() { return messageType != null ? messageType : "TEXT"; }
    public void setMessageType(String messageType) { this.messageType = messageType; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }

    public String getMediaThumbUrl() { return mediaThumbUrl; }
    public void setMediaThumbUrl(String mediaThumbUrl) { this.mediaThumbUrl = mediaThumbUrl; }

    public String getLocalUri() { return localUri; }
    public void setLocalUri(String localUri) { this.localUri = localUri; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }

    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }

    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }

    public boolean isOriginalQuality() { return originalQuality; }
    public void setOriginalQuality(boolean originalQuality) { this.originalQuality = originalQuality; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}

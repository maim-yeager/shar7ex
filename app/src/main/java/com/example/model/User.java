package com.example.model;

import java.io.Serializable;

public class User implements Serializable {
    private String uid;
    private String username;
    private String email;
    private String displayName;
    private String photoUrl;
    private boolean online;
    private long lastSeen;
    private int totalSentFiles;
    private int totalReceivedFiles;
    private long storageUsedBytes;
    private long createdAt;
    private String role; // "user", "admin"
    private boolean blocked;

    public User() {
        // Required for Firestore serialization
        this.role = "user";
        this.blocked = false;
    }

    public User(String uid, String username, String email, String displayName, String photoUrl) {
        this.uid = uid;
        this.username = username;
        this.email = email;
        this.displayName = displayName;
        this.photoUrl = photoUrl;
        this.online = true;
        this.lastSeen = System.currentTimeMillis();
        this.totalSentFiles = 0;
        this.totalReceivedFiles = 0;
        this.storageUsedBytes = 0;
        this.createdAt = System.currentTimeMillis();
        this.role = "user";
        this.blocked = false;
    }

    // Getters and Setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName != null && !displayName.isEmpty() ? displayName : username; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }

    public long getLastSeen() { return lastSeen; }
    public void setLastSeen(long lastSeen) { this.lastSeen = lastSeen; }

    public int getTotalSentFiles() { return totalSentFiles; }
    public void setTotalSentFiles(int totalSentFiles) { this.totalSentFiles = totalSentFiles; }

    public int getTotalReceivedFiles() { return totalReceivedFiles; }
    public void setTotalReceivedFiles(int totalReceivedFiles) { this.totalReceivedFiles = totalReceivedFiles; }

    public long getStorageUsedBytes() { return storageUsedBytes; }
    public void setStorageUsedBytes(long storageUsedBytes) { this.storageUsedBytes = storageUsedBytes; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isBlocked() { return blocked; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; }

    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(role);
    }
}

package com.example.model;

import java.io.Serializable;

public class SharingSession implements Serializable {
    private String sessionId;
    private String hostUid;
    private String hostName;
    private String hostPhoto;
    private String pinCode;
    private String qrToken;
    private boolean active;
    private long expiresAt;
    private String deviceName;
    private String connectedUid;
    private String connectedName;
    private int sharedFileCount;
    private long timestamp;

    public SharingSession() {
        this.active = true;
        this.timestamp = System.currentTimeMillis();
        this.expiresAt = System.currentTimeMillis() + (10 * 60 * 1000); // 10 mins
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getHostUid() { return hostUid; }
    public void setHostUid(String hostUid) { this.hostUid = hostUid; }

    public String getHostName() { return hostName; }
    public void setHostName(String hostName) { this.hostName = hostName; }

    public String getHostPhoto() { return hostPhoto; }
    public void setHostPhoto(String hostPhoto) { this.hostPhoto = hostPhoto; }

    public String getPinCode() { return pinCode; }
    public void setPinCode(String pinCode) { this.pinCode = pinCode; }

    public String getQrToken() { return qrToken; }
    public void setQrToken(String qrToken) { this.qrToken = qrToken; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(long expiresAt) { this.expiresAt = expiresAt; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public String getConnectedUid() { return connectedUid; }
    public void setConnectedUid(String connectedUid) { this.connectedUid = connectedUid; }

    public String getConnectedName() { return connectedName; }
    public void setConnectedName(String connectedName) { this.connectedName = connectedName; }

    public int getSharedFileCount() { return sharedFileCount; }
    public void setSharedFileCount(int sharedFileCount) { this.sharedFileCount = sharedFileCount; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}

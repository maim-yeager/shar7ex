package com.example.model;

import java.io.Serializable;

public class AdminStats implements Serializable {
    private int totalUsers;
    private int activeUsers;
    private int totalTransfers;
    private long totalStorageBytes;
    private int reportedUsersCount;

    public AdminStats() {}

    public int getTotalUsers() { return totalUsers; }
    public void setTotalUsers(int totalUsers) { this.totalUsers = totalUsers; }

    public int getActiveUsers() { return activeUsers; }
    public void setActiveUsers(int activeUsers) { this.activeUsers = activeUsers; }

    public int getTotalTransfers() { return totalTransfers; }
    public void setTotalTransfers(int totalTransfers) { this.totalTransfers = totalTransfers; }

    public long getTotalStorageBytes() { return totalStorageBytes; }
    public void setTotalStorageBytes(long totalStorageBytes) { this.totalStorageBytes = totalStorageBytes; }

    public int getReportedUsersCount() { return reportedUsersCount; }
    public void setReportedUsersCount(int reportedUsersCount) { this.reportedUsersCount = reportedUsersCount; }
}

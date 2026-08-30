package com.example.model;

import java.io.Serializable;
import java.util.List;

public class ChatConversation implements Serializable {
    private String chatId;
    private List<String> participantUids;
    private String lastMessage;
    private String lastMessageType;
    private long lastMessageTime;
    private int unreadCount;
    private String otherUid;
    private String otherUserName;
    private String otherUserPhoto;
    private boolean otherUserOnline;

    public ChatConversation() {}

    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }

    public List<String> getParticipantUids() { return participantUids; }
    public void setParticipantUids(List<String> participantUids) { this.participantUids = participantUids; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public String getLastMessageType() { return lastMessageType; }
    public void setLastMessageType(String lastMessageType) { this.lastMessageType = lastMessageType; }

    public long getLastMessageTime() { return lastMessageTime; }
    public void setLastMessageTime(long lastMessageTime) { this.lastMessageTime = lastMessageTime; }

    public int getUnreadCount() { return unreadCount; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }

    public String getOtherUid() { return otherUid; }
    public void setOtherUid(String otherUid) { this.otherUid = otherUid; }

    public String getOtherUserName() { return otherUserName != null ? otherUserName : "User"; }
    public void setOtherUserName(String otherUserName) { this.otherUserName = otherUserName; }

    public String getOtherUserPhoto() { return otherUserPhoto; }
    public void setOtherUserPhoto(String otherUserPhoto) { this.otherUserPhoto = otherUserPhoto; }

    public boolean isOtherUserOnline() { return otherUserOnline; }
    public void setOtherUserOnline(boolean otherUserOnline) { this.otherUserOnline = otherUserOnline; }
}

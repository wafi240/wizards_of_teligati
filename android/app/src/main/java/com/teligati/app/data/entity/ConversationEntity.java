package com.teligati.app.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "conversations")
public class ConversationEntity {
    @PrimaryKey
    private long id;
    private long otherParticipantId;
    private String otherParticipantName;
    private String otherParticipantEmail;
    private String otherParticipantRole;
    private String otherParticipantDept;
    private String lastMessageText;
    private long lastMessageAt;
    private long unreadCount;

    public ConversationEntity(long id, long otherParticipantId, String otherParticipantName,
                              String otherParticipantEmail, String otherParticipantRole,
                              String otherParticipantDept, String lastMessageText,
                              long lastMessageAt, long unreadCount) {
        this.id = id;
        this.otherParticipantId = otherParticipantId;
        this.otherParticipantName = otherParticipantName;
        this.otherParticipantEmail = otherParticipantEmail;
        this.otherParticipantRole = otherParticipantRole;
        this.otherParticipantDept = otherParticipantDept;
        this.lastMessageText = lastMessageText;
        this.lastMessageAt = lastMessageAt;
        this.unreadCount = unreadCount;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getOtherParticipantId() { return otherParticipantId; }
    public void setOtherParticipantId(long otherParticipantId) { this.otherParticipantId = otherParticipantId; }

    public String getOtherParticipantName() { return otherParticipantName; }
    public void setOtherParticipantName(String otherParticipantName) { this.otherParticipantName = otherParticipantName; }

    public String getOtherParticipantEmail() { return otherParticipantEmail; }
    public void setOtherParticipantEmail(String otherParticipantEmail) { this.otherParticipantEmail = otherParticipantEmail; }

    public String getOtherParticipantRole() { return otherParticipantRole; }
    public void setOtherParticipantRole(String otherParticipantRole) { this.otherParticipantRole = otherParticipantRole; }

    public String getOtherParticipantDept() { return otherParticipantDept; }
    public void setOtherParticipantDept(String otherParticipantDept) { this.otherParticipantDept = otherParticipantDept; }

    public String getLastMessageText() { return lastMessageText; }
    public void setLastMessageText(String lastMessageText) { this.lastMessageText = lastMessageText; }

    public long getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(long lastMessageAt) { this.lastMessageAt = lastMessageAt; }

    public long getUnreadCount() { return unreadCount; }
    public void setUnreadCount(long unreadCount) { this.unreadCount = unreadCount; }
}

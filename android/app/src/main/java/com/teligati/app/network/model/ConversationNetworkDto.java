package com.teligati.app.network.model;

public class ConversationNetworkDto {
    private Long id;
    private Long otherParticipantId;
    private String otherParticipantName;
    private String otherParticipantEmail;
    private String otherParticipantRole;
    private String otherParticipantDept;
    private String lastMessageText;
    private String lastMessageAt;
    private long unreadCount;
    private String createdAt;

    public ConversationNetworkDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getOtherParticipantId() { return otherParticipantId; }
    public void setOtherParticipantId(Long otherParticipantId) { this.otherParticipantId = otherParticipantId; }

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

    public String getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(String lastMessageAt) { this.lastMessageAt = lastMessageAt; }

    public long getUnreadCount() { return unreadCount; }
    public void setUnreadCount(long unreadCount) { this.unreadCount = unreadCount; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}

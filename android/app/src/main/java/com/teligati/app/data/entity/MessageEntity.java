package com.teligati.app.data.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "messages")
public class MessageEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private long conversationId;
    private String senderId;
    private String senderName;
    private String receiverId;
    private String receiverName;
    private String content;
    private long timestamp;
    private String formattedTime;
    private boolean isRead;

    public MessageEntity(long id, long conversationId, String senderId, String senderName,
                         String receiverId, String receiverName, String content,
                         long timestamp, String formattedTime, boolean isRead) {
        this.id = id;
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.receiverId = receiverId;
        this.receiverName = receiverName;
        this.content = content;
        this.timestamp = timestamp;
        this.formattedTime = formattedTime;
        this.isRead = isRead;
    }

    @Ignore
    public MessageEntity(long id, String senderId, String senderName, String receiverId,
                         String receiverName, String content, long timestamp, String formattedTime) {
        this(id, 0L, senderId, senderName, receiverId, receiverName, content, timestamp, formattedTime, false);
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getConversationId() { return conversationId; }
    public void setConversationId(long conversationId) { this.conversationId = conversationId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getFormattedTime() { return formattedTime; }
    public void setFormattedTime(String formattedTime) { this.formattedTime = formattedTime; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
}

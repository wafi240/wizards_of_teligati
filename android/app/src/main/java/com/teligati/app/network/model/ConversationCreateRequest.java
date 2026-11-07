package com.teligati.app.network.model;

public class ConversationCreateRequest {
    private Long recipientId;
    private String initialMessage;

    public ConversationCreateRequest() {}

    public ConversationCreateRequest(Long recipientId, String initialMessage) {
        this.recipientId = recipientId;
        this.initialMessage = initialMessage;
    }

    public Long getRecipientId() { return recipientId; }
    public void setRecipientId(Long recipientId) { this.recipientId = recipientId; }

    public String getInitialMessage() { return initialMessage; }
    public void setInitialMessage(String initialMessage) { this.initialMessage = initialMessage; }
}

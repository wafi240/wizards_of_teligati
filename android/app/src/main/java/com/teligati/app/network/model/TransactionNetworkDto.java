package com.teligati.app.network.model;

public class TransactionNetworkDto {
    private Long id;
    private String transactionId;
    private String title;
    private String category;
    private double amount;
    private String type;
    private String status;
    private String timestamp;
    private String completedAt;
    private Long userId;
    private Long escrowId;
    private String disclaimer;
    private String demoNotice;

    public TransactionNetworkDto() {}

    public TransactionNetworkDto(Long id, String transactionId, String title, String category, double amount, String type, String status, String timestamp, String completedAt, Long userId, Long escrowId, String disclaimer, String demoNotice) {
        this.id = id;
        this.transactionId = transactionId;
        this.title = title;
        this.category = category;
        this.amount = amount;
        this.type = type;
        this.status = status;
        this.timestamp = timestamp;
        this.completedAt = completedAt;
        this.userId = userId;
        this.escrowId = escrowId;
        this.disclaimer = disclaimer;
        this.demoNotice = demoNotice;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public String getCompletedAt() { return completedAt; }
    public void setCompletedAt(String completedAt) { this.completedAt = completedAt; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getEscrowId() { return escrowId; }
    public void setEscrowId(Long escrowId) { this.escrowId = escrowId; }
    public String getDisclaimer() { return disclaimer; }
    public void setDisclaimer(String disclaimer) { this.disclaimer = disclaimer; }
    public String getDemoNotice() { return demoNotice; }
    public void setDemoNotice(String demoNotice) { this.demoNotice = demoNotice; }
}

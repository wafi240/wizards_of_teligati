package com.teligati.app.network.model;

public class EscrowNetworkDto {
    private Long id;
    private String escrowReference;
    private String title;
    private String description;
    private double amount;
    private String status;
    private Long payerId;
    private String payerName;
    private Long payeeId;
    private String payeeName;
    private Long jobId;
    private String createdAt;
    private String updatedAt;
    private String disclaimer;
    private String demoNotice;

    public EscrowNetworkDto() {}

    public EscrowNetworkDto(Long id, String escrowReference, String title, String description, double amount, String status, Long payerId, String payerName, Long payeeId, String payeeName, Long jobId, String createdAt, String updatedAt, String disclaimer, String demoNotice) {
        this.id = id;
        this.escrowReference = escrowReference;
        this.title = title;
        this.description = description;
        this.amount = amount;
        this.status = status;
        this.payerId = payerId;
        this.payerName = payerName;
        this.payeeId = payeeId;
        this.payeeName = payeeName;
        this.jobId = jobId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.disclaimer = disclaimer;
        this.demoNotice = demoNotice;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEscrowReference() { return escrowReference; }
    public void setEscrowReference(String escrowReference) { this.escrowReference = escrowReference; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getPayerId() { return payerId; }
    public void setPayerId(Long payerId) { this.payerId = payerId; }
    public String getPayerName() { return payerName; }
    public void setPayerName(String payerName) { this.payerName = payerName; }
    public Long getPayeeId() { return payeeId; }
    public void setPayeeId(Long payeeId) { this.payeeId = payeeId; }
    public String getPayeeName() { return payeeName; }
    public void setPayeeName(String payeeName) { this.payeeName = payeeName; }
    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public String getDisclaimer() { return disclaimer; }
    public void setDisclaimer(String disclaimer) { this.disclaimer = disclaimer; }
    public String getDemoNotice() { return demoNotice; }
    public void setDemoNotice(String demoNotice) { this.demoNotice = demoNotice; }
}

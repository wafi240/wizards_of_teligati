package com.teligati.app.network.model;

public class EscrowCreateRequest {
    private String title;
    private String description;
    private double amount;
    private Long payeeId;
    private Long jobId;

    public EscrowCreateRequest(String title, String description, double amount, Long payeeId, Long jobId) {
        this.title = title;
        this.description = description;
        this.amount = amount;
        this.payeeId = payeeId;
        this.jobId = jobId;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public double getAmount() { return amount; }
    public Long getPayeeId() { return payeeId; }
    public Long getJobId() { return jobId; }
}

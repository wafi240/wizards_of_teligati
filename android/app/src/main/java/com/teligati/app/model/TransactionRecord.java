package com.teligati.app.model;

public class TransactionRecord {
    private String id;
    private String title;
    private String category;
    private double amount;
    private String date;
    private String status;

    public TransactionRecord(String id, String title, String category, double amount, String date, String status) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.amount = amount;
        this.date = date;
        this.status = status;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getCategory() { return category; }
    public double getAmount() { return amount; }
    public String getDate() { return date; }
    public String getStatus() { return status; }
}

package com.teligati.app.network.model;

public class TopUpRequest {
    private double amount;
    private String paymentMethod;
    private String note;

    public TopUpRequest(double amount, String paymentMethod, String note) {
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.note = note;
    }

    public double getAmount() { return amount; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getNote() { return note; }
}

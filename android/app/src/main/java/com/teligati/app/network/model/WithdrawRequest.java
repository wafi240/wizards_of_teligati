package com.teligati.app.network.model;

public class WithdrawRequest {
    private double amount;
    private String destinationAccount;
    private String note;

    public WithdrawRequest(double amount, String destinationAccount, String note) {
        this.amount = amount;
        this.destinationAccount = destinationAccount;
        this.note = note;
    }

    public double getAmount() { return amount; }
    public String getDestinationAccount() { return destinationAccount; }
    public String getNote() { return note; }
}

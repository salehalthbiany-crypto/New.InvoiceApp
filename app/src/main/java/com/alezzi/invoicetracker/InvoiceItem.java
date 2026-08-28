package com.alezzi.invoicetracker;

public class InvoiceItem {
    private long id;
    private double amount;
    private String details;
    private double balance;

    public InvoiceItem() {
        this.id = System.currentTimeMillis();
        this.amount = 0.0;
        this.details = "";
        this.balance = 0.0;
    }

    public InvoiceItem(double amount, String details, double balance) {
        this.id = System.currentTimeMillis();
        this.amount = amount;
        this.details = details;
        this.balance = balance;
    }

    public long getId() { return id; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
}
package com.example.demo.dto;

public class PaymentResponse {
    private Long id;
    private Long rideId;
    private double amount;
    private boolean paid;

    public PaymentResponse() {}

    public PaymentResponse(Long id, Long rideId, double amount, boolean paid) {
        this.id = id;
        this.rideId = rideId;
        this.amount = amount;
        this.paid = paid;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRideId() { return rideId; }
    public void setRideId(Long rideId) { this.rideId = rideId; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public boolean isPaid() { return paid; }
    public void setPaid(boolean paid) { this.paid = paid; }
}

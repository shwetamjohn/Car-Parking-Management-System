package com.parkingsystem.model;

/**
 * Model class representing the PAYMENTS table.
 * Linked to PARKING_TICKETS via ticketId (FK).
 * One payment is created per exit transaction.
 */
public class Payment {

    private int    paymentId;    // PK
    private double amount;       // computed parking charge in INR
    private String paymentTime;  // formatted timestamp from DB
    private String paymentMode;  // "CASH" | "CARD" | "UPI"
    private int    ticketId;     // FK → PARKING_TICKETS

    // ----- Constructors -----

    public Payment() {}

    public Payment(int paymentId, double amount, String paymentTime,
                   String paymentMode, int ticketId) {
        this.paymentId   = paymentId;
        this.amount      = amount;
        this.paymentTime = paymentTime;
        this.paymentMode = paymentMode;
        this.ticketId    = ticketId;
    }

    // ----- Getters -----

    public int    getPaymentId()   { return paymentId; }
    public double getAmount()      { return amount; }
    public String getPaymentTime() { return paymentTime; }
    public String getPaymentMode() { return paymentMode; }
    public int    getTicketId()    { return ticketId; }

    // ----- Setters -----

    public void setPaymentId(int paymentId)        { this.paymentId = paymentId; }
    public void setAmount(double amount)            { this.amount = amount; }
    public void setPaymentTime(String paymentTime)  { this.paymentTime = paymentTime; }
    public void setPaymentMode(String paymentMode)  { this.paymentMode = paymentMode; }
    public void setTicketId(int ticketId)           { this.ticketId = ticketId; }

    // ----- Utility -----

    @Override
    public String toString() {
        return "Payment{paymentId=" + paymentId +
                ", amount=" + amount +
                ", paymentTime='" + paymentTime + '\'' +
                ", paymentMode='" + paymentMode + '\'' +
                ", ticketId=" + ticketId + '}';
    }
}

package com.eventhall.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Class and object requirement: Payment instances record advances and installments.
public class Payment {
    private long id;
    private long bookingId;
    private BigDecimal amount;
    private PaymentType type;
    private LocalDateTime paymentDate;

    public Payment(long id, long bookingId, BigDecimal amount, PaymentType type, LocalDateTime paymentDate) {
        this.id = id;
        this.bookingId = bookingId;
        this.amount = amount;
        this.type = type;
        this.paymentDate = paymentDate;
    }

    public Payment(long bookingId, BigDecimal amount, PaymentType type) {
        this(0, bookingId, amount, type, LocalDateTime.now());
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getBookingId() { return bookingId; }
    public void setBookingId(long bookingId) { this.bookingId = bookingId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public PaymentType getType() { return type; }
    public void setType(PaymentType type) { this.type = type; }
    public LocalDateTime getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }
}
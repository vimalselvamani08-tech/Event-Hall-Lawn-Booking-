package com.eventhall.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import com.eventhall.exception.InvalidPaymentException;

// Encapsulation requirement: callers can change balance only by applying a validated payment.
// Class and object requirement: Booking instances connect customers, venues, dates, and payments.
public class Booking {
    private long id;
    private Customer customer;
    private Venue venue;
    private LocalDate eventDate;
    private String slot;
    private String eventType;
    private int guests;
    private BigDecimal totalAmount;
    private BigDecimal amountPaid;
    private BookingStatus status;

    // Constructor requirement: a new booking starts with its full total as the remaining balance.
    public Booking(Customer customer, Venue venue, LocalDate eventDate, String slot,
                   String eventType, int guests, BigDecimal totalAmount) {
        this(0, customer, venue, eventDate, slot, eventType, guests, totalAmount,
                BigDecimal.ZERO, BookingStatus.CONFIRMED);
    }

    public Booking(long id, Customer customer, Venue venue, LocalDate eventDate, String slot,
                   String eventType, int guests, BigDecimal totalAmount,
                   BigDecimal amountPaid, BookingStatus status) {
        this.id = id;
        this.customer = customer;
        this.venue = venue;
        this.eventDate = eventDate;
        this.slot = slot;
        this.eventType = eventType;
        this.guests = guests;
        this.totalAmount = totalAmount;
        this.amountPaid = amountPaid;
        this.status = status;
    }

    public void addPayment(BigDecimal amount) throws InvalidPaymentException {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidPaymentException("Payment must be greater than zero.");
        }
        if (amount.compareTo(getBalance()) > 0) {
            throw new InvalidPaymentException("Payment cannot exceed the remaining balance.");
        }
        amountPaid = amountPaid.add(amount);
        if (getBalance().signum() == 0) {
            status = BookingStatus.FULLY_PAID;
        }
    }

    public BigDecimal getBalance() { return totalAmount.subtract(amountPaid); }
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public Venue getVenue() { return venue; }
    public void setVenue(Venue venue) { this.venue = venue; }
    public LocalDate getEventDate() { return eventDate; }
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }
    public String getSlot() { return slot; }
    public void setSlot(String slot) { this.slot = slot; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public int getGuests() { return guests; }
    public void setGuests(int guests) { this.guests = guests; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getAmountPaid() { return amountPaid; }
    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }
}

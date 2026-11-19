package com.eventhall.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.eventhall.exception.InvalidPaymentException;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class BookingTest {
    @Test
    void appliesPaymentsAndMarksBookingFullyPaidAtZeroBalance() throws Exception {
        Customer customer = new Customer(1, "Test Customer", "555-0199", null, null);
        Venue venue = new MarriageHall(2, "Test Hall", 300, new BigDecimal("1000.00"), "", VenueStatus.ACTIVE);
        Booking booking = new Booking(customer, venue, LocalDate.now().plusDays(4), "EVENING",
                "Reception", 100, new BigDecimal("1000.00"));

        assertEquals(new BigDecimal("1000.00"), booking.getBalance());
        booking.addPayment(new BigDecimal("300.00"));
        assertEquals(new BigDecimal("700.00"), booking.getBalance());
        booking.addPayment(new BigDecimal("700.00"));
        assertEquals(BigDecimal.ZERO.setScale(2), booking.getBalance());
        assertEquals(BookingStatus.FULLY_PAID, booking.getStatus());
    }

    @Test
    void rejectsPaymentGreaterThanBalance() {
        Customer customer = new Customer(1, "Test Customer", "555-0199", null, null);
        Venue venue = new MarriageHall(2, "Test Hall", 300, new BigDecimal("1000.00"), "", VenueStatus.ACTIVE);
        Booking booking = new Booking(customer, venue, LocalDate.now().plusDays(4), "EVENING",
                "Reception", 100, new BigDecimal("1000.00"));

        InvalidPaymentException exception = assertThrows(InvalidPaymentException.class,
            () -> booking.addPayment(new BigDecimal("1000.01")));
        assertEquals("Payment cannot exceed the remaining balance.", exception.getMessage());
    }
}
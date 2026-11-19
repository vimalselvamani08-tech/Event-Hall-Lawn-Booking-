package com.eventhall.service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import com.eventhall.dao.BookingDao;
import com.eventhall.dao.CustomerDao;
import com.eventhall.dao.PaymentDao;
import com.eventhall.dao.VenueDao;
import com.eventhall.exception.CustomerNotFoundException;
import com.eventhall.exception.HallAlreadyBookedException;
import com.eventhall.exception.InvalidPaymentException;
import com.eventhall.model.Booking;
import com.eventhall.model.Customer;
import com.eventhall.model.Payment;
import com.eventhall.model.PaymentType;
import com.eventhall.model.Venue;
import com.eventhall.util.AuditLogger;
import com.eventhall.util.ConnectionProvider;
import com.eventhall.util.DBConnection;

public class BookingService {
    private final BookingDao bookingDao;
    private final CustomerDao customerDao;
    private final VenueDao venueDao;
    private final PaymentDao paymentDao;
    private final ConnectionProvider connectionProvider;

    public BookingService(BookingDao bookingDao, CustomerDao customerDao, VenueDao venueDao, PaymentDao paymentDao) {
        this(bookingDao, customerDao, venueDao, paymentDao, DBConnection::getConnection);
    }

    public BookingService(BookingDao bookingDao, CustomerDao customerDao, VenueDao venueDao, PaymentDao paymentDao,
                          ConnectionProvider connectionProvider) {
        this.bookingDao = bookingDao;
        this.customerDao = customerDao;
        this.venueDao = venueDao;
        this.paymentDao = paymentDao;
        this.connectionProvider = connectionProvider;
    }

    public long createBooking(long customerId, long venueId, LocalDate eventDate, String slot,
                              String eventType, int guests, BigDecimal advance)
            throws SQLException, CustomerNotFoundException, HallAlreadyBookedException, InvalidPaymentException {
        if (eventDate == null || eventDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Bookings cannot be made for past dates.");
        }
        if (slot == null || slot.isBlank()) throw new IllegalArgumentException("A time slot is required.");

        Customer customer = customerDao.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found."));
        Venue venue = venueDao.findById(venueId)
                .orElseThrow(() -> new IllegalArgumentException("Venue not found."));
        if (!"ACTIVE".equals(venue.getStatus().name())) {
            throw new IllegalArgumentException("The selected venue is inactive.");
        }
        if (guests <= 0 || guests > venue.getCapacity()) {
            throw new IllegalArgumentException("Guest count must be within venue capacity.");
        }
        BigDecimal total = venue.calculateRent(guests);
        if (advance == null || advance.signum() <= 0 || advance.compareTo(total) > 0) {
            throw new InvalidPaymentException("A positive advance no greater than the total is required.");
        }

        Booking booking = new Booking(customer, venue, eventDate, slot.trim(), eventType, guests, total);
        try (Connection connection = connectionProvider.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            try {
                connection.setAutoCommit(false);
                if (!bookingDao.isAvailable(connection, venueId, eventDate, slot.trim())) {
                    throw new HallAlreadyBookedException("Venue is already booked for the selected date and slot.");
                }
                long bookingId = bookingDao.create(connection, booking);
                booking.setId(bookingId);
                booking.addPayment(advance);
                paymentDao.create(connection, new Payment(bookingId, advance, PaymentType.ADVANCE));
                if (booking.getBalance().signum() == 0) {
                    bookingDao.updateStatus(connection, bookingId, booking.getStatus().name());
                }
                connection.commit();
                AuditLogger.log("BOOKING_CREATED", "bookingId=" + bookingId + ", venueId=" + venueId);
                return bookingId;
            } catch (HallAlreadyBookedException | InvalidPaymentException exception) {
                rollback(connection, exception);
                throw exception;
            } catch (SQLException exception) {
                rollback(connection, exception);
                if (isUniqueConstraintViolation(exception)) {
                    throw new HallAlreadyBookedException("Venue was booked by another request for that date and slot.");
                }
                throw exception;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        }
    }

    public TreeMap<LocalDate, ArrayList<Booking>> getSchedule() throws SQLException {
        TreeMap<LocalDate, ArrayList<Booking>> schedule = new TreeMap<>();
        for (Booking booking : bookingDao.findUpcoming()) {
            schedule.computeIfAbsent(booking.getEventDate(), ignored -> new ArrayList<>()).add(booking);
        }
        return schedule;
    }

    public List<LocalDate> findNextAvailableDates(long venueId, LocalDate from, String slot, int count)
            throws SQLException {
        List<LocalDate> dates = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection()) {
            for (LocalDate candidate = from.isBefore(LocalDate.now()) ? LocalDate.now() : from;
                 dates.size() < count; candidate = candidate.plusDays(1)) {
                if (bookingDao.isAvailable(connection, venueId, candidate, slot)) dates.add(candidate);
            }
        }
        return dates;
    }

    public boolean cancelBooking(long bookingId) throws SQLException {
        boolean changed = bookingDao.updateStatus(bookingId, "CANCELLED");
        if (changed) AuditLogger.log("BOOKING_CANCELLED", "bookingId=" + bookingId);
        return changed;
    }

    public List<Booking> findAll() throws SQLException { return bookingDao.findAll(); }

    public Booking findById(long bookingId) throws SQLException { return bookingDao.findById(bookingId); }

    public List<Booking> findCustomerHistory(long customerId) throws SQLException {
        return bookingDao.findCustomerHistory(customerId);
    }

    private static boolean isUniqueConstraintViolation(SQLException exception) {
        for (SQLException current = exception; current != null; current = current.getNextException()) {
            String message = current.getMessage();
            if (current.getErrorCode() == 19
                    && message != null && message.contains("UNIQUE constraint failed: bookings.venue_id")) {
                return true;
            }
            if (current.getErrorCode() == 1062
                    || (message != null && message.contains("uq_booking_venue_date_slot"))) {
                return true;
            }
        }
        return false;
    }

    private static void rollback(Connection connection, Exception original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            original.addSuppressed(rollbackFailure);
        }
    }
}

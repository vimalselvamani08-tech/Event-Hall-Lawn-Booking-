package com.eventhall.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import com.eventhall.model.Booking;
import com.eventhall.model.PaymentState;

public interface BookingDao {
    boolean isAvailable(Connection connection, long venueId, LocalDate date, String slot) throws SQLException;
    long create(Connection connection, Booking booking) throws SQLException;
    PaymentState lockPaymentState(Connection connection, long bookingId) throws SQLException;
    boolean updateStatus(Connection connection, long bookingId, String status) throws SQLException;
    Booking findById(long id) throws SQLException;
    List<Booking> findAll() throws SQLException;
    List<Booking> findUpcoming() throws SQLException;
    List<Booking> findPendingPayments() throws SQLException;
    List<Booking> findCustomerHistory(long customerId) throws SQLException;
    boolean updateStatus(long bookingId, String status) throws SQLException;
}
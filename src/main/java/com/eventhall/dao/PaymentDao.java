package com.eventhall.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import com.eventhall.model.Payment;

public interface PaymentDao {
    long create(Connection connection, Payment payment) throws SQLException;
    java.math.BigDecimal sumForBooking(Connection connection, long bookingId) throws SQLException;
    List<Payment> findByBooking(long bookingId) throws SQLException;
    List<Payment> findByCustomer(long customerId) throws SQLException;
}
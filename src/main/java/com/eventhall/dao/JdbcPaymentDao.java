package com.eventhall.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.eventhall.model.Payment;
import com.eventhall.model.PaymentType;
import com.eventhall.util.DBConnection;

public class JdbcPaymentDao implements PaymentDao {
    @Override
    public long create(Connection connection, Payment payment) throws SQLException {
        String sql = "INSERT INTO payments (booking_id, amount, payment_type, payment_date) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, payment.getBookingId());
            statement.setBigDecimal(2, payment.getAmount());
            statement.setString(3, payment.getType().name());
            statement.setString(4, payment.getPaymentDate().toString());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Creating a payment did not return an id.");
                return keys.getLong(1);
            }
        }
    }

    @Override
    public BigDecimal sumForBooking(Connection connection, long bookingId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COALESCE(SUM(amount), 0) FROM payments WHERE booking_id = ?")) {
            statement.setLong(1, bookingId);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getBigDecimal(1);
            }
        }
    }

    @Override
    public List<Payment> findByBooking(long bookingId) throws SQLException {
        String sql = "SELECT * FROM payments WHERE booking_id = ? ORDER BY payment_date";
        try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, bookingId);
            try (ResultSet results = statement.executeQuery()) { return mapAll(results); }
        }
    }

    @Override
    public List<Payment> findByCustomer(long customerId) throws SQLException {
        String sql = "SELECT p.* FROM payments p JOIN bookings b ON b.id = p.booking_id "
                + "WHERE b.customer_id = ? ORDER BY p.payment_date DESC";
        try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            try (ResultSet results = statement.executeQuery()) { return mapAll(results); }
        }
    }

    private List<Payment> mapAll(ResultSet results) throws SQLException {
        List<Payment> payments = new ArrayList<>();
        while (results.next()) {
            payments.add(new Payment(results.getLong("id"), results.getLong("booking_id"),
                    results.getBigDecimal("amount"), PaymentType.valueOf(results.getString("payment_type")),
                    java.time.LocalDateTime.parse(results.getString("payment_date").replace(' ', 'T'))));
        }
        return payments;
    }
}
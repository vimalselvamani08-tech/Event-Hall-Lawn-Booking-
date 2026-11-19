package com.eventhall.service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;

import com.eventhall.dao.BookingDao;
import com.eventhall.dao.PaymentDao;
import com.eventhall.exception.InvalidPaymentException;
import com.eventhall.model.Payment;
import com.eventhall.model.BookingStatus;
import com.eventhall.model.PaymentType;
import com.eventhall.model.PaymentState;
import com.eventhall.util.AuditLogger;
import com.eventhall.util.ConnectionProvider;
import com.eventhall.util.DBConnection;

public class PaymentService {
    private final BookingDao bookingDao;
    private final PaymentDao paymentDao;
    private final ConnectionProvider connectionProvider;

    public PaymentService(BookingDao bookingDao, PaymentDao paymentDao) {
        this(bookingDao, paymentDao, DBConnection::getConnection);
    }

    public PaymentService(BookingDao bookingDao, PaymentDao paymentDao, ConnectionProvider connectionProvider) {
        this.bookingDao = bookingDao;
        this.paymentDao = paymentDao;
        this.connectionProvider = connectionProvider;
    }

    public long recordInstallment(long bookingId, BigDecimal amount)
            throws SQLException, InvalidPaymentException {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidPaymentException("Payment must be greater than zero.");
        }
        try (Connection connection = connectionProvider.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            try {
                connection.setAutoCommit(false);
                PaymentState state = bookingDao.lockPaymentState(connection, bookingId);
                if (state == null) throw new InvalidPaymentException("Booking was not found.");
                if (state.status() == BookingStatus.CANCELLED || state.status() == BookingStatus.COMPLETED) {
                    throw new InvalidPaymentException("Payments cannot be added to a cancelled or completed booking.");
                }
                BigDecimal paid = paymentDao.sumForBooking(connection, bookingId);
                BigDecimal balance = state.totalAmount().subtract(paid);
                if (amount.compareTo(balance) > 0) {
                    throw new InvalidPaymentException("Payment cannot exceed the remaining balance of " + balance + ".");
                }
                Payment payment = new Payment(bookingId, amount, PaymentType.INSTALLMENT);
                long paymentId = paymentDao.create(connection, payment);
                if (amount.compareTo(balance) == 0) {
                    bookingDao.updateStatus(connection, bookingId, BookingStatus.FULLY_PAID.name());
                }
                connection.commit();
                AuditLogger.log("PAYMENT_RECORDED", "bookingId=" + bookingId + ", amount=" + amount);
                return paymentId;
            } catch (InvalidPaymentException | SQLException exception) {
                rollback(connection, exception);
                throw exception;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        }
    }

    private static void rollback(Connection connection, Exception original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            original.addSuppressed(rollbackFailure);
        }
    }
}
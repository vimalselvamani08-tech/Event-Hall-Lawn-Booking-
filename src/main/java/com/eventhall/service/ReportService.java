package com.eventhall.service;

import com.eventhall.dao.BookingDao;
import com.eventhall.dao.PaymentDao;
import com.eventhall.model.Booking;
import com.eventhall.model.Payment;
import com.eventhall.util.FileRecords;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;

public class ReportService {
    private final BookingDao bookingDao;
    private final PaymentDao paymentDao;

    public ReportService(BookingDao bookingDao, PaymentDao paymentDao) {
        this.bookingDao = bookingDao;
        this.paymentDao = paymentDao;
    }

    public List<Booking> pendingPayments() throws SQLException { return bookingDao.findPendingPayments(); }
    public List<Booking> upcomingEvents() throws SQLException { return bookingDao.findUpcoming(); }
    public List<Booking> bookingHistory() throws SQLException { return bookingDao.findAll(); }
    public List<Booking> customerHistory(long customerId) throws SQLException {
        return bookingDao.findCustomerHistory(customerId);
    }
    public List<Payment> customerPayments(long customerId) throws SQLException {
        return paymentDao.findByCustomer(customerId);
    }
    public List<Payment> bookingPayments(long bookingId) throws SQLException { return paymentDao.findByBooking(bookingId); }
    public Path exportBookings() throws SQLException, IOException {
        return FileRecords.exportBookings(bookingDao.findAll());
    }
}
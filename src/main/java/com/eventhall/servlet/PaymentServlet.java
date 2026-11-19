package com.eventhall.servlet;

import com.eventhall.model.Booking;
import com.eventhall.model.Payment;
import com.eventhall.model.PaymentType;
import com.eventhall.service.AppServices;
import com.eventhall.util.FileRecords;
import com.eventhall.util.AuditLogger;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;

@WebServlet("/admin/payments")
public class PaymentServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            long bookingId = Long.parseLong(request.getParameter("bookingId"));
            Booking booking = AppServices.booking().findById(bookingId);
            if (booking == null) { response.sendError(404); return; }
            request.setAttribute("booking", booking);
            request.setAttribute("payments", AppServices.report().bookingPayments(bookingId));
            request.getRequestDispatcher("/WEB-INF/views/payments.jsp").forward(request, response);
        } catch (SQLException | NumberFormatException exception) {
            throw new ServletException("Unable to load payment details.", exception);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        long bookingId = Long.parseLong(request.getParameter("bookingId"));
        try {
            BigDecimal amount = new BigDecimal(request.getParameter("amount"));
            long paymentId = AppServices.payment().recordInstallment(bookingId, amount);
            Booking booking = AppServices.booking().findById(bookingId);
            Payment payment = new Payment(paymentId, bookingId, amount, PaymentType.INSTALLMENT,
                    java.time.LocalDateTime.now());
            try {
                FileRecords.writeReceipt(booking, payment);
            } catch (IOException receiptFailure) {
                AuditLogger.log("RECEIPT_ERROR", "bookingId=" + bookingId + ": " + receiptFailure.getMessage());
            }
            response.sendRedirect(request.getContextPath() + "/admin/payments?bookingId=" + bookingId + "&saved=1");
        } catch (com.eventhall.exception.InvalidPaymentException | SQLException | NumberFormatException exception) {
            request.setAttribute("error", exception.getMessage());
            doGet(request, response);
        }
    }
}
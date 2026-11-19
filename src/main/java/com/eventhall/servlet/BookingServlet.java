package com.eventhall.servlet;

import com.eventhall.exception.CustomerNotFoundException;
import com.eventhall.exception.HallAlreadyBookedException;
import com.eventhall.exception.InvalidPaymentException;
import com.eventhall.service.AppServices;
import com.eventhall.model.Booking;
import com.eventhall.model.Payment;
import com.eventhall.util.AuditLogger;
import com.eventhall.util.FileRecords;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;

@WebServlet("/admin/bookings")
public class BookingServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            request.setAttribute("today", LocalDate.now());
            request.setAttribute("customers", AppServices.customer().findAll());
            request.setAttribute("venues", AppServices.venue().findAll());
            request.setAttribute("bookings", AppServices.booking().findAll());
            request.getRequestDispatcher("/WEB-INF/views/bookings.jsp").forward(request, response);
        } catch (SQLException exception) {
            throw new ServletException("Unable to load bookings.", exception);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            if ("cancel".equals(request.getParameter("action"))) {
                AppServices.booking().cancelBooking(Long.parseLong(request.getParameter("bookingId")));
            } else {
                long bookingId = AppServices.booking().createBooking(
                        Long.parseLong(request.getParameter("customerId")),
                        Long.parseLong(request.getParameter("venueId")),
                        LocalDate.parse(request.getParameter("eventDate")), request.getParameter("slot"),
                        request.getParameter("eventType"), Integer.parseInt(request.getParameter("guests")),
                        new BigDecimal(request.getParameter("advance")));
                try {
                    Booking booking = AppServices.booking().findById(bookingId);
                    Payment advancePayment = AppServices.report().bookingPayments(bookingId).get(0);
                    FileRecords.writeReceipt(booking, advancePayment);
                } catch (IOException | SQLException receiptFailure) {
                    AuditLogger.log("RECEIPT_ERROR", "bookingId=" + bookingId + ": " + receiptFailure.getMessage());
                }
                request.getSession().setAttribute("notice", "Booking " + bookingId + " confirmed with advance payment.");
            }
            response.sendRedirect(request.getContextPath() + "/admin/bookings");
        } catch (HallAlreadyBookedException exception) {
            try {
                request.setAttribute("nextDates", AppServices.booking().findNextAvailableDates(
                        Long.parseLong(request.getParameter("venueId")), LocalDate.now(),
                        request.getParameter("slot"), 5));
            } catch (SQLException lookupFailure) {
                exception.addSuppressed(lookupFailure);
            }
            request.setAttribute("error", exception.getMessage());
            doGet(request, response);
        } catch (CustomerNotFoundException | InvalidPaymentException | SQLException | IllegalArgumentException exception) {
            request.setAttribute("error", exception.getMessage());
            doGet(request, response);
        }
    }
}
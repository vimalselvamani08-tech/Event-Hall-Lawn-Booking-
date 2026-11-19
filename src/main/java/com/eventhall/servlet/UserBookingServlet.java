package com.eventhall.servlet;

import com.eventhall.exception.CustomerNotFoundException;
import com.eventhall.exception.HallAlreadyBookedException;
import com.eventhall.exception.InvalidPaymentException;
import com.eventhall.model.Customer;
import com.eventhall.service.AppServices;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;

@WebServlet("/user/bookings")
public class UserBookingServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            Customer customer = (Customer) request.getSession(false).getAttribute("customer");
            request.setAttribute("today", LocalDate.now());
            request.setAttribute("venues", AppServices.venue().findAll().stream()
                    .filter(venue -> "ACTIVE".equals(venue.getStatus().name())).toList());
            request.setAttribute("bookings", AppServices.booking().findCustomerHistory(customer.getId()));
            request.getRequestDispatcher("/WEB-INF/views/user-bookings.jsp").forward(request, response);
        } catch (SQLException exception) {
            throw new ServletException("Unable to load your bookings.", exception);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            Customer customer = (Customer) request.getSession(false).getAttribute("customer");
            AppServices.booking().createBooking(customer.getId(),
                    Long.parseLong(request.getParameter("venueId")),
                    LocalDate.parse(request.getParameter("eventDate")), request.getParameter("slot"),
                    request.getParameter("eventType"), Integer.parseInt(request.getParameter("guests")),
                    new BigDecimal(request.getParameter("advance")));
            request.getSession().setAttribute("notice", "Your booking request is confirmed with the advance payment.");
            response.sendRedirect(request.getContextPath() + "/user/bookings");
        } catch (HallAlreadyBookedException exception) {
            try {
                request.setAttribute("nextDates", AppServices.booking().findNextAvailableDates(
                        Long.parseLong(request.getParameter("venueId")), LocalDate.now(),
                        request.getParameter("slot"), 5));
            } catch (SQLException ignored) {
                request.setAttribute("nextDates", java.util.List.of());
            }
            request.setAttribute("error", exception.getMessage());
            doGet(request, response);
        } catch (CustomerNotFoundException | InvalidPaymentException | IllegalArgumentException exception) {
            request.setAttribute("error", exception.getMessage());
            doGet(request, response);
        } catch (SQLException exception) {
            throw new ServletException("Unable to process your booking.", exception);
        }
    }
}
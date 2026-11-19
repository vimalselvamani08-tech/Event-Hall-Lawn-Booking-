package com.eventhall.servlet;

import com.eventhall.service.AppServices;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;

@WebServlet("/admin/reports")
public class ReportsServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if ("csv".equals(request.getParameter("export"))) {
            try {
                var file = AppServices.report().exportBookings();
                response.setContentType("text/csv");
                response.setHeader("Content-Disposition", "attachment; filename=bookings.csv");
                Files.copy(file, response.getOutputStream());
                return;
            } catch (SQLException exception) {
                throw new ServletException("Unable to export bookings.", exception);
            }
        }
        try {
            request.setAttribute("pending", AppServices.report().pendingPayments());
            request.setAttribute("upcoming", AppServices.report().upcomingEvents());
            request.setAttribute("history", AppServices.report().bookingHistory());
            request.setAttribute("customers", AppServices.customer().findAll());
            String customerId = request.getParameter("customerId");
            if (customerId != null && !customerId.isBlank()) {
                long id = Long.parseLong(customerId);
                request.setAttribute("customerHistory", AppServices.report().customerHistory(id));
                request.setAttribute("customerPayments", AppServices.report().customerPayments(id));
            }
            request.getRequestDispatcher("/WEB-INF/views/reports.jsp").forward(request, response);
        } catch (SQLException exception) {
            throw new ServletException("Unable to load reports.", exception);
        }
    }
}
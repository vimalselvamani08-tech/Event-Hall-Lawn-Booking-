package com.eventhall.servlet;

import com.eventhall.service.AppServices;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/admin/dashboard")
public class DashboardServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            request.setAttribute("upcoming", AppServices.booking().getSchedule());
            request.setAttribute("pending", AppServices.booking().findAll().stream()
                    .filter(booking -> "CONFIRMED".equals(booking.getStatus().name())
                            && booking.getBalance().signum() > 0).toList());
            request.getRequestDispatcher("/WEB-INF/views/dashboard.jsp").forward(request, response);
        } catch (SQLException exception) {
            throw new ServletException("Unable to load dashboard.", exception);
        }
    }
}
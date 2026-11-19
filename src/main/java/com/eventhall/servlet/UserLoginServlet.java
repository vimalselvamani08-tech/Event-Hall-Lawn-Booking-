package com.eventhall.servlet;

import com.eventhall.exception.InvalidLoginException;
import com.eventhall.model.Customer;
import com.eventhall.service.AppServices;
import com.eventhall.util.AuditLogger;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/user/login")
public class UserLoginServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/views/user-login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            Customer customer = AppServices.userAuthentication().authenticate(
                    request.getParameter("email"), request.getParameter("password"));
            request.getSession(true).setAttribute("customer", customer);
            request.getSession().removeAttribute("admin");
            request.changeSessionId();
            AuditLogger.log("CUSTOMER_LOGIN", "customerId=" + customer.getId());
            response.sendRedirect(request.getContextPath() + "/user/bookings");
        } catch (InvalidLoginException exception) {
            request.setAttribute("error", exception.getMessage());
            doGet(request, response);
        } catch (IllegalArgumentException exception) {
            request.setAttribute("error", exception.getMessage());
            doGet(request, response);
        } catch (SQLException exception) {
            AuditLogger.log("CUSTOMER_LOGIN_ERROR", exception.getMessage());
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Sign-in service unavailable.");
        }
    }
}
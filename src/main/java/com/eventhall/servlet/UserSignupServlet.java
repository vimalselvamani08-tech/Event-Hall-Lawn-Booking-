package com.eventhall.servlet;

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

@WebServlet("/user/signup")
public class UserSignupServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/views/user-signup.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String password = request.getParameter("password");
        if (password == null || !password.equals(request.getParameter("confirmPassword"))) {
            request.setAttribute("error", "The passwords do not match.");
            doGet(request, response);
            return;
        }
        try {
            Customer customer = AppServices.userAuthentication().register(
                    request.getParameter("name"), request.getParameter("phone"), request.getParameter("email"),
                    request.getParameter("address"), password);
                request.getSession(true).removeAttribute("admin");
            request.getSession(true).setAttribute("customer", customer);
            request.changeSessionId();
            AuditLogger.log("CUSTOMER_REGISTERED", "customerId=" + customer.getId());
            response.sendRedirect(request.getContextPath() + "/user/bookings");
        } catch (IllegalArgumentException exception) {
            request.setAttribute("error", exception.getMessage());
            doGet(request, response);
        } catch (SQLException exception) {
            AuditLogger.log("CUSTOMER_SIGNUP_ERROR", exception.getMessage());
            request.setAttribute("error", "Unable to create the account. This email may already be registered.");
            doGet(request, response);
        }
    }
}
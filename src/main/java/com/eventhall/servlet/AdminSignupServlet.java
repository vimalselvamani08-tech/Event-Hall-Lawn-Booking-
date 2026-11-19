package com.eventhall.servlet;

import com.eventhall.service.AppServices;
import com.eventhall.model.Admin;
import com.eventhall.util.AuditLogger;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/admin/signup")
public class AdminSignupServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/views/admin-signup.jsp").forward(request, response);
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
            AppServices.authentication().registerAdmin(request.getParameter("username"), password);
            Admin creator = (Admin) request.getSession().getAttribute("admin");
            AuditLogger.log("ADMIN_CREATED", "createdBy=" + creator.getUsername());
            request.setAttribute("success", "Administrator account created. The new admin can now sign in.");
            doGet(request, response);
        } catch (IllegalArgumentException exception) {
            request.setAttribute("error", exception.getMessage());
            doGet(request, response);
        } catch (SQLException exception) {
            AuditLogger.log("ADMIN_SIGNUP_ERROR", exception.getMessage());
            request.setAttribute("error", "That username may already be registered.");
            doGet(request, response);
        }
    }
}
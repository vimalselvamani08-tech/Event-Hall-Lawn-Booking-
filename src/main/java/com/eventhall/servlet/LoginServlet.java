package com.eventhall.servlet;

import com.eventhall.exception.InvalidLoginException;
import com.eventhall.model.Admin;
import com.eventhall.service.AppServices;
import com.eventhall.util.AuditLogger;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            Admin admin = AppServices.authentication().authenticate(request.getParameter("username"),
                    request.getParameter("password"));
            request.changeSessionId();
            request.getSession().removeAttribute("customer");
            request.getSession(true).setAttribute("admin", admin);
            AuditLogger.log("LOGIN", "username=" + admin.getUsername());
            response.sendRedirect(request.getContextPath() + "/admin/dashboard");
        } catch (InvalidLoginException exception) {
            request.setAttribute("error", exception.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
        } catch (SQLException exception) {
            AuditLogger.log("LOGIN_ERROR", exception.getMessage());
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Authentication service unavailable.");
        }
    }
}
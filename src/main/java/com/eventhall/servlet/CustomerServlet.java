package com.eventhall.servlet;

import com.eventhall.model.Customer;
import com.eventhall.service.AppServices;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/admin/customers")
public class CustomerServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            String query = request.getParameter("q");
            request.setAttribute("query", query == null ? "" : query);
            request.setAttribute("customers", query == null ? AppServices.customer().findAll()
                    : AppServices.customer().search(query));
                String editId = request.getParameter("edit");
                if (editId != null) {
                AppServices.customer().findById(Long.parseLong(editId))
                    .ifPresent(customer -> request.setAttribute("editing", customer));
                }
            request.getRequestDispatcher("/WEB-INF/views/customers.jsp").forward(request, response);
        } catch (SQLException exception) {
            throw new ServletException("Unable to load customers.", exception);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            String action = request.getParameter("action");
            if ("delete".equals(action)) {
                AppServices.customer().delete(Long.parseLong(request.getParameter("id")));
            } else {
                long id = request.getParameter("id").isBlank() ? 0 : Long.parseLong(request.getParameter("id"));
                Customer customer = new Customer(id, request.getParameter("name"), request.getParameter("phone"),
                        request.getParameter("email"), request.getParameter("address"));
                if (id == 0) AppServices.customer().create(customer); else AppServices.customer().update(customer);
            }
            response.sendRedirect(request.getContextPath() + "/admin/customers");
        } catch (SQLException | IllegalArgumentException exception) {
            request.setAttribute("error", "Customer could not be saved: " + exception.getMessage());
            doGet(request, response);
        }
    }
}
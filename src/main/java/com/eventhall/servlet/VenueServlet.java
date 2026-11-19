package com.eventhall.servlet;

import com.eventhall.model.CommunityCenter;
import com.eventhall.model.ConventionHall;
import com.eventhall.model.GardenLawn;
import com.eventhall.model.MarriageHall;
import com.eventhall.model.Venue;
import com.eventhall.model.VenueStatus;
import com.eventhall.service.AppServices;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;

@WebServlet("/admin/halls")
public class VenueServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            request.setAttribute("venues", AppServices.venue().findAll());
            String editId = request.getParameter("edit");
            if (editId != null) {
                AppServices.venue().findById(Long.parseLong(editId)).ifPresent(venue -> request.setAttribute("editing", venue));
            }
            request.getRequestDispatcher("/WEB-INF/views/halls.jsp").forward(request, response);
        } catch (SQLException exception) {
            throw new ServletException("Unable to load venues.", exception);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            String action = request.getParameter("action");
            if ("delete".equals(action)) {
                AppServices.venue().delete(Long.parseLong(request.getParameter("id")));
            } else {
                long id = parseLong(request.getParameter("id"), 0);
                String name = request.getParameter("name");
                int capacity = Integer.parseInt(request.getParameter("capacity"));
                BigDecimal rent = new BigDecimal(request.getParameter("rent"));
                String type = request.getParameter("type");
                String facilities = request.getParameter("facilities");
                String description = request.getParameter("description");
                String imageUrl = request.getParameter("imageUrl");
                VenueStatus status = VenueStatus.valueOf(request.getParameter("status"));
                Venue venue = switch (type) {
                    case "MARRIAGE_HALL" -> new MarriageHall(id, name, capacity, rent, facilities, description, imageUrl, status);
                    case "COMMUNITY_CENTER" -> new CommunityCenter(id, name, capacity, rent, facilities, description, imageUrl, status);
                    case "OUTDOOR_LAWN" -> new GardenLawn(id, name, capacity, rent, facilities, description, imageUrl, status);
                    case "INDOOR_CONVENTION_HALL" -> new ConventionHall(id, name, capacity, rent, facilities, description, imageUrl, status);
                    default -> throw new IllegalArgumentException("Unsupported venue type: " + type);
                };
                if (id == 0) AppServices.venue().create(venue); else AppServices.venue().update(venue);
            }
            response.sendRedirect(request.getContextPath() + "/admin/halls");
        } catch (SQLException | IllegalArgumentException exception) {
            request.setAttribute("error", "Venue could not be saved: " + exception.getMessage());
            doGet(request, response);
        }
    }

    private static long parseLong(String value, long fallback) {
        return value == null || value.isBlank() ? fallback : Long.parseLong(value);
    }
}
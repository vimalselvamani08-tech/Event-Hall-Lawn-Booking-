package com.eventhall.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import com.eventhall.model.Booking;
import com.eventhall.model.BookingStatus;
import com.eventhall.model.CommunityCenter;
import com.eventhall.model.Customer;
import com.eventhall.model.ConventionHall;
import com.eventhall.model.GardenLawn;
import com.eventhall.model.MarriageHall;
import com.eventhall.model.PaymentState;
import com.eventhall.model.Venue;
import com.eventhall.model.VenueStatus;
import com.eventhall.util.DBConnection;

public class JdbcBookingDao implements BookingDao {
    private static final String SELECT_BOOKINGS = "SELECT b.*, c.name customer_name, c.phone, c.email, c.address, "
            + "v.name venue_name, v.type, v.capacity, v.rent, v.facilities, v.status venue_status, "
            + "COALESCE(p.amount_paid, 0) amount_paid FROM bookings b "
            + "JOIN customers c ON c.id = b.customer_id JOIN venues v ON v.id = b.venue_id "
            + "LEFT JOIN (SELECT booking_id, SUM(amount) amount_paid FROM payments GROUP BY booking_id) p "
            + "ON p.booking_id = b.id ";

    @Override
    public boolean isAvailable(Connection connection, long venueId, LocalDate date, String slot) throws SQLException {
        String sql = "SELECT 1 FROM bookings WHERE venue_id = ? AND event_date = ? AND slot = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, venueId);
            statement.setString(2, date.toString());
            statement.setString(3, slot);
            try (ResultSet result = statement.executeQuery()) { return !result.next(); }
        }
    }

    @Override
    public long create(Connection connection, Booking booking) throws SQLException {
        String sql = "INSERT INTO bookings (customer_id, venue_id, event_date, slot, event_type, guests, total_amount, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, booking.getCustomer().getId());
            statement.setLong(2, booking.getVenue().getId());
            statement.setString(3, booking.getEventDate().toString());
            statement.setString(4, booking.getSlot());
            statement.setString(5, booking.getEventType());
            statement.setInt(6, booking.getGuests());
            statement.setBigDecimal(7, booking.getTotalAmount());
            statement.setString(8, booking.getStatus().name());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Creating a booking did not return an id.");
                return keys.getLong(1);
            }
        }
    }

    @Override
    public PaymentState lockPaymentState(Connection connection, long bookingId) throws SQLException {
        // SQLite has one writer at a time; this no-op update reserves the write lock before reading the balance.
        try (PreparedStatement lock = connection.prepareStatement(
                "UPDATE bookings SET status = status WHERE id = ?")) {
            lock.setLong(1, bookingId);
            if (lock.executeUpdate() == 0) return null;
        }
        String sql = "SELECT total_amount, status FROM bookings WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, bookingId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return null;
                return new PaymentState(result.getBigDecimal("total_amount"),
                        BookingStatus.valueOf(result.getString("status")));
            }
        }
    }

    @Override
    public boolean updateStatus(Connection connection, long bookingId, String status) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE bookings SET status = ? WHERE id = ?")) {
            statement.setString(1, status);
            statement.setLong(2, bookingId);
            return statement.executeUpdate() == 1;
        }
    }

    @Override
    public Booking findById(long id) throws SQLException {
        return queryOne(SELECT_BOOKINGS + "WHERE b.id = ?", id);
    }

    @Override
    public List<Booking> findAll() throws SQLException {
        return queryList(SELECT_BOOKINGS + "ORDER BY b.event_date DESC");
    }

    @Override
    public List<Booking> findUpcoming() throws SQLException {
        return queryList(SELECT_BOOKINGS + "WHERE b.event_date >= CURRENT_DATE AND b.status IN ('CONFIRMED', 'FULLY_PAID') "
            + "ORDER BY b.event_date");
    }

    @Override
    public List<Booking> findPendingPayments() throws SQLException {
        return queryList(SELECT_BOOKINGS + "WHERE b.status = 'CONFIRMED' "
            + "AND b.total_amount > COALESCE(p.amount_paid, 0) ORDER BY b.event_date");
    }

    @Override
    public List<Booking> findCustomerHistory(long customerId) throws SQLException {
        return queryList(SELECT_BOOKINGS + "WHERE b.customer_id = ? ORDER BY b.event_date DESC", customerId);
    }

    @Override
    public boolean updateStatus(long bookingId, String status) throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("UPDATE bookings SET status = ? WHERE id = ?")) {
            statement.setString(1, status);
            statement.setLong(2, bookingId);
            return statement.executeUpdate() == 1;
        }
    }

    private Booking queryOne(String sql, long id) throws SQLException {
        try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return null;
                return map(result);
            }
        }
    }

    private List<Booking> queryList(String sql, Object... parameters) throws SQLException {
        try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < parameters.length; index++) statement.setObject(index + 1, parameters[index]);
            try (ResultSet results = statement.executeQuery()) {
                List<Booking> bookings = new ArrayList<>();
                while (results.next()) bookings.add(map(results));
                return bookings;
            }
        }
    }

    private Booking map(ResultSet result) throws SQLException {
        Customer customer = new Customer(result.getLong("customer_id"), result.getString("customer_name"),
                result.getString("phone"), result.getString("email"), result.getString("address"));
        String type = result.getString("type");
        VenueStatus venueStatus = VenueStatus.valueOf(result.getString("venue_status"));
        Venue venue;
        if ("MARRIAGE_HALL".equals(type)) {
            venue = new MarriageHall(result.getLong("venue_id"), result.getString("venue_name"),
                    result.getInt("capacity"), result.getBigDecimal("rent"), result.getString("facilities"),
                    venueStatus);
        } else if ("COMMUNITY_CENTER".equals(type)) {
            venue = new CommunityCenter(result.getLong("venue_id"), result.getString("venue_name"),
                    result.getInt("capacity"), result.getBigDecimal("rent"), result.getString("facilities"),
                    venueStatus);
        } else if ("OUTDOOR_LAWN".equals(type)) {
            venue = new GardenLawn(result.getLong("venue_id"), result.getString("venue_name"),
                    result.getInt("capacity"), result.getBigDecimal("rent"), result.getString("facilities"),
                    venueStatus);
        } else {
            venue = new ConventionHall(result.getLong("venue_id"), result.getString("venue_name"),
                    result.getInt("capacity"), result.getBigDecimal("rent"), result.getString("facilities"),
                    venueStatus);
        }
        return new Booking(result.getLong("id"), customer, venue, readEventDate(result),
                result.getString("slot"), result.getString("event_type"), result.getInt("guests"),
                result.getBigDecimal("total_amount"), result.getBigDecimal("amount_paid"),
                BookingStatus.valueOf(result.getString("status")));
    }

    private LocalDate readEventDate(ResultSet result) throws SQLException {
        String storedDate = result.getString("event_date");
        try {
            return LocalDate.parse(storedDate);
        } catch (DateTimeParseException legacyValue) {
            try {
                return Instant.ofEpochMilli(Long.parseLong(storedDate))
                        .atZone(ZoneId.systemDefault()).toLocalDate();
            } catch (NumberFormatException | DateTimeException invalidValue) {
                throw new SQLException("Invalid event_date value in booking record.", legacyValue);
            }
        }
    }
}
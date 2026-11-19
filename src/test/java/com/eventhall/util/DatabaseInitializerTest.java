package com.eventhall.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.Test;

class DatabaseInitializerTest {
    @Test
    void repairsLegacyBookingsWithoutAddingOptionalColumns() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:");
             Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = OFF");
            statement.execute("CREATE TABLE customers (id INTEGER PRIMARY KEY)");
            statement.execute("CREATE TABLE venues (id INTEGER PRIMARY KEY)");
            statement.execute("CREATE TABLE bookings ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "customer_id INTEGER NOT NULL REFERENCES customers(id), "
                    + "venue_id INTEGER NOT NULL REFERENCES venues_old(id), "
                    + "event_date TEXT NOT NULL, slot TEXT NOT NULL, event_type TEXT NOT NULL, "
                    + "guests INTEGER NOT NULL, total_amount NUMERIC NOT NULL, status TEXT NOT NULL, "
                    + "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            statement.execute("INSERT INTO customers (id) VALUES (3)");
            statement.execute("INSERT INTO venues (id) VALUES (8)");
            statement.execute("INSERT INTO bookings "
                    + "(id, customer_id, venue_id, event_date, slot, event_type, guests, total_amount, status) "
                    + "VALUES (11, 3, 8, '2032-12-31', 'MORNING', 'Wedding', 120, 980, 'CONFIRMED')");
            statement.execute("PRAGMA foreign_keys = ON");

            DatabaseInitializer.repairBookingVenueReference(connection);

            assertEquals("venues", foreignKeyTarget(connection, "bookings", "venue_id"));
            assertFalse(hasColumn(connection, "bookings", "hall_amount"));
            assertFalse(hasColumn(connection, "bookings", "service_amount"));
            assertEquals(1, count(connection, "SELECT count(*) FROM bookings WHERE id = 11 AND total_amount = 980"));
        }
    }

    @Test
    void repairsBrokenVenueReferenceAndPreservesRelatedBookingData() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:");
             Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = OFF");
            statement.execute("CREATE TABLE customers (id INTEGER PRIMARY KEY)");
            statement.execute("CREATE TABLE venues (id INTEGER PRIMARY KEY)");
            statement.execute("CREATE TABLE bookings ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "customer_id INTEGER NOT NULL REFERENCES customers(id), "
                    + "venue_id INTEGER NOT NULL REFERENCES venues_old(id), "
                    + "event_date TEXT NOT NULL, slot TEXT NOT NULL, event_type TEXT NOT NULL, "
                    + "guests INTEGER NOT NULL, total_amount NUMERIC NOT NULL, "
                    + "hall_amount NUMERIC NOT NULL DEFAULT 0, service_amount NUMERIC NOT NULL DEFAULT 0, "
                    + "status TEXT NOT NULL, "
                    + "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "CONSTRAINT uq_booking_venue_date_slot UNIQUE (venue_id, event_date, slot))");
            statement.execute("CREATE TABLE payments ("
                    + "id INTEGER PRIMARY KEY, booking_id INTEGER NOT NULL REFERENCES bookings(id), "
                    + "amount NUMERIC NOT NULL)");
            statement.execute("CREATE TABLE booking_services ("
                    + "booking_id INTEGER NOT NULL REFERENCES bookings(id), service_key TEXT NOT NULL, "
                    + "service_name TEXT NOT NULL, price NUMERIC NOT NULL, "
                    + "PRIMARY KEY (booking_id, service_key))");
            statement.execute("INSERT INTO customers (id) VALUES (3)");
            statement.execute("INSERT INTO venues (id) VALUES (8)");
            statement.execute("INSERT INTO bookings "
                    + "(id, customer_id, venue_id, event_date, slot, event_type, guests, total_amount, "
                    + "hall_amount, service_amount, status) "
                    + "VALUES (11, 3, 8, '2032-12-31', 'MORNING', 'Wedding', 120, 50980, 980, 50000, 'CONFIRMED')");
            statement.execute("INSERT INTO payments (id, booking_id, amount) VALUES (5, 11, 500)");
            statement.execute("INSERT INTO booking_services (booking_id, service_key, service_name, price) "
                    + "VALUES (11, 'catering', 'Catering', 25000)");
            statement.execute("PRAGMA foreign_keys = ON");

            DatabaseInitializer.repairBookingVenueReference(connection);

            assertEquals("venues", foreignKeyTarget(connection, "bookings", "venue_id"));
            assertEquals("bookings", foreignKeyTarget(connection, "payments", "booking_id"));
            assertEquals(1, count(connection, "SELECT count(*) FROM bookings WHERE id = 11 "
                    + "AND hall_amount = 980 AND service_amount = 50000"));
            assertEquals(1, count(connection, "SELECT count(*) FROM payments WHERE id = 5 AND booking_id = 11"));
            assertEquals(1, count(connection, "SELECT count(*) FROM booking_services "
                    + "WHERE booking_id = 11 AND service_key = 'catering' AND price = 25000"));
            try (var violations = connection.createStatement().executeQuery("PRAGMA foreign_key_check")) {
                assertFalse(violations.next());
            }
            assertEquals(1, count(connection, "PRAGMA foreign_keys"));
            assertThrows(SQLException.class, () -> statement.executeUpdate("INSERT INTO bookings "
                    + "(customer_id, venue_id, event_date, slot, event_type, guests, total_amount, "
                    + "hall_amount, service_amount, status) "
                    + "VALUES (3, 8, '2032-12-31', 'MORNING', 'Wedding', 120, 980, 980, 0, 'CONFIRMED')"));
        }
    }

    private static String foreignKeyTarget(Connection connection, String table, String column) throws SQLException {
        try (var result = connection.createStatement().executeQuery("PRAGMA foreign_key_list(" + table + ")")) {
            while (result.next()) {
                if (column.equals(result.getString("from"))) {
                    return result.getString("table");
                }
            }
        }
        return null;
    }

    private static int count(Connection connection, String sql) throws SQLException {
        try (var result = connection.createStatement().executeQuery(sql)) {
            result.next();
            return result.getInt(1);
        }
    }

    private static boolean hasColumn(Connection connection, String table, String column) throws SQLException {
        try (var result = connection.createStatement().executeQuery("PRAGMA table_info(" + table + ")")) {
            while (result.next()) {
                if (column.equals(result.getString("name"))) return true;
            }
        }
        return false;
    }
}

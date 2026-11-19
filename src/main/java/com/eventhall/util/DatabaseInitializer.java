package com.eventhall.util;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@WebListener
public class DatabaseInitializer implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent event) {
        try (InputStream input = DatabaseInitializer.class.getClassLoader().getResourceAsStream("schema.sql")) {
            if (input == null) throw new SQLException("schema.sql was not found on the application classpath.");
            String script = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            try (Connection connection = DBConnection.getConnection()) {
                ensureVenueSchema(connection);
                try (Statement statement = connection.createStatement()) {
                    for (String sql : script.split(";")) {
                        String command = sql.lines().map(line -> line.replaceFirst("--.*$", ""))
                                .reduce("", (left, right) -> left + right + "\n").trim();
                        if (!command.isEmpty()) statement.execute(command);
                    }
                }
                ensureCustomerPasswordColumn(connection);
                ensureVenueSchema(connection);
                repairBookingVenueReference(connection);
            }
            event.getServletContext().log("SQLite database initialized.");
        } catch (IOException | SQLException exception) {
            event.getServletContext().log("SQLite database initialization failed.", exception);
            throw new IllegalStateException("Unable to initialize SQLite database.", exception);
        }
    }

    private static void ensureCustomerPasswordColumn(Connection connection) throws SQLException {
        boolean exists = false;
        try (var result = connection.createStatement().executeQuery("PRAGMA table_info(customers)")) {
            while (result.next()) {
                if ("password_hash".equals(result.getString("name"))) {
                    exists = true;
                    break;
                }
            }
        }
        if (!exists) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE customers ADD COLUMN password_hash TEXT");
            }
        }
    }

    private static void ensureVenueSchema(Connection connection) throws SQLException {
        ensureColumn(connection, "venues", "description", "ALTER TABLE venues ADD COLUMN description TEXT DEFAULT ''");
        ensureColumn(connection, "venues", "image_url", "ALTER TABLE venues ADD COLUMN image_url TEXT");

        boolean needsMigration = false;
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT sql FROM sqlite_master WHERE type = 'table' AND name = 'venues'")) {
            if (result.next()) {
                String createSql = result.getString("sql");
                if (createSql != null && createSql.contains("CHECK")
                        && !createSql.contains("OUTDOOR_LAWN")
                        && !createSql.contains("INDOOR_CONVENTION_HALL")) {
                    needsMigration = true;
                }
            }
        }

        if (needsMigration) {
            rebuildVenueTable(connection);
        }
    }

    static void repairBookingVenueReference(Connection connection) throws SQLException {
        boolean correctReference = false;
        try (var result = connection.createStatement().executeQuery("PRAGMA foreign_key_list(bookings)")) {
            while (result.next()) {
                if ("venue_id".equals(result.getString("from"))) {
                    correctReference = "venues".equals(result.getString("table"));
                    break;
                }
            }
        }
        if (correctReference) {
            return;
        }

        boolean originalAutoCommit = connection.getAutoCommit();
        if (!originalAutoCommit) {
            throw new SQLException("Cannot repair the bookings table while a transaction is active.");
        }
        boolean hasHallAmount = hasColumn(connection, "bookings", "hall_amount");
        boolean hasServiceAmount = hasColumn(connection, "bookings", "service_amount");
        String optionalColumns = (hasHallAmount
                ? ", hall_amount NUMERIC NOT NULL DEFAULT 0 CHECK (hall_amount >= 0)" : "")
                + (hasServiceAmount
                ? ", service_amount NUMERIC NOT NULL DEFAULT 0 CHECK (service_amount >= 0)" : "");
        String optionalInsertColumns = (hasHallAmount ? ", hall_amount" : "")
                + (hasServiceAmount ? ", service_amount" : "");
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = OFF");
        }
        try {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE bookings_repaired ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "customer_id INTEGER NOT NULL REFERENCES customers(id), "
                        + "venue_id INTEGER NOT NULL REFERENCES venues(id), "
                        + "event_date TEXT NOT NULL, slot TEXT NOT NULL, event_type TEXT NOT NULL, "
                        + "guests INTEGER NOT NULL CHECK (guests > 0), "
                        + "total_amount NUMERIC NOT NULL CHECK (total_amount >= 0), "
                        + "status TEXT NOT NULL CHECK (status IN "
                        + "('CONFIRMED', 'FULLY_PAID', 'CANCELLED', 'COMPLETED')), "
                        + "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP"
                        + optionalColumns + ", "
                        + "CONSTRAINT uq_booking_venue_date_slot UNIQUE (venue_id, event_date, slot))");
                statement.execute("INSERT INTO bookings_repaired "
                        + "(id, customer_id, venue_id, event_date, slot, event_type, guests, total_amount, "
                        + "status, created_at" + optionalInsertColumns + ") "
                        + "SELECT id, customer_id, venue_id, event_date, slot, event_type, guests, total_amount, "
                        + "status, created_at"
                        + (hasHallAmount ? ", hall_amount" : "")
                        + (hasServiceAmount ? ", service_amount" : "") + " FROM bookings");
                statement.execute("DROP TABLE bookings");
                statement.execute("ALTER TABLE bookings_repaired RENAME TO bookings");
                statement.execute("CREATE INDEX IF NOT EXISTS idx_bookings_event_date ON bookings(event_date)");
                statement.execute("CREATE INDEX IF NOT EXISTS idx_bookings_status ON bookings(status)");
            }
            connection.commit();
        } catch (SQLException exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(originalAutoCommit);
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
            }
        }
    }

    private static boolean hasColumn(Connection connection, String tableName, String columnName)
            throws SQLException {
        try (var result = connection.createStatement().executeQuery("PRAGMA table_info(" + tableName + ")")) {
            while (result.next()) {
                if (columnName.equals(result.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void ensureColumn(Connection connection, String tableName, String columnName, String alterStatement) throws SQLException {
        boolean exists = false;
        try (var result = connection.createStatement().executeQuery("PRAGMA table_info(" + tableName + ")")) {
            while (result.next()) {
                if (columnName.equals(result.getString("name"))) {
                    exists = true;
                    break;
                }
            }
        }
        if (!exists) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(alterStatement);
            }
        }
    }

    private static void rebuildVenueTable(Connection connection) throws SQLException {
        boolean originalAutoCommit = connection.getAutoCommit();
        if (!originalAutoCommit) {
            throw new SQLException("Cannot rebuild the venues table while a transaction is active.");
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = OFF");
        }
        try {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE venues_rebuilt ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, "
                        + "type TEXT NOT NULL CHECK (type IN "
                        + "('MARRIAGE_HALL', 'COMMUNITY_CENTER', 'OUTDOOR_LAWN', 'INDOOR_CONVENTION_HALL')), "
                        + "capacity INTEGER NOT NULL CHECK (capacity > 0), "
                        + "rent NUMERIC NOT NULL CHECK (rent >= 0), facilities TEXT, "
                        + "status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')), "
                        + "description TEXT DEFAULT '', image_url TEXT)");
                statement.execute("INSERT INTO venues_rebuilt "
                        + "(id, name, type, capacity, rent, facilities, status, description, image_url) "
                        + "SELECT id, name, type, capacity, rent, facilities, status, "
                        + "COALESCE(description, ''), COALESCE(image_url, '') FROM venues");
                statement.execute("DROP TABLE venues");
                statement.execute("ALTER TABLE venues_rebuilt RENAME TO venues");
            }
            connection.commit();
        } catch (SQLException exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(originalAutoCommit);
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
            }
        }
    }
}
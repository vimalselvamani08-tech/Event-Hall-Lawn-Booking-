package com.eventhall.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class DBConnection {
    private static final Properties SETTINGS = loadSettings();

    private DBConnection() { }

    private static Properties loadSettings() {
        Properties properties = new Properties();
        try (InputStream input = DBConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
        return properties;
    }

    public static Connection getConnection() throws SQLException {
        String url = System.getProperty("eventhall.db.url", SETTINGS.getProperty("db.url"));
        if (url == null || !url.startsWith("jdbc:sqlite:")) {
            throw new SQLException("A SQLite JDBC URL is required.");
        }
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException exception) {
            throw new SQLException("SQLite JDBC driver is missing from the application.", exception);
        }
        url = url.replace("${user.home}", System.getProperty("user.home"));
        String databasePath = url.substring("jdbc:sqlite:".length());
        if (!databasePath.equals(":memory:") && !databasePath.startsWith("file:")) {
            try {
                Path parent = Path.of(databasePath).toAbsolutePath().getParent();
                if (parent != null) Files.createDirectories(parent);
            } catch (java.io.IOException exception) {
                throw new SQLException("Unable to create the SQLite database directory.", exception);
            }
        }
        Connection connection = DriverManager.getConnection(url);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 5000");
            return connection;
        } catch (SQLException exception) {
            connection.close();
            throw exception;
        }
    }
}
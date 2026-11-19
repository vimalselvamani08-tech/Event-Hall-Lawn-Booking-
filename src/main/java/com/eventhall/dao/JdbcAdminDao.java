package com.eventhall.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

import com.eventhall.model.Admin;

import com.eventhall.model.Admin;
import com.eventhall.util.DBConnection;
import com.eventhall.util.DBConnection;
import com.eventhall.util.DBConnection;

public class JdbcAdminDao implements AdminDao {
    @Override
    public Optional<Admin> findByUsername(String username) throws SQLException {
        String sql = "SELECT id, username, password_hash FROM admins WHERE username = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return Optional.of(new Admin(result.getLong("id"), result.getString("username"),
                            result.getString("password_hash")));
                }
                return Optional.empty();
            }
        }
    }

    @Override
    public void create(String username, String passwordHash) throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO admins (username, password_hash) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, username);
            statement.setString(2, passwordHash);
            statement.executeUpdate();
        }
    }
}
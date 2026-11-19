package com.eventhall.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.eventhall.model.Customer;
import com.eventhall.util.DBConnection;

public class JdbcCustomerDao implements CustomerDao {
    @Override
    public Customer create(Customer customer) throws SQLException {
        String sql = "INSERT INTO customers (name, phone, email, address, password_hash) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, customer);
            statement.setString(5, customer.getPasswordHash());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) customer.setId(keys.getLong(1));
            }
            return customer;
        }
    }

    @Override
    public boolean update(Customer customer) throws SQLException {
        String sql = "UPDATE customers SET name = ?, phone = ?, email = ?, address = ? WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, customer);
            statement.setLong(5, customer.getId());
            return statement.executeUpdate() == 1;
        }
    }

    @Override
    public boolean delete(long id) throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM customers WHERE id = ?")) {
            statement.setLong(1, id);
            return statement.executeUpdate() == 1;
        }
    }

    @Override
    public Optional<Customer> findById(long id) throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM customers WHERE id = ?")) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        }
    }

    @Override
    public Optional<Customer> findByEmail(String email) throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM customers WHERE lower(email) = lower(?)")) {
            statement.setString(1, email);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        }
    }

    @Override
    public boolean setPasswordHash(long id, String passwordHash) throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE customers SET password_hash = ? WHERE id = ? AND password_hash IS NULL")) {
            statement.setString(1, passwordHash);
            statement.setLong(2, id);
            return statement.executeUpdate() == 1;
        }
    }

    @Override
    public List<Customer> search(String query) throws SQLException {
        String sql = "SELECT * FROM customers WHERE name LIKE ? OR phone LIKE ? ORDER BY name";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            String pattern = "%" + query + "%";
            statement.setString(1, pattern);
            statement.setString(2, pattern);
            try (ResultSet results = statement.executeQuery()) { return mapAll(results); }
        }
    }

    @Override
    public List<Customer> findAll() throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM customers ORDER BY name");
             ResultSet results = statement.executeQuery()) {
            return mapAll(results);
        }
    }

    private static void bind(PreparedStatement statement, Customer customer) throws SQLException {
        statement.setString(1, customer.getName());
        statement.setString(2, customer.getPhone());
        statement.setString(3, customer.getEmail());
        statement.setString(4, customer.getAddress());
    }

    private static Customer map(ResultSet result) throws SQLException {
        Customer customer = new Customer(result.getLong("id"), result.getString("name"), result.getString("phone"),
            result.getString("email"), result.getString("address"));
        customer.setPasswordHash(result.getString("password_hash"));
        return customer;
    }

    private static List<Customer> mapAll(ResultSet results) throws SQLException {
        List<Customer> customers = new ArrayList<>();
        while (results.next()) customers.add(map(results));
        return customers;
    }
}
package com.eventhall.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import com.eventhall.model.Customer;

public interface CustomerDao {
    Customer create(Customer customer) throws SQLException;
    boolean update(Customer customer) throws SQLException;
    boolean delete(long id) throws SQLException;
    Optional<Customer> findById(long id) throws SQLException;
    Optional<Customer> findByEmail(String email) throws SQLException;
    boolean setPasswordHash(long id, String passwordHash) throws SQLException;
    List<Customer> search(String query) throws SQLException;
    List<Customer> findAll() throws SQLException;
}
package com.eventhall.dao;

import java.sql.SQLException;
import java.util.Optional;
import com.eventhall.model.Admin;

public interface AdminDao {
    Optional<Admin> findByUsername(String username) throws SQLException;
    void create(String username, String passwordHash) throws SQLException;
}
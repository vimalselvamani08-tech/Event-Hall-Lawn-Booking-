package com.eventhall.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import com.eventhall.model.Venue;

public interface VenueDao {
    Venue create(Venue venue) throws SQLException;
    boolean update(Venue venue) throws SQLException;
    boolean delete(long id) throws SQLException;
    Optional<Venue> findById(long id) throws SQLException;
    List<Venue> findAll() throws SQLException;
}
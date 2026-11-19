package com.eventhall.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.eventhall.model.CommunityCenter;
import com.eventhall.model.ConventionHall;
import com.eventhall.model.GardenLawn;
import com.eventhall.model.MarriageHall;
import com.eventhall.model.Venue;
import com.eventhall.model.VenueStatus;
import com.eventhall.util.DBConnection;

public class JdbcVenueDao implements VenueDao {
    @Override
    public Venue create(Venue venue) throws SQLException {
        String sql = "INSERT INTO venues (name, type, capacity, rent, facilities, description, image_url, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, venue);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) { if (keys.next()) venue.setId(keys.getLong(1)); }
            return venue;
        }
    }

    @Override
    public boolean update(Venue venue) throws SQLException {
        String sql = "UPDATE venues SET name = ?, type = ?, capacity = ?, rent = ?, facilities = ?, description = ?, image_url = ?, status = ? WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, venue);
            statement.setLong(9, venue.getId());
            return statement.executeUpdate() == 1;
        }
    }

    @Override
    public boolean delete(long id) throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM venues WHERE id = ?")) {
            statement.setLong(1, id);
            return statement.executeUpdate() == 1;
        }
    }

    @Override
    public Optional<Venue> findById(long id) throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM venues WHERE id = ?")) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        }
    }

    @Override
    public List<Venue> findAll() throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM venues ORDER BY name");
             ResultSet results = statement.executeQuery()) {
            List<Venue> venues = new ArrayList<>();
            while (results.next()) venues.add(map(results));
            return venues;
        }
    }

    private static void bind(PreparedStatement statement, Venue venue) throws SQLException {
        statement.setString(1, venue.getName());
        statement.setString(2, venue.getType());
        statement.setInt(3, venue.getCapacity());
        statement.setBigDecimal(4, venue.getBaseRent());
        statement.setString(5, venue.getFacilities());
        statement.setString(6, venue.getDescription());
        statement.setString(7, venue.getImageUrl());
        statement.setString(8, venue.getStatus().name());
    }

    private static Venue map(ResultSet result) throws SQLException {
        String type = result.getString("type");
        long id = result.getLong("id");
        String name = result.getString("name");
        int capacity = result.getInt("capacity");
        var rent = result.getBigDecimal("rent");
        String facilities = result.getString("facilities");
        String description = result.getString("description");
        String imageUrl = result.getString("image_url");
        String status = result.getString("status");
        VenueStatus venueStatus = VenueStatus.valueOf(status);
        if ("MARRIAGE_HALL".equals(type)) {
            return new MarriageHall(id, name, capacity, rent, facilities, description, imageUrl, venueStatus);
        }
        if ("COMMUNITY_CENTER".equals(type)) {
            return new CommunityCenter(id, name, capacity, rent, facilities, description, imageUrl, venueStatus);
        }
        if ("OUTDOOR_LAWN".equals(type)) {
            return new GardenLawn(id, name, capacity, rent, facilities, description, imageUrl, venueStatus);
        }
        return new ConventionHall(id, name, capacity, rent, facilities, description, imageUrl, venueStatus);
    }
}
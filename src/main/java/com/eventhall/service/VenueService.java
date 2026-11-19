package com.eventhall.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import com.eventhall.dao.VenueDao;
import com.eventhall.model.Venue;
import com.eventhall.model.Venue;

public class VenueService {
    private final VenueDao venueDao;
    public VenueService(VenueDao venueDao) { this.venueDao = venueDao; }
    public Venue create(Venue venue) throws SQLException { return venueDao.create(venue); }
    public boolean update(Venue venue) throws SQLException { return venueDao.update(venue); }
    public boolean delete(long id) throws SQLException { return venueDao.delete(id); }
    public Optional<Venue> findById(long id) throws SQLException { return venueDao.findById(id); }
    public List<Venue> findAll() throws SQLException { return venueDao.findAll(); }
}
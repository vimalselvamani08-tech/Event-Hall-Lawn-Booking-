package com.eventhall.service;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.eventhall.dao.CustomerDao;
import com.eventhall.dao.CustomerDao;
import com.eventhall.model.Customer;

public class CustomerService {
    private final CustomerDao customerDao;

    public CustomerService(CustomerDao customerDao) { this.customerDao = customerDao; }
    public Customer create(Customer customer) throws SQLException { return customerDao.create(customer); }
    public boolean update(Customer customer) throws SQLException { return customerDao.update(customer); }
    public boolean delete(long id) throws SQLException { return customerDao.delete(id); }
    public Optional<Customer> findById(long id) throws SQLException { return customerDao.findById(id); }
    public List<Customer> search(String query) throws SQLException { return customerDao.search(query == null ? "" : query.trim()); }
    public List<Customer> findAll() throws SQLException { return customerDao.findAll(); }

    public Map<Long, Customer> findLookup(List<Long> ids) throws SQLException {
        Map<Long, Customer> lookup = new HashMap<>();
        for (Long id : ids) customerDao.findById(id).ifPresent(customer -> lookup.put(id, customer));
        return lookup;
    }
}
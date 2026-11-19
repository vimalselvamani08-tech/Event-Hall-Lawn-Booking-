package com.eventhall.service;

import com.eventhall.dao.CustomerDao;
import com.eventhall.exception.InvalidLoginException;
import com.eventhall.model.Customer;
import java.sql.SQLException;
import java.util.regex.Pattern;
import org.mindrot.jbcrypt.BCrypt;

public class UserAuthenticationService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private final CustomerDao customerDao;

    public UserAuthenticationService(CustomerDao customerDao) { this.customerDao = customerDao; }

    public Customer authenticate(String email, String password) throws SQLException, InvalidLoginException {
        String normalizedEmail = normalizeEmail(email);
        Customer customer = customerDao.findByEmail(normalizedEmail).orElse(null);
        if (customer == null || customer.getPasswordHash() == null
                || !BCrypt.checkpw(password == null ? "" : password, customer.getPasswordHash())) {
            throw new InvalidLoginException("Invalid email or password.");
        }
        return customer;
    }

    public Customer register(String name, String phone, String email, String address, String password)
            throws SQLException {
        String normalizedEmail = normalizeEmail(email);
        if (name == null || name.isBlank() || name.trim().length() > 160) {
            throw new IllegalArgumentException("Enter your name (up to 160 characters).");
        }
        if (phone == null || phone.isBlank() || phone.trim().length() > 30) {
            throw new IllegalArgumentException("Enter a phone number (up to 30 characters).");
        }
        if (password == null || password.length() < 10 || password.length() > 72) {
            throw new IllegalArgumentException("Password must be between 10 and 72 characters.");
        }

        String hash = BCrypt.hashpw(password, BCrypt.gensalt(12));
        Customer existing = customerDao.findByEmail(normalizedEmail).orElse(null);
        if (existing != null) {
            boolean contactMatches = existing.getName().equalsIgnoreCase(name.trim())
                    && existing.getPhone().equals(phone.trim());
            if (!contactMatches) {
                throw new IllegalArgumentException("This email is already on a customer record. Verify the name and phone or contact the venue administrator.");
            }
            if (existing.getPasswordHash() != null || !customerDao.setPasswordHash(existing.getId(), hash)) {
                throw new IllegalArgumentException("An account already exists for this email.");
            }
            return existing;
        }
        Customer customer = new Customer(name.trim(), phone.trim(), normalizedEmail,
                address == null || address.isBlank() ? null : address.trim());
        customer.setPasswordHash(hash);
        return customerDao.create(customer);
    }

    private static String normalizeEmail(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalized.length() > 160 || !EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }
        return normalized;
    }
}
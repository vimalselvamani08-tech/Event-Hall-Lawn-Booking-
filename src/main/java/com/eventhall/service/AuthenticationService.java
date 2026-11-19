package com.eventhall.service;

import java.sql.SQLException;
import java.util.regex.Pattern;
import org.mindrot.jbcrypt.BCrypt;
import com.eventhall.dao.AdminDao;
import com.eventhall.exception.InvalidLoginException;
import com.eventhall.model.Admin;

public class AuthenticationService {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("[A-Za-z0-9._-]{3,40}");
    private final AdminDao adminDao;

    public AuthenticationService(AdminDao adminDao) { this.adminDao = adminDao; }

    public Admin authenticate(String username, String password) throws SQLException, InvalidLoginException {
        if (username == null || password == null) throw new InvalidLoginException("Invalid username or password.");
        Admin admin = adminDao.findByUsername(username.trim()).orElse(null);
        if (admin == null || !BCrypt.checkpw(password, admin.getPasswordHash())) {
            throw new InvalidLoginException("Invalid username or password.");
        }
        return admin;
    }

    public void registerAdmin(String username, String password)
            throws SQLException, IllegalArgumentException {
        String normalized = username == null ? "" : username.trim();
        if (!USERNAME_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Username must be 3-40 characters using letters, numbers, dot, dash, or underscore.");
        }
        validatePassword(password);
        adminDao.create(normalized, BCrypt.hashpw(password, BCrypt.gensalt(12)));
    }

    private static void validatePassword(String password) {
        if (password == null || password.length() < 10 || password.length() > 72) {
            throw new IllegalArgumentException("Password must be between 10 and 72 characters.");
        }
    }
}
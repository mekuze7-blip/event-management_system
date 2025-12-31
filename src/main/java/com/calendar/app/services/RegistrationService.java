package com.calendar.app.services;

import com.calendar.app.utils.DatabaseConnection;
import java.sql.*;

public class RegistrationService {
    
    public RegistrationResult registerUser(String username, String password, String email, String fullName) {
        System.out.println("Registration attempt for: " + username);
        
        // Check database connection
        if (DatabaseConnection.getConnection() == null) {
            return new RegistrationResult(false, "Database connection failed. Please ensure MySQL is running and database is set up.");
        }
        
        // Validate inputs
        if (username == null || username.trim().isEmpty()) {
            return new RegistrationResult(false, "Username is required");
        }
        if (password == null || password.length() < 6) {
            return new RegistrationResult(false, "Password must be at least 6 characters");
        }
        if (email == null || !email.contains("@")) {
            return new RegistrationResult(false, "Valid email is required");
        }
        if (fullName == null || fullName.trim().isEmpty()) {
            return new RegistrationResult(false, "Full name is required");
        }
        
        // Check if username already exists
        if (userExists(username)) {
            return new RegistrationResult(false, "Username already exists");
        }
        
        // Check if email already exists
        if (emailExists(email)) {
            return new RegistrationResult(false, "Email already registered");
        }
        
        // Create user in database
        try {
            if (createUserInDatabase(username, password, email, fullName)) {
                System.out.println("? Registration successful for: " + username);
                return new RegistrationResult(true, "Registration successful! You can now login.");
            } else {
                return new RegistrationResult(false, "Registration failed. Please try again.");
            }
        } catch (SQLException e) {
            System.err.println("Database error during registration: " + e.getMessage());
            return new RegistrationResult(false, "Database error: " + e.getMessage());
        }
    }
    
    private boolean userExists(String username) {
        String sql = "SELECT COUNT(*) as count FROM users WHERE username = ?";
        
        try (PreparedStatement stmt = DatabaseConnection.getReusableConnection().prepareStatement(sql)) {
            
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("count") > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking username: " + e.getMessage());
        }
        return false;
    }
    
    private boolean emailExists(String email) {
        String sql = "SELECT COUNT(*) as count FROM users WHERE email = ?";
        
        try (PreparedStatement stmt = DatabaseConnection.getReusableConnection().prepareStatement(sql)) {
            
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("count") > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking email: " + e.getMessage());
        }
        return false;
    }
    
    private boolean createUserInDatabase(String username, String password, String email, String fullName) throws SQLException {
        String sql = "INSERT INTO users (username, password, email, full_name, role) VALUES (?, ?, ?, ?, 'USER')";
        
        try (PreparedStatement stmt = DatabaseConnection.getReusableConnection().prepareStatement(sql)) {
            
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setString(3, email);
            stmt.setString(4, fullName);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }
    
    // Result class for registration
    public static class RegistrationResult {
        private boolean success;
        private String message;
        
        public RegistrationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
}
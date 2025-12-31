package com.calendar.app.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static Connection connection = null;
    private static final String URL = "jdbc:mysql://localhost:3306/event_management?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "122119me";
    
    public static Connection getConnection() {
        if (connection == null) {
            try {
                System.out.println("🔌 Attempting database connection...");
                System.out.println("URL: " + URL.replace(PASSWORD, "***"));
                System.out.println("User: " + USER);
                
                // Load MySQL driver
                Class.forName("com.mysql.cj.jdbc.Driver");
                System.out.println("✅ MySQL Driver loaded");
                
                // Create connection with timeout
                DriverManager.setLoginTimeout(10); // 10 seconds timeout
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("🎉 Database connected successfully!");
                System.out.println("Connection valid: " + connection.isValid(2));
                
            } catch (ClassNotFoundException e) {
                System.err.println("❌ MySQL Driver not found: " + e.getMessage());
                System.err.println("Make sure MySQL Connector/J is in classpath");
                e.printStackTrace();
                return null;
            } catch (SQLException e) {
                System.err.println("❌ Database connection failed:");
                System.err.println("  Error: " + e.getMessage());
                System.err.println("  Error code: " + e.getErrorCode());
                System.err.println("  SQL state: " + e.getSQLState());
                
                // Provide helpful troubleshooting info
                if (e.getErrorCode() == 1045) {
                    System.err.println("  → Check username/password");
                } else if (e.getErrorCode() == 1049) {
                    System.err.println("  → Database 'event_management' does not exist");
                } else if (e.getErrorCode() == 2003) {
                    System.err.println("  → Cannot connect to MySQL server. Is MySQL running?");
                }
                
                e.printStackTrace();
                return null;
            } catch (Exception e) {
                System.err.println("❌ Unexpected error: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        }
        return connection;
    }
    
    /**
     * Returns a connection that should NOT be closed by the caller.
     * This method ensures the static connection stays open for reuse.
     */
    public static Connection getReusableConnection() {
        Connection conn = getConnection();
        if (conn != null) {
            try {
                // Ensure connection is still valid
                if (conn.isClosed() || !conn.isValid(2)) {
                    System.out.println("🔄 Connection invalid, reconnecting...");
                    connection = null; // Force reconnection
                    conn = getConnection();
                }
            } catch (SQLException e) {
                System.err.println("❌ Connection validation failed: " + e.getMessage());
                connection = null; // Force reconnection on next call
                return null;
            }
        }
        return conn;
    }
    
    public static void initialize() {
        System.out.println("Initializing database connection...");
        Connection conn = getConnection();
        if (conn != null) {
            System.out.println("✅ Database initialization complete");
        } else {
            System.out.println("❌ Database initialization failed");
        }
    }
    
    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("Database connection closed.");
                connection = null;
            } catch (SQLException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }
    
    public static boolean testConnection() {
        try {
            Connection conn = getConnection();
            if (conn != null && !conn.isClosed()) {
                System.out.println("✅ Database connection test: PASSED");
                return true;
            } else {
                System.out.println("❌ Database connection test: FAILED");
                return false;
            }
        } catch (SQLException e) {
            System.err.println("❌ Database test failed: " + e.getMessage());
            return false;
        }
    }
}
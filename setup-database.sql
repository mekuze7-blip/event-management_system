-- Event Management System - Users Table SQL

-- Create database if it doesn't exist
CREATE DATABASE IF NOT EXISTS event_management;
USE event_management;

-- Create users table if it doesn't exist
CREATE TABLE IF NOT EXISTS users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) DEFAULT 'USER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login TIMESTAMP NULL,
    login_attempts INT DEFAULT 0,
    account_locked BOOLEAN DEFAULT FALSE
);

-- Insert default admin user if not exists
INSERT IGNORE INTO users (username, password, email, full_name, role) 
VALUES ('admin', 'admin123', 'admin@eventsystem.com', 'Administrator', 'ADMIN');

-- Insert test users if not exist
INSERT IGNORE INTO users (username, password, email, full_name, role) VALUES
('john.doe', 'password123', 'john@example.com', 'John Doe', 'USER'),
('jane.smith', 'password456', 'jane@example.com', 'Jane Smith', 'USER'),
('manager', 'manager123', 'manager@example.com', 'Manager User', 'ADMIN');

-- Display all users
SELECT * FROM events ORDER BY created_at DESC;
SELECT * FROM users ORDER BY created_at DESC;
DELETE FROM users 
WHERE username != 'meku';
DELETE FROM events 
WHERE user_id IN (SELECT id FROM users WHERE username != 'meku');

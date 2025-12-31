-- Event Management System Database Schema

CREATE DATABASE IF NOT EXISTS event_management;
USE event_management;

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    full_name VARCHAR(100),
    role ENUM('ADMIN', 'USER') DEFAULT 'USER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Events table
CREATE TABLE IF NOT EXISTS events (
    id INT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    event_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME,
    location VARCHAR(200),
    contact_phone VARCHAR(20),
    category ENUM('MEETING', 'BIRTHDAY', 'HOLIDAY', 'REMINDER', 'OTHER') DEFAULT 'OTHER',
    priority ENUM('LOW', 'MEDIUM', 'HIGH') DEFAULT 'MEDIUM',
    user_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Event participants table
CREATE TABLE IF NOT EXISTS event_participants (
    id INT PRIMARY KEY AUTO_INCREMENT,
    event_id INT NOT NULL,
    user_id INT NOT NULL,
    status ENUM('PENDING', 'ACCEPTED', 'DECLINED') DEFAULT 'PENDING',
    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY unique_participation (event_id, user_id)
);

-- Insert default user mek (password: admin123)
INSERT INTO users (username, password, email, full_name, role) 
VALUES ('mek', '$2a$10$YourHashedPasswordHere', 'mek@calendar.com', 'Mek', 'ADMIN')
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

-- Remove all users except mek
DELETE FROM users WHERE username != 'mek';

-- Insert test events
INSERT INTO events (title, description, event_date, start_time, end_time, location, category, priority, user_id) VALUES
('Team Meeting', 'Weekly team sync up', CURDATE() + INTERVAL 1 DAY, '10:00:00', '11:00:00', 'Conference Room A', 'MEETING', 'HIGH', (SELECT id FROM users WHERE username = 'mek')),
('Birthday Party', 'John''s birthday celebration', CURDATE() + INTERVAL 3 DAY, '18:00:00', '22:00:00', 'Main Hall', 'BIRTHDAY', 'MEDIUM', (SELECT id FROM users WHERE username = 'mek')),
('Project Deadline', 'Final submission for Project X', CURDATE() + INTERVAL 7 DAY, '23:59:59', '23:59:59', 'Office', 'REMINDER', 'HIGH', (SELECT id FROM users WHERE username = 'mek'))
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

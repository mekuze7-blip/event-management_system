package com.calendar.app.services;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import com.calendar.app.models.Event;
import com.calendar.app.utils.DatabaseConnection;

public class DatabaseService {
    
    // Check if user exists in database
    public boolean validateUser(String username, String password) {
        String sql = "SELECT COUNT(*) as count FROM users WHERE username = ? AND password = ?";
        
        try (PreparedStatement stmt = DatabaseConnection.getReusableConnection().prepareStatement(sql)) {
            
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("count") > 0;
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
        return false;
    }
    
    // Get all events for a user
    public List<Event> getUserEvents(int userId) {
        List<Event> events = new ArrayList<>();
        String sql = "SELECT * FROM events WHERE user_id = ? ORDER BY event_date DESC";
        
        try {
            Connection conn = DatabaseConnection.getReusableConnection();
            if (conn == null) {
                System.err.println("Database connection is null, returning empty events list");
                return events;
            }
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setInt(1, userId);
                ResultSet rs = stmt.executeQuery();
                
                while (rs.next()) {
                    Event event = new Event();
                    event.setId(rs.getInt("id"));
                    event.setTitle(rs.getString("title"));
                    event.setDescription(rs.getString("description"));
                    event.setLocation(rs.getString("location"));
                    event.setEventDate(rs.getDate("event_date").toLocalDate());
                    event.setStartTime(rs.getTime("start_time").toLocalTime());
                    event.setEndTime(rs.getTime("end_time").toLocalTime());
                    event.setCategory(rs.getString("category"));
                    event.setUserId(rs.getInt("user_id"));
                    events.add(event);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching events: " + e.getMessage());
        }
        return events;
    }
    
    // Add new event
    public boolean addEvent(Event event) {
        String sql = "INSERT INTO events (title, description, location, event_date, start_time, end_time, category, user_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        try {
            Connection conn = DatabaseConnection.getReusableConnection();
            if (conn == null) {
                System.err.println("Database connection is null, cannot add event");
                return false;
            }
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, event.getTitle());
                stmt.setString(2, event.getDescription());
                stmt.setString(3, event.getLocation());
                stmt.setDate(4, Date.valueOf(event.getEventDate()));
                stmt.setTime(5, Time.valueOf(event.getStartTime()));
                stmt.setTime(6, Time.valueOf(event.getEndTime()));
                stmt.setString(7, event.getCategory());
                stmt.setInt(8, event.getUserId());
                
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error adding event: " + e.getMessage());
            return false;
        }
    }
    
    // Update event in database
    public boolean updateEvent(Event event) {
        String sql = "UPDATE events SET title=?, description=?, location=?, event_date=?, start_time=?, end_time=?, category=? WHERE id=? AND user_id=?";
        
        try {
            Connection conn = DatabaseConnection.getReusableConnection();
            if (conn == null) {
                System.err.println("Database connection is null, cannot update event");
                return false;
            }
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, event.getTitle());
                stmt.setString(2, event.getDescription());
                stmt.setString(3, event.getLocation());
                stmt.setDate(4, Date.valueOf(event.getEventDate()));
                stmt.setTime(5, Time.valueOf(event.getStartTime()));
                stmt.setTime(6, Time.valueOf(event.getEndTime()));
                stmt.setString(7, event.getCategory());
                stmt.setInt(8, event.getId());
                stmt.setInt(9, event.getUserId());
                
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error updating event: " + e.getMessage());
            return false;
        }
    }
    
    // Delete event from database
    public boolean deleteEvent(int eventId, int userId) {
        String sql = "DELETE FROM events WHERE id=? AND user_id=?";
        
        try {
            Connection conn = DatabaseConnection.getReusableConnection();
            if (conn == null) {
                System.err.println("Database connection is null, cannot delete event");
                return false;
            }
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setInt(1, eventId);
                stmt.setInt(2, userId);
                
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error deleting event: " + e.getMessage());
            return false;
        }
    }
    
    // Get event statistics
    public String getEventStats(int userId) {
        String sql = "SELECT " +
                     "COUNT(*) as total, " +
                     "COUNT(CASE WHEN category = 'Meeting' THEN 1 END) as meetings, " +
                     "COUNT(CASE WHEN category = 'Personal' THEN 1 END) as personal, " +
                     "COUNT(CASE WHEN category = 'Work' THEN 1 END) as work " +
                     "FROM events WHERE user_id = ?";
        
        try {
            Connection conn = DatabaseConnection.getReusableConnection();
            if (conn == null) {
                System.err.println("Database connection is null, cannot get stats");
                return "Total Events: 0 | Meetings: 0 | Personal: 0 | Work: 0";
            }
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setInt(1, userId);
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    return String.format("Total Events: %d | Meetings: %d | Personal: %d | Work: %d",
                        rs.getInt("total"),
                        rs.getInt("meetings"),
                        rs.getInt("personal"),
                        rs.getInt("work"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting stats: " + e.getMessage());
        }
        return "No statistics available";
    }

    public List<Event> getAllEvents() {
        List<Event> events = new ArrayList<>();
        String sql = "SELECT e.*, u.username FROM events e JOIN users u ON e.user_id = u.id ORDER BY e.event_date, e.start_time";

        try {
            Connection conn = DatabaseConnection.getReusableConnection();
            if (conn == null) {
                System.err.println("Database connection is null, cannot get all events");
                return events;
            }
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    Event event = new Event();
                    event.setId(rs.getInt("id"));
                    event.setTitle(rs.getString("title"));
                    event.setDescription(rs.getString("description"));
                    event.setLocation(rs.getString("location"));
                    event.setEventDate(rs.getDate("event_date").toLocalDate());
                    event.setStartTime(rs.getTime("start_time").toLocalTime());
                    event.setEndTime(rs.getTime("end_time").toLocalTime());
                    event.setCategory(rs.getString("category"));
                    event.setUserId(rs.getInt("user_id"));
                    event.setUserName(rs.getString("username")); // Assuming Event model has setUserName
                    events.add(event);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching all events: " + e.getMessage());
        }
        return events;
    }
}
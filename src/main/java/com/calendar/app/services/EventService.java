package com.calendar.app.services;

import com.calendar.app.models.Event;
import com.calendar.app.utils.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EventService {
    
    public List<Event> getUserEvents(int userId) {
        List<Event> events = new ArrayList<>();
        String sql = "SELECT id, title, description, location FROM events WHERE user_id = ?";
        
        try (PreparedStatement stmt = DatabaseConnection.getReusableConnection().prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Event event = new Event();
                event.setId(rs.getInt("id"));
                event.setTitle(rs.getString("title"));
                event.setDescription(rs.getString("description"));
                event.setLocation(rs.getString("location"));
                events.add(event);
            }
            
        } catch (SQLException e) {
            System.err.println("Error loading events: " + e.getMessage());
        }
        return events;
    }
    
    public boolean createEvent(Event event) {
        String sql = "INSERT INTO events (title, description, location, user_id) VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement stmt = DatabaseConnection.getReusableConnection().prepareStatement(sql)) {
            
            stmt.setString(1, event.getTitle());
            stmt.setString(2, event.getDescription());
            stmt.setString(3, event.getLocation());
            stmt.setInt(4, event.getUserId());
            
            int rows = stmt.executeUpdate();
            return rows > 0;
            
        } catch (SQLException e) {
            System.err.println("Error creating event: " + e.getMessage());
            return false;
        }
    }
    
    public boolean updateEvent(Event event) {
        String sql = "UPDATE events SET title=?, description=?, location=? WHERE id=? AND user_id=?";
        
        try (PreparedStatement stmt = DatabaseConnection.getReusableConnection().prepareStatement(sql)) {
            
            stmt.setString(1, event.getTitle());
            stmt.setString(2, event.getDescription());
            stmt.setString(3, event.getLocation());
            stmt.setInt(4, event.getId());
            stmt.setInt(5, event.getUserId());
            
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating event: " + e.getMessage());
            return false;
        }
    }
    
    public boolean deleteEvent(int eventId, int userId) {
        String sql = "DELETE FROM events WHERE id=? AND user_id=?";
        
        try (PreparedStatement stmt = DatabaseConnection.getReusableConnection().prepareStatement(sql)) {
            
            stmt.setInt(1, eventId);
            stmt.setInt(2, userId);
            
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error deleting event: " + e.getMessage());
            return false;
        }
    }
}
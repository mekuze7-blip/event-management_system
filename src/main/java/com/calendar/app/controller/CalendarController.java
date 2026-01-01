package com.calendar.app.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.time.YearMonth;
import com.calendar.app.models.Event;
import com.calendar.app.utils.DatabaseConnection;
import com.calendar.app.utils.SessionManager;
import java.sql.*;

public class CalendarController {
    
    @FXML private Label monthYearLabel;
    @FXML private GridPane calendarGrid;
    @FXML private VBox eventList;
    @FXML private ListView<Event> eventsListView;
    
    private YearMonth currentYearMonth;
    
    @FXML
    public void initialize() {
        System.out.println("CalendarController initialized");
        currentYearMonth = YearMonth.now();
        updateCalendar();
        loadEventsForMonth();
    }
    
    private void updateCalendar() {
        calendarGrid.getChildren().clear();
        monthYearLabel.setText(currentYearMonth.getMonth().toString() + " " + currentYearMonth.getYear());
        
        // Day headers
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        for (int i = 0; i < 7; i++) {
            Label dayLabel = new Label(days[i]);
            dayLabel.setStyle("-fx-font-weight: bold; -fx-padding: 10; -fx-alignment: center;");
            calendarGrid.add(dayLabel, i, 0);
        }
        
        LocalDate firstOfMonth = currentYearMonth.atDay(1);
        int dayOfWeek = firstOfMonth.getDayOfWeek().getValue();
        
        // Fill calendar
        int day = 1;
        for (int row = 1; row < 7; row++) {
            for (int col = 0; col < 7; col++) {
                if ((row == 1 && col < dayOfWeek - 1) || day > currentYearMonth.lengthOfMonth()) {
                    VBox emptyCell = new VBox();
                    emptyCell.setStyle("-fx-min-height: 60; -fx-min-width: 100;");
                    calendarGrid.add(emptyCell, col, row);
                    continue;
                }
                
                VBox dayCell = new VBox();
                dayCell.setStyle("-fx-border-color: #ddd; -fx-padding: 5; -fx-min-height: 60; -fx-min-width: 100;");
                
                Label dayLabel = new Label(String.valueOf(day));
                dayLabel.setStyle("-fx-font-weight: bold;");
                dayCell.getChildren().add(dayLabel);
                
                // Highlight today
                LocalDate today = LocalDate.now();
                if (currentYearMonth.getYear() == today.getYear() && 
                    currentYearMonth.getMonthValue() == today.getMonthValue() && 
                    day == today.getDayOfMonth()) {
                    dayCell.setStyle("-fx-border-color: #3498db; -fx-background-color: #e3f2fd; -fx-padding: 5; -fx-min-height: 60; -fx-min-width: 100;");
                }
                
                calendarGrid.add(dayCell, col, row);
                day++;
            }
        }
    }
    
    private void loadEventsForMonth() {
        eventsListView.getItems().clear();
        
        String sql = "SELECT id, title, description, location, start_time FROM events WHERE MONTH(start_time) = ? AND YEAR(start_time) = ? AND user_id = ? ORDER BY start_time";
        
        try (PreparedStatement stmt = DatabaseConnection.getReusableConnection().prepareStatement(sql)) {
            
            stmt.setInt(1, currentYearMonth.getMonthValue());
            stmt.setInt(2, currentYearMonth.getYear());
            stmt.setInt(3, SessionManager.getCurrentUser().getId());
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Event event = new Event();
                event.setId(rs.getInt("id"));
                event.setTitle(rs.getString("title"));
                event.setDescription(rs.getString("description"));
                event.setLocation(rs.getString("location"));
                eventsListView.getItems().add(event);
            }
            
        } catch (SQLException e) {
            System.err.println("Error loading events: " + e.getMessage());
        }
    }
    
    @FXML
    private void handlePreviousMonth() {
        currentYearMonth = currentYearMonth.minusMonths(1);
        updateCalendar();
        loadEventsForMonth();
    }
    
    @FXML
    private void handleNextMonth() {
        currentYearMonth = currentYearMonth.plusMonths(1);
        updateCalendar();
        loadEventsForMonth();
    }
    
    @FXML
    private void handleToday() {
        currentYearMonth = YearMonth.now();
        updateCalendar();
        loadEventsForMonth();
    }
    
    @FXML
    private void handleAddEvent() {
        System.out.println("Add Event button clicked");
        // Open event creation dialog
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("New Event");
        alert.setHeaderText("Event Creation");
        alert.setContentText("This feature will open event creation form");
        alert.showAndWait();
    }

    @FXML
    private void handleBackToDashboard() {
        System.out.println("Back to Dashboard clicked from CalendarController");
        // Close this calendar window so the underlying dashboard becomes visible again
        Stage stage = (Stage) monthYearLabel.getScene().getWindow();
        stage.close();
    }
}

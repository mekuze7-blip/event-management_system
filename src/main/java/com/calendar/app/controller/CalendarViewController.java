package com.calendar.app.controllers;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.List;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import java.util.ArrayList;
import com.calendar.app.models.Event;
import com.calendar.app.services.DatabaseService;
import com.calendar.app.utils.SessionManager;

public class CalendarViewController {
    
    private YearMonth currentYearMonth;
    private GridPane calendarGrid;
    private Label monthLabel;
    private Consumer<LocalDate> onDateClicked;
    private Consumer<Event> onEventClicked;
    private DatabaseService databaseService = new DatabaseService();
    
    public void setOnDateClicked(Consumer<LocalDate> onDateClicked) {
        this.onDateClicked = onDateClicked;
    }

    public void setOnEventClicked(Consumer<Event> onEventClicked) {
        this.onEventClicked = onEventClicked;
    }
    
    public void showCalendar(Stage parentStage) {
        Stage calendarStage = new Stage();
        calendarStage.setTitle("Calendar View - Event Management System");
        
        // Get the calendar view from our new method
        VBox calendarView = getCalendarView(true); // Pass true to include close button
        
        // Find the close button and set its action for this specific stage
        Button closeBtn = (Button) calendarView.lookup("#closeBtn");
        if (closeBtn != null) {
            closeBtn.setOnAction(e -> calendarStage.close());
        }
        
        Scene scene = new Scene(calendarView, 800, 600);
        calendarStage.setScene(scene);
        calendarStage.initOwner(parentStage);
        calendarStage.show();
    }
    
    private VBox getCalendarView(boolean includeCloseButton) {
        currentYearMonth = YearMonth.now();
        VBox mainLayout = new VBox(20);
        mainLayout.setPadding(new Insets(20));
        mainLayout.setStyle("-fx-background-color: #f8f9fa;");
        
        // Header with navigation
        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER);
        
        Button prevMonth = new Button("Previous");
        prevMonth.setStyle("-fx-padding: 8 15;");
        prevMonth.setOnAction(e -> {
            currentYearMonth = currentYearMonth.minusMonths(1);
            updateCalendar();
        });
        
        monthLabel = new Label();
        monthLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        Button nextMonth = new Button("Next");
        nextMonth.setStyle("-fx-padding: 8 15;");
        nextMonth.setOnAction(e -> {
            currentYearMonth = currentYearMonth.plusMonths(1);
            updateCalendar();
        });
        
        Button todayBtn = new Button("Today"); // Style removed for a cleaner look
        todayBtn.setStyle("-fx-padding: 8 15;");
        todayBtn.setOnAction(e -> {
            currentYearMonth = YearMonth.now();
            updateCalendar();
        });
        
        header.getChildren().addAll(prevMonth, monthLabel, nextMonth, todayBtn);
        
        // Day headers
        GridPane dayHeaders = new GridPane();
        dayHeaders.setHgap(5);
        dayHeaders.setVgap(5);
        dayHeaders.setPadding(new Insets(0, 0, 10, 0));
        
        for (int i = 0; i < 7; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(100.0 / 7);
            dayHeaders.getColumnConstraints().add(col);
        }
        
        String[] days = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        for (int i = 0; i < 7; i++) {
            VBox dayHeader = new VBox();
            dayHeader.setAlignment(Pos.CENTER);
            dayHeader.setPrefHeight(40);
            dayHeader.setMaxWidth(Double.MAX_VALUE);
            dayHeader.setStyle("-fx-background-color: #2c3e50; -fx-background-radius: 5;");
            
            Label dayLabel = new Label(days[i]);
            dayLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: white;");
            dayHeader.getChildren().add(dayLabel);
            
            dayHeaders.add(dayHeader, i, 0);
        }
        
        // Calendar grid
        calendarGrid = new GridPane();
        calendarGrid.setHgap(5);
        calendarGrid.setVgap(5);
        VBox.setVgrow(calendarGrid, Priority.ALWAYS);
        
        for (int i = 0; i < 7; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(100.0 / 7);
            calendarGrid.getColumnConstraints().add(col);
        }
        
        for (int i = 0; i < 6; i++) {
            RowConstraints row = new RowConstraints();
            row.setPercentHeight(100.0 / 6);
            calendarGrid.getRowConstraints().add(row);
        }
        
        // Initialize calendar
        updateCalendar();
        
        // Controls panel
        HBox controls = new HBox(15);
        controls.setPadding(new Insets(20, 0, 0, 0));
        controls.setAlignment(Pos.CENTER);
        
        if (includeCloseButton) {
            Button closeBtn = new Button("Close");
            closeBtn.setId("closeBtn"); // Set an ID to look it up later
            controls.getChildren().add(closeBtn);
        }
        
        mainLayout.getChildren().addAll(header, dayHeaders, calendarGrid);
        if (!controls.getChildren().isEmpty()) {
            mainLayout.getChildren().add(controls);
        }
        
        return mainLayout;
    }
    
    
    private void updateCalendar() {
        monthLabel.setText(currentYearMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.US) + 
                          " " + currentYearMonth.getYear());
        
        calendarGrid.getChildren().clear();
        
        LocalDate firstOfMonth = currentYearMonth.atDay(1);
        int dayOfWeek = firstOfMonth.getDayOfWeek().getValue() % 7; // 0=Sun, 6=Sat
        
        int daysInMonth = currentYearMonth.lengthOfMonth();
        int rows = 6; // Maximum rows needed
        
        // Fetch events for the current month to display indicators
        int currentUserId = SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getId() : 0;
        List<Event> allEvents = databaseService.getAllEvents();
        List<Event> userEvents = new ArrayList<>();
        for (Event e : allEvents) {
            if (e.getUserId() == currentUserId) {
                userEvents.add(e);
            }
        }
        
        // Create day cells
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = currentYearMonth.atDay(day);
            int column = (dayOfWeek + day - 1) % 7;
            int row = (dayOfWeek + day - 1) / 7;
            
            VBox dayCell = new VBox(5);
            dayCell.setMinHeight(70);
            dayCell.setMaxWidth(Double.MAX_VALUE);
            dayCell.setPadding(new Insets(5));

            // Day number
            Label dayNumber = new Label(String.valueOf(day));
            dayNumber.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
            
            // Highlight today
            if (date.equals(LocalDate.now())) {
                dayCell.setStyle("-fx-border-color: #3498db; -fx-border-width: 2; -fx-border-radius: 5; -fx-background-color: white; -fx-padding: 5;");
            } else {
                dayCell.setStyle("-fx-border-color: #ddd; -fx-border-radius: 5; -fx-padding: 5; -fx-background-color: white;");
            }
            
            // Add event indicators (dots)
            List<Event> dayEvents = new ArrayList<>();
            for (Event e : userEvents) {
                if (e.getEventDate().equals(date)) {
                    dayEvents.add(e);
                }
            }
            
            if (!dayEvents.isEmpty()) {
                HBox indicators = new HBox(2);
                indicators.setAlignment(Pos.CENTER);
                for (int k = 0; k < Math.min(dayEvents.size(), 3); k++) {
                    Event event = dayEvents.get(k);
                    Circle dot = new Circle(4, Color.web("#e74c3c"));
                    dot.setCursor(javafx.scene.Cursor.HAND);
                    Tooltip.install(dot, new Tooltip(event.getTitle()));
                    dot.setOnMouseClicked(ev -> {
                        ev.consume(); // Prevent click from reaching the day cell
                        if (onEventClicked != null) onEventClicked.accept(event);
                    });
                    indicators.getChildren().add(dot);
                }
                if (dayEvents.size() > 3) {
                    Label plus = new Label("+");
                    plus.setStyle("-fx-font-size: 8px;");
                    indicators.getChildren().add(plus);
                }
                dayCell.getChildren().add(indicators);
            }
            
            // Make day interactive
            dayCell.setOnMouseClicked(e -> {
                if (onDateClicked != null) {
                    onDateClicked.accept(date);
                }
            });
            dayCell.setStyle(dayCell.getStyle() + "-fx-cursor: hand;");
            
            dayCell.getChildren().add(dayNumber);
            calendarGrid.add(dayCell, column, row);
        }
        
        // Fill empty cells
        int totalCells = rows * 7;
        int filledCells = daysInMonth + dayOfWeek;
        
        for (int i = filledCells; i < totalCells; i++) {
            int column = i % 7;
            int row = i / 7;
            
            VBox emptyCell = new VBox();
            emptyCell.setMinHeight(70);
            emptyCell.setMaxWidth(Double.MAX_VALUE);
            emptyCell.setStyle("-fx-border-color: #eee; -fx-background-color: #fafafa;");
            calendarGrid.add(emptyCell, column, row);
        }
    }
    
    private void showAllEvents() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("All Events");
        alert.setHeaderText("Event List for " + currentYearMonth.getMonth() + " " + currentYearMonth.getYear());
        alert.setContentText("No events found.");
        alert.setWidth(400);
        alert.setHeight(400);
        alert.showAndWait();
    }

    public Node getCalendarView() {
        // This is the new public method that the Main class will call.
        // It returns the calendar view without a "Close" button.
        return getCalendarView(false);
    }
}
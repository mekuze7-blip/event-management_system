package com.calendar.app.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import com.calendar.app.models.Event;
import com.calendar.app.services.DatabaseService;
import com.calendar.app.utils.SessionManager;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.LocalTime;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.io.File;
import java.io.PrintWriter;
import javafx.stage.FileChooser;

public class EventManagerController {
    
    private ObservableList<Event> events;
    private TableView<Event> eventsTable;
    private DatabaseService databaseService = new DatabaseService();
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private Consumer<Event> onEditEventRequest;

    public void setOnEditEventRequest(Consumer<Event> onEditEventRequest) {
        this.onEditEventRequest = onEditEventRequest;
    }
    
    @SuppressWarnings({"deprecation", "unchecked"})
    public Node getEventManagerView() {
        // Load events ONLY for the current user to ensure Edit/Delete works
        int currentUserId = SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getId() : 0;
        events = FXCollections.observableArrayList(loadUserEvents());
        
        BorderPane mainLayout = new BorderPane();
        mainLayout.setPadding(new Insets(20));
        mainLayout.setStyle("-fx-background-color: #f8f9fa;");
        
        // Header
        HBox header = new HBox();
        header.setPadding(new Insets(0, 0, 20, 0));
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label title = new Label("My Event Manager");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        Label countLabel = new Label(events.size() + " events found");
        countLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d; -fx-padding: 0 0 0 20;");
        
        header.getChildren().addAll(title, countLabel);
        mainLayout.setTop(header);
        
        // Toolbar
        HBox toolbar = new HBox(10);
        toolbar.setPadding(new Insets(10));
        toolbar.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 5; -fx-padding: 10;");
        
        Button editBtn = new Button("Edit");
        editBtn.setStyle("-fx-background-color: #4ba0d8ff; -fx-text-fill: white; -fx-padding: 8 15;");
        editBtn.setOnAction(e -> editSelectedEvent());
        
        Button deleteBtn = new Button("Delete");
        deleteBtn.setStyle("-fx-background-color: #ba554aff; -fx-text-fill: white; -fx-padding: 8 15;");
        deleteBtn.setOnAction(e -> deleteSelectedEvent());
        
        Button refreshBtn = new Button("Refresh");
        refreshBtn.setStyle("-fx-padding: 8 15;");
        refreshBtn.setOnAction(e -> refreshTable());
        
        TextField searchField = new TextField();
        searchField.setPromptText("Search events...");
        searchField.setPrefWidth(200);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterEvents(newValue);
        });
        
        HBox.setHgrow(searchField, Priority.ALWAYS);
        toolbar.getChildren().addAll(editBtn, deleteBtn, refreshBtn, searchField);
        
        // Events table
        eventsTable = new TableView<>();
        eventsTable.setItems(events);
        eventsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // Create columns
        TableColumn<Event, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleCol.setPrefWidth(150);
        
        TableColumn<Event, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("eventDate"));
        dateCol.setPrefWidth(100);
        
        TableColumn<Event, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(cellData -> {
            Event event = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(
                event.getStartTime().format(timeFormatter) + " - " + event.getEndTime().format(timeFormatter)
            );
        });
        timeCol.setPrefWidth(120);
        
        TableColumn<Event, String> locationCol = new TableColumn<>("Location");
        locationCol.setCellValueFactory(new PropertyValueFactory<>("location"));
        locationCol.setPrefWidth(120);
        
        TableColumn<Event, String> phoneCol = new TableColumn<>("Phone");
        phoneCol.setCellValueFactory(new PropertyValueFactory<>("contactPhone"));
        phoneCol.setPrefWidth(100);
        
        // TableColumn<Event, String> emailCol = new TableColumn<>("Email");
        // emailCol.setCellValueFactory(new PropertyValueFactory<>("contactEmail"));
        // emailCol.setPrefWidth(150);
        
        TableColumn<Event, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        categoryCol.setPrefWidth(100);
        
        TableColumn<Event, String> userCol = new TableColumn<>("User");
        userCol.setCellValueFactory(new PropertyValueFactory<>("userName")); // Assumes Event has a 'userName' property
        userCol.setPrefWidth(80);
        
        eventsTable.getColumns().addAll(titleCol, dateCol, timeCol, locationCol, phoneCol, categoryCol, userCol);
        
        // Status bar
        HBox statusBar = new HBox();
        statusBar.setPadding(new Insets(10, 0, 0, 0));
        statusBar.setStyle("-fx-background-color: #ecf0f1; -fx-background-radius: 5; -fx-padding: 10;");
        
        Label status = new Label("Double-click an event to view details. Use search to filter events.");
        status.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");
        statusBar.getChildren().add(status);
        
        // Double-click to view details
        eventsTable.setRowFactory(tv -> {
            TableRow<Event> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    Event rowData = row.getItem();
                    if (onEditEventRequest != null) {
                        onEditEventRequest.accept(rowData);
                    } else {
                        showEventDetails(rowData);
                    }
                }
            });
            return row;
        });
        
        VBox center = new VBox(10, toolbar, eventsTable, statusBar);
        VBox.setVgrow(eventsTable, Priority.ALWAYS);
        mainLayout.setCenter(center);
        
        return mainLayout;
    }

    public void showEventManager(Stage parentStage) {
         // Deprecated: Use getEventManagerView() for dashboard integration
    }
    
    private void showAddEventDialog() {
        Dialog<Event> dialog = new Dialog<>();
        dialog.setTitle("Add New Event");
        dialog.setHeaderText("Create a new event");
        
        // Create form
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 100, 10, 10));
        
        TextField titleField = new TextField();
        titleField.setPromptText("Event Title");
        
        TextField locationField = new TextField();
        locationField.setPromptText("Location");
        
        TextField phoneField = new TextField();
        phoneField.setPromptText("Contact Phone");
        
        DatePicker datePicker = new DatePicker();
        datePicker.setValue(LocalDate.now().plusDays(1));
        datePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
            }
        });
        
        TextField startTimeField = new TextField();
        startTimeField.setPromptText("14:30");
        
        TextField endTimeField = new TextField();
        endTimeField.setPromptText("15:30");
        
        ComboBox<String> categoryBox = new ComboBox<>();
        categoryBox.getItems().addAll("Meeting", "Personal", "Work", "Social", "Other");
        categoryBox.setValue("Meeting");
        
        grid.add(new Label("Title*:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Location:"), 0, 1);
        grid.add(locationField, 1, 1);
        grid.add(new Label("Phone:"), 0, 2);
        grid.add(phoneField, 1, 2);
        grid.add(new Label("Date*:"), 0, 3);
        grid.add(datePicker, 1, 3);
        grid.add(new Label("Start Time*:"), 0, 4);
        grid.add(startTimeField, 1, 4);
        grid.add(new Label("End Time*:"), 0, 5);
        grid.add(endTimeField, 1, 5);
        grid.add(new Label("Category:"), 0, 6);
        grid.add(categoryBox, 1, 6);
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        // Validate before closing
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                if (titleField.getText().isEmpty()) {
                    showAlert("Error", "Title is required");
                    return null;
                }
                
                try {
                    LocalTime startTime = parseTime(startTimeField.getText(), LocalTime.of(9, 0));
                    LocalTime endTime = parseTime(endTimeField.getText(), startTime.plusHours(1));
                    
                    if (datePicker.getValue() != null && java.time.LocalDateTime.of(datePicker.getValue(), startTime).isBefore(java.time.LocalDateTime.now())) {
                        showAlert("Error", "Event cannot be scheduled in the past.");
                        return null;
                    }
                    
                    Event newEvent = new Event(
                        titleField.getText(),
                        "",
                        locationField.getText(),
                        datePicker.getValue(),
                        startTime,
                        endTime,
                        categoryBox.getValue(),
                        SessionManager.getCurrentUser().getId()
                    );
                    newEvent.setContactPhone(phoneField.getText());
                    return newEvent;
                } catch (Exception e) {
                    showAlert("Error", "Invalid time format. Please use HH:mm (e.g., 14:30 or 9).");
                    return null;
                }
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(newEvent -> {
            if (databaseService.addEvent(newEvent)) {
                events.add(newEvent);
                eventsTable.refresh();
                showAlert("Success", "Event '" + newEvent.getTitle() + "' added successfully!");
            } else {
                showAlert("Error", "Failed to save event to database.");
            }
        });
    }
    
    private void editSelectedEvent() {
        Event selected = eventsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select an event to edit.");
            return;
        }
        
        if (onEditEventRequest != null) {
            onEditEventRequest.accept(selected);
            return;
        }
        
        Dialog<Event> dialog = new Dialog<>();
        dialog.setTitle("Edit Event");
        dialog.setHeaderText("Edit event details");
        
        // Create form
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 100, 10, 10));
        
        TextField titleField = new TextField(selected.getTitle());
        
        TextField locationField = new TextField(selected.getLocation());
        
        TextField phoneField = new TextField(selected.getContactPhone());
        
        DatePicker datePicker = new DatePicker(selected.getEventDate());
        datePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
            }
        });
        
        TextField startTimeField = new TextField(selected.getStartTime().format(timeFormatter));
        
        TextField endTimeField = new TextField(selected.getEndTime().format(timeFormatter));
        
        ComboBox<String> categoryBox = new ComboBox<>();
        categoryBox.getItems().addAll("Meeting", "Personal", "Work", "Social", "Other");
        categoryBox.setValue(selected.getCategory());
        
        grid.add(new Label("Title*:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Location:"), 0, 1);
        grid.add(locationField, 1, 1);
        grid.add(new Label("Phone:"), 0, 2);
        grid.add(phoneField, 1, 2);
        grid.add(new Label("Date*:"), 0, 3);
        grid.add(datePicker, 1, 3);
        grid.add(new Label("Start Time*:"), 0, 4);
        grid.add(startTimeField, 1, 4);
        grid.add(new Label("End Time*:"), 0, 5);
        grid.add(endTimeField, 1, 5);
        grid.add(new Label("Category:"), 0, 6);
        grid.add(categoryBox, 1, 6);
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        // Validate before closing
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                if (titleField.getText().isEmpty()) {
                    showAlert("Error", "Title is required");
                    return null;
                }
                
                try {
                    LocalDate newDate = datePicker.getValue();
                    LocalTime newStartTime = parseTime(startTimeField.getText(), selected.getStartTime());
                    
                    if (newDate != null && java.time.LocalDateTime.of(newDate, newStartTime).isBefore(java.time.LocalDateTime.now())) {
                        showAlert("Error", "Event cannot be scheduled in the past.");
                        return null;
                    }
                    
                    selected.setTitle(titleField.getText());
                    selected.setDescription("");
                    selected.setLocation(locationField.getText());
                    selected.setEventDate(newDate);
                    selected.setStartTime(newStartTime);
                    selected.setEndTime(parseTime(endTimeField.getText(), selected.getEndTime()));
                    selected.setCategory(categoryBox.getValue());
                    selected.setContactPhone(phoneField.getText());
                    
                    return selected;
                } catch (Exception e) {
                    showAlert("Error", "Invalid time format. Please use HH:mm (e.g., 14:30 or 9).");
                    return null;
                }
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(updatedEvent -> {
            if (databaseService.updateEvent(updatedEvent)) {
                eventsTable.refresh();
                showAlert("Success", "Event '" + updatedEvent.getTitle() + "' updated successfully!");
            } else {
                showAlert("Error", "Failed to update event in database.");
            }
        });
    }
    
    private void deleteSelectedEvent() {
        Event selected = eventsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select an event to delete.");
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Event");
        confirm.setContentText("Are you sure you want to delete '" + selected.getTitle() + "'?");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (databaseService.deleteEvent(selected.getId(), SessionManager.getCurrentUser().getId())) {
                    events.remove(selected);
                    eventsTable.refresh();
                    showAlert("Deleted", "Event '" + selected.getTitle() + "' has been deleted.");
                } else {
                    showAlert("Error", "Failed to delete event from database.");
                }
            }
        });
    }
    
    public void refreshTable() {
        if (events == null) return;
        events.clear();
        events.addAll(loadUserEvents());
        
        // showAlert("Refreshed", "Event list has been refreshed from database."); // Silent refresh is better for auto-updates
    }
    
    private void exportEventsToCSV() {
        if (events == null || events.isEmpty()) {
            showAlert("No Data", "There are no events to export.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Events");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("my_events.csv");
        
        File file = fileChooser.showSaveDialog(eventsTable.getScene().getWindow());
        
        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file)) {
                // Header
                writer.println("Title,Date,Start Time,End Time,Location,Category,Phone");
                
                // Data
                for (Event e : events) {
                    writer.printf("%s,%s,%s,%s,%s,%s,%s%n",
                        escapeCSV(e.getTitle()),
                        e.getEventDate(),
                        e.getStartTime(),
                        e.getEndTime(),
                        escapeCSV(e.getLocation()),
                        escapeCSV(e.getCategory()),
                        escapeCSV(e.getContactPhone())
                    );
                }
                
                showAlert("Success", "Events exported successfully to " + file.getAbsolutePath());
            } catch (Exception ex) {
                showAlert("Error", "Failed to export events: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }
    
    private String escapeCSV(String text) {
        if (text == null) return "";
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
    
    private List<Event> loadUserEvents() {
        List<Event> userEvents = new ArrayList<>();
        int currentUserId = SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getId() : 0;
        if (currentUserId == 0) return userEvents;

        // Direct DB call to ensure all fields (phone, email) are loaded correctly.
        String sql = "SELECT * FROM events WHERE user_id = ? ORDER BY event_date DESC, start_time DESC";
        try (Connection conn = com.calendar.app.utils.DatabaseConnection.getReusableConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, currentUserId);
            var rs = stmt.executeQuery();
            while (rs.next()) {
                Event event = new Event();
                event.setId(rs.getInt("id"));
                event.setTitle(rs.getString("title"));
                event.setDescription("");
                event.setLocation(rs.getString("location"));
                event.setEventDate(rs.getDate("event_date").toLocalDate());
                event.setStartTime(rs.getTime("start_time").toLocalTime());
                event.setEndTime(rs.getTime("end_time").toLocalTime());
                event.setCategory(rs.getString("category"));
                event.setUserId(rs.getInt("user_id"));
                event.setContactPhone(rs.getString("contact_phone"));
                // event.setContactEmail(rs.getString("contact_email"));
                event.setUserName(SessionManager.getCurrentUser().getUsername());
                userEvents.add(event);
            }
        } catch (SQLException ex) {
            System.err.println("Failed to load events directly: " + ex.getMessage());
            showAlert("Database Error", "Could not load events. Some data might be missing. Error: " + ex.getMessage());
        }
        return userEvents;
    }

    private void filterEvents(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            eventsTable.setItems(events);
        } else {
            ObservableList<Event> filtered = FXCollections.observableArrayList();
            for (Event event : events) {
                if (event.getTitle().toLowerCase().contains(searchText.toLowerCase()) ||
                    event.getLocation().toLowerCase().contains(searchText.toLowerCase()) ||
                    event.getCategory().toLowerCase().contains(searchText.toLowerCase())) {
                    filtered.add(event);
                }
            }
            eventsTable.setItems(filtered);
        }
    }
    
    private void showEventDetails(Event event) {
        Alert details = new Alert(Alert.AlertType.INFORMATION);
        details.setTitle("Event Details");
        details.setHeaderText(event.getTitle());
        
        String content = String.format(
            "Date: %s\n" +
            "Time: %s - %s\n" +
            "Location: %s\n" +
            "Phone: %s\n" +
            "Category: %s\n" +
            "User: %s", // Added User
            event.getEventDate(),
            event.getStartTime(),
            event.getEndTime(),
            event.getLocation(),
            event.getContactPhone(),
            event.getCategory(),
            event.getUserName() // Assuming Event model has getUserName()
        );
        
        details.setContentText(content);
        details.setWidth(400);
        details.showAndWait();
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private LocalTime parseTime(String timeText, LocalTime defaultTime) {
        if (timeText == null || timeText.isBlank()) {
            return defaultTime;
        }
        timeText = timeText.trim();
        // Handle single digit input like "9" -> "09:00"
        if (timeText.matches("\\d{1,2}")) {
            timeText = String.format("%02d:00", Integer.parseInt(timeText));
        }
        // Handle "HH:mm"
        return LocalTime.parse(timeText, timeFormatter);
    }
}
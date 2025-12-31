package com.calendar.app.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import com.calendar.app.models.Event;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class EventController {

    @FXML private TableView<Event> eventsTable;
    @FXML private TableColumn<Event, String> titleColumn;
    @FXML private TableColumn<Event, String> dateColumn;
    @FXML private TableColumn<Event, String> timeColumn;
    @FXML private TableColumn<Event, String> locationColumn;
    @FXML private TableColumn<Event, String> categoryColumn;

    @FXML private TextField searchField;
    @FXML private ChoiceBox<String> filterChoice;

    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    public void initialize() {
        System.out.println("EventController initialized");

        // Setup table columns
        titleColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTitle()));
        
        dateColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getEventDate().format(dateFormatter)
            ));
        
        timeColumn.setCellValueFactory(cellData -> {
            Event event = cellData.getValue();
            String timeRange = event.getStartTime().format(timeFormatter) + " - " + 
                              event.getEndTime().format(timeFormatter);
            return new javafx.beans.property.SimpleStringProperty(timeRange);
        });
        
        locationColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getLocation()));
        
        categoryColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getCategory()));

        // Setup filter choices
        filterChoice.getItems().addAll("All Events", "Today", "This Week", "This Month", "Upcoming");
        filterChoice.setValue("All Events");

        // Setup search functionality
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterEvents();
        });

        filterChoice.valueProperty().addListener((observable, oldValue, newValue) -> {
            filterEvents();
        });

        loadEvents();
    }

    private void loadEvents() {
        eventsTable.getItems().clear();
        
        // For testing, create sample events since we don't have real EventService yet
        createSampleEvents();
        
        System.out.println("Loaded sample events for testing");
    }

    private void createSampleEvents() {
        // Create sample events for testing
        java.util.List<Event> sampleEvents = new java.util.ArrayList<>();
        
        // Event 1
        Event event1 = new Event();
        event1.setTitle("Team Meeting");
        event1.setEventDate(LocalDate.now().plusDays(1));
        event1.setStartTime(LocalTime.of(14, 30));
        event1.setEndTime(LocalTime.of(15, 30));
        event1.setLocation("Conference Room A");
        event1.setCategory("Meeting");
        sampleEvents.add(event1);

        // Event 2
        Event event2 = new Event();
        event2.setTitle("Doctor Appointment");
        event2.setEventDate(LocalDate.now().plusDays(2));
        event2.setStartTime(LocalTime.of(10, 0));
        event2.setEndTime(LocalTime.of(11, 0));
        event2.setLocation("Medical Center");
        event2.setCategory("Personal");
        sampleEvents.add(event2);

        // Event 3
        Event event3 = new Event();
        event3.setTitle("Project Deadline");
        event3.setEventDate(LocalDate.now().plusDays(5));
        event3.setStartTime(LocalTime.of(17, 0));
        event3.setEndTime(LocalTime.of(18, 0));
        event3.setLocation("Office");
        event3.setCategory("Work");
        sampleEvents.add(event3);

        // Event 4
        Event event4 = new Event();
        event4.setTitle("Birthday Party");
        event4.setEventDate(LocalDate.now().plusDays(7));
        event4.setStartTime(LocalTime.of(19, 0));
        event4.setEndTime(LocalTime.of(22, 0));
        event4.setLocation("City Restaurant");
        event4.setCategory("Social");
        sampleEvents.add(event4);

        eventsTable.getItems().addAll(sampleEvents);
    }

    private void filterEvents() {
        String searchText = searchField.getText().toLowerCase();
        String filter = filterChoice.getValue();

        java.util.List<Event> filteredEvents = new java.util.ArrayList<>();

        for (Event event : eventsTable.getItems()) {
            boolean matches = true;

            // Search text filter
            if (!searchText.isEmpty()) {
                matches = event.getTitle().toLowerCase().contains(searchText) ||
                         event.getLocation().toLowerCase().contains(searchText) ||
                         event.getCategory().toLowerCase().contains(searchText);
            }

            // Date filter
            if (matches && !filter.equals("All Events")) {
                LocalDate eventDate = event.getEventDate();
                LocalDate today = LocalDate.now();

                switch (filter) {
                    case "Today":
                        matches = eventDate.equals(today);
                        break;
                    case "This Week":
                        LocalDate endOfWeek = today.plusDays(7);
                        matches = !eventDate.isBefore(today) && !eventDate.isAfter(endOfWeek);
                        break;
                    case "This Month":
                        matches = eventDate.getMonth() == today.getMonth() && 
                                 eventDate.getYear() == today.getYear();
                        break;
                    case "Upcoming":
                        matches = !eventDate.isBefore(today);
                        break;
                }
            }

            if (matches) {
                filteredEvents.add(event);
            }
        }

        // Update table
        eventsTable.getItems().clear();
        eventsTable.getItems().addAll(filteredEvents);
    }

    @FXML
    private void handleAddEvent() {
        System.out.println("Add Event button clicked");
        showEventDialog("Add New Event", null);
    }

    @FXML
    private void handleEditEvent() {
        Event selected = eventsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            System.out.println("Edit Event: " + selected.getTitle());
            showEventDialog("Edit Event", selected);
        } else {
            showAlert("No Selection", "Please select an event to edit.");
        }
    }

    @FXML
    private void handleDeleteEvent() {
        Event selected = eventsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm Delete");
            confirm.setHeaderText("Delete Event");
            confirm.setContentText("Are you sure you want to delete '" + selected.getTitle() + "'?");

            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    eventsTable.getItems().remove(selected);
                    showAlert("Success", "Event deleted successfully.");
                }
            });
        } else {
            showAlert("No Selection", "Please select an event to delete.");
        }
    }

    @FXML
    private void handleRefresh() {
        loadEvents();
        searchField.clear();
        filterChoice.setValue("All Events");
        showAlert("Refreshed", "Event list has been refreshed.");
    }

    private void showEventDialog(String title, Event eventToEdit) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText("Event Dialog");
        alert.setContentText("This would open a dialog to " + (eventToEdit == null ? "add" : "edit") + " an event.");
        alert.showAndWait();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleBackToDashboard() {
        System.out.println("Back to Dashboard clicked from EventController");
        // Simply close this window so the main dashboard (owner) becomes visible again
        Stage stage = (Stage) eventsTable.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleCalendarView() {
        System.out.println("Open Calendar View clicked from EventController");
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/calendar.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Calendar View");
            stage.setScene(new Scene(root, 900, 600));
            stage.initOwner(eventsTable.getScene().getWindow());
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Unable to open calendar view: " + e.getMessage());
        }
    }

    @FXML
    private void handleExportEvents() {
        System.out.println("Export Events clicked from EventController");
        showAlert("Export", "Export functionality is not implemented yet.");
    }
}
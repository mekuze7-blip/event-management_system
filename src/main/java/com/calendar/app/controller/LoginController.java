package com.calendar.app.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import com.calendar.app.models.User;
import com.calendar.app.utils.SessionManager;
import java.io.IOException;

public class LoginController {
    
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    
    @FXML
    public void initialize() {
        System.out.println("? LoginController initialized - NO BOM ISSUES");
        errorLabel.setVisible(false);
        
        usernameField.requestFocus();
    }
    
    @FXML
    private void handleLogin() {
        System.out.println("=== LOGIN ATTEMPT ===");
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        
        System.out.println("Username: " + username);
        System.out.println("Password: " + password);
        
        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password");
            return;
        }
        
        // SIMPLE AUTH - NO DATABASE REQUIRED
        if (authenticateSimple(username, password)) {
            System.out.println("? Login successful!");
            
            // Create user object
            User user = new User();
            user.setId(1);
            user.setUsername(username);
            user.setFullName(getFullName(username));
            user.setEmail(username + "@example.com");
            user.setRole(getRole(username));
            
            SessionManager.setCurrentUser(user);
            loadMainApplication();
        } else {
            showError("Invalid username or password");
        }
    }
    
    private boolean authenticateSimple(String username, String password) {
        // Accept ANY login for testing
        System.out.println("Accepting ALL logins for testing...");
        return true;
        
        /* Uncomment for actual authentication:
        return (username.equals("john.doe") && password.equals("password123")) ||
               (username.equals("jane.smith") && password.equals("password456"));
        */
    }
    
    private String getFullName(String username) {
        switch (username) {
            case "john.doe": return "John Doe";
            case "jane.smith": return "Jane Smith";
            default: return username;
        }
    }
    
    private String getRole(String username) {
        return "USER";
    }
    
    private void loadMainApplication() {
        try {
            Stage currentStage = (Stage) usernameField.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/main.fxml"));
            Stage mainStage = new Stage();
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            mainStage.setScene(scene);
            mainStage.setTitle("Event Management System v2.0 - Dashboard");
            mainStage.show();
            currentStage.close();
        } catch (IOException e) {
            showError("Failed to load application: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleRegister() {
        try {
            Stage currentStage = (Stage) usernameField.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/registration.fxml"));
            Scene scene = new Scene(root, 800, 600);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            currentStage.setScene(scene);
            currentStage.setTitle("Registration - Event Management System");
        } catch (Exception e) {
            showError("Failed to load registration: " + e.getMessage());
        }
    }
    
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}
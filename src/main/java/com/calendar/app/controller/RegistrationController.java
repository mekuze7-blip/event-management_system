package com.calendar.app.controllers;

import com.calendar.app.services.RegistrationService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class RegistrationController {
    
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField emailField;
    @FXML private TextField fullNameField;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;
    @FXML private Button registerButton;
    @FXML private Button backToLoginButton;
    
    private RegistrationService registrationService = new RegistrationService();
    
    @FXML
    public void initialize() {
        System.out.println("RegistrationController initialized");
        clearMessages();
        
        // Set up field listeners for real-time validation
        setupFieldValidation();
        
        // Pre-fill for testing (optional)
        usernameField.setText("newuser");
        passwordField.setText("password123");
        confirmPasswordField.setText("password123");
        emailField.setText("newuser@example.com");
        fullNameField.setText("New User");
    }
    
    private void setupFieldValidation() {
        // Real-time password match checking
        passwordField.textProperty().addListener((observable, oldValue, newValue) -> {
            checkPasswordMatch();
        });
        
        confirmPasswordField.textProperty().addListener((observable, oldValue, newValue) -> {
            checkPasswordMatch();
        });
        
        // Email format validation
        emailField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.isEmpty() && !newValue.contains("@")) {
                emailField.setStyle("-fx-border-color: #e74c3c;");
            } else {
                emailField.setStyle("");
            }
        });
    }
    
    private void checkPasswordMatch() {
        String password = passwordField.getText();
        String confirm = confirmPasswordField.getText();
        
        if (!confirm.isEmpty() && !password.equals(confirm)) {
            confirmPasswordField.setStyle("-fx-border-color: #e74c3c;");
        } else {
            confirmPasswordField.setStyle("");
        }
    }
    
    @FXML
    private void handleRegister() {
        clearMessages();
        
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String email = emailField.getText().trim();
        String fullName = fullNameField.getText().trim();
        
        // Basic validation
        if (username.isEmpty() || password.isEmpty() || email.isEmpty() || fullName.isEmpty()) {
            showError("All fields are required");
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match");
            return;
        }
        
        if (password.length() < 6) {
            showError("Password must be at least 6 characters");
            return;
        }
        
        if (!email.contains("@")) {
            showError("Please enter a valid email address");
            return;
        }
        
        // Disable button during registration
        registerButton.setDisable(true);
        registerButton.setText("Registering...");
        
        // Try to register
        RegistrationService.RegistrationResult result = registrationService.registerUser(
            username, password, email, fullName
        );
        
        if (result.isSuccess()) {
            showSuccess(result.getMessage());
            
            // Clear form
            usernameField.clear();
            passwordField.clear();
            confirmPasswordField.clear();
            emailField.clear();
            fullNameField.clear();
            
            // Enable registration after delay
            new java.util.Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        javafx.application.Platform.runLater(() -> {
                            registerButton.setDisable(false);
                            registerButton.setText("Register");
                        });
                    }
                },
                2000
            );
            
        } else {
            showError(result.getMessage());
            registerButton.setDisable(false);
            registerButton.setText("Register");
        }
    }
    
    @FXML
    private void handleBackToLogin() {
        try {
            Stage currentStage = (Stage) backToLoginButton.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
            Scene scene = new Scene(root, 800, 600);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            currentStage.setScene(scene);
            currentStage.setTitle("Login - Event Management System");
        } catch (Exception e) {
            System.err.println("Error returning to login: " + e.getMessage());
        }
    }
    
    private void showError(String message) {
        errorLabel.setText("❌ " + message);
        errorLabel.setVisible(true);
        successLabel.setVisible(false);
    }
    
    private void showSuccess(String message) {
        successLabel.setText("✅ " + message);
        successLabel.setVisible(true);
        errorLabel.setVisible(false);
    }
    
    private void clearMessages() {
        errorLabel.setVisible(false);
        successLabel.setVisible(false);
    }
}
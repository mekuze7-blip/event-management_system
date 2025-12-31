package com.calendar.app.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public class MainController {
    
    @FXML
    private Button logoutButton;
    
    @FXML
    public void initialize() {
        System.out.println("? MainController initialized");
        
        logoutButton.setOnAction(e -> {
            try {
                Stage currentStage = (Stage) logoutButton.getScene().getWindow();
                Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
                Stage loginStage = new Stage();
                Scene scene = new Scene(root, 800, 600);
                scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
                loginStage.setScene(scene);
                loginStage.setTitle("Login");
                loginStage.show();
                currentStage.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }
}
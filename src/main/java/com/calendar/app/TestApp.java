package com.calendar.app;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class TestApp extends Application {
    @Override
    public void start(Stage primaryStage) {
        try {
            System.out.println("Testing FXML loading...");

            // Try to load FXML
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
            System.out.println("✅ FXML loaded successfully!");

            Scene scene = new Scene(root, 800, 600);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            primaryStage.setTitle("Login Test - SUCCESS");
            primaryStage.setScene(scene);
            primaryStage.show();

        } catch (Exception e) {
            System.err.println("❌ FXML loading failed: " + e.getMessage());
            e.printStackTrace();

            // Fallback: Show simple window
            Label label = new Label("FXML Error: " + e.getMessage());
            VBox fallback = new VBox(label);
            Scene scene = new Scene(fallback, 600, 400);
            primaryStage.setTitle("Fallback Window");
            primaryStage.setScene(scene);
            primaryStage.show();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
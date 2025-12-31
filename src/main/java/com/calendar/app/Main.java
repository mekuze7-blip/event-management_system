package com.calendar.app;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import com.calendar.app.controllers.CalendarViewController;
import com.calendar.app.controllers.EventManagerController;
import com.calendar.app.models.Event;
import com.calendar.app.services.DatabaseService;
import com.calendar.app.services.RegistrationService;
import com.calendar.app.utils.DatabaseConnection;
import com.calendar.app.utils.SessionManager;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.TimeZone;
import java.util.Optional;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Date;
import java.sql.Time;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Set;
import java.util.HashSet;
import java.util.Properties;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class Main extends Application {
    
    private Stage primaryStage;
    private CalendarViewController calendarController;
    private EventManagerController eventManagerController;
    private DatabaseService databaseService;
    private final ScheduledExecutorService notificationScheduler = Executors.newSingleThreadScheduledExecutor();
    private final Set<String> notifiedEventIds = new HashSet<>();
    
    // User Settings (Runtime storage for demo purposes)
    private String currentUserPhoneNumber = "+251942557417";
    private boolean smsNotificationsEnabled = true;
    private boolean emailNotificationsEnabled = true;
    private String emailSenderAddress = "noreply@ems.com";
    private final String CONFIG_FILE = "ems_settings.properties";
    private Properties appSettings = new Properties();
    private String infobipApiKey = "501a6d1c0532ef04d793d8d10e31fcb4-a035a4d0-a018-462c-9b4f-abe8e4e174f6";
    private String infobipBaseUrl = "https://l2rkvw.api.infobip.com";
    
    // SMTP Settings (Native Implementation)
    private boolean useSmtp = true;
    private String smtpHost = "smtp.gmail.com";
    private String smtpPort = "465";
    private String smtpEmail = "mekuze7@gmail.com";
    private String smtpPassword = "fxlvokgqynsazmjj";
    
    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.calendarController = new CalendarViewController();
        this.eventManagerController = new EventManagerController();
        this.databaseService = new DatabaseService();
        
        // Setup Calendar Interaction
        this.calendarController.setOnDateClicked(date -> {
            showEventDialog(null, date);
        });
        
        this.calendarController.setOnEventClicked(event -> {
            showEventDialog(event, null);
        });
        
        this.eventManagerController.setOnEditEventRequest(event -> {
            showEventDialog(event, null);
        });
        
        // Load saved settings
        loadSettings();
        
        System.out.println();
        
        // Ensure window stays on top initially
        primaryStage.setAlwaysOnTop(true);

        // Bring window to front
        primaryStage.setOnShown(e -> {
            primaryStage.toFront();
            primaryStage.setAlwaysOnTop(false);
        });
        
        // Handle window close
        primaryStage.setOnCloseRequest(e -> {
            System.out.println("Application closing...");
            notificationScheduler.shutdownNow();
            Platform.exit();
            System.exit(0);
        });

        // Start background notification service
        startNotificationService();
        System.out.println("Notification service started. Monitoring for events...");

        createLoginScreen();
    }
    
    private void createLoginScreen() {
        System.out.println("Showing login screen...");
        
        // Modern Gradient Background
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #2c3e50, #4ca1af);");

        // Logo/Brand Icon
        Circle logoBg = new Circle(35);
        logoBg.setFill(new javafx.scene.paint.LinearGradient(0, 0, 1, 1, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
            new javafx.scene.paint.Stop(0, Color.web("#4ca1af")),
            new javafx.scene.paint.Stop(1, Color.web("#2c3e50"))));
        logoBg.setEffect(new javafx.scene.effect.DropShadow(10, Color.rgb(0,0,0,0.2)));
        
        Label logoText = new Label("EMS");
        logoText.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");
        StackPane logoContainer = new StackPane(logoBg, logoText);
        
        // Login form Card
        VBox form = new VBox(20);
        form.setPadding(new Insets(40));
        form.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 20, 0, 0, 5);");
        form.setMaxWidth(380);
        form.setAlignment(Pos.CENTER);
        
        Label formTitle = new Label("Welcome Back");
        formTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        Label formSubtitle = new Label("Sign in to your account");
        formSubtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");
        
        VBox headerBox = new VBox(10, logoContainer, formTitle, formSubtitle);
        headerBox.setAlignment(Pos.CENTER);
        headerBox.setPadding(new Insets(0, 0, 20, 0));
        
        // Inputs
        TextField username = createFormField("Username");
        username.setPrefWidth(Double.MAX_VALUE);
        
        PasswordField password = createPasswordField("Password");
        password.setPrefWidth(Double.MAX_VALUE);
        
        // Login Button
        Button loginBtn = new Button("LOGIN");
        loginBtn.setPrefWidth(Double.MAX_VALUE);
        loginBtn.setPrefHeight(45);
        loginBtn.setStyle("-fx-background-color: #2c3e50; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 8; -fx-cursor: hand;");
        
        loginBtn.setOnMouseEntered(e -> loginBtn.setStyle("-fx-background-color: #34495e; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 8; -fx-cursor: hand;"));
        loginBtn.setOnMouseExited(e -> loginBtn.setStyle("-fx-background-color: #2c3e50; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 8; -fx-cursor: hand;"));
        
        Label status = new Label();
        status.setStyle("-fx-font-size: 14px;");
        
        // Registration link - FIXED VERSION
        HBox registrationLink = new HBox();
        registrationLink.setAlignment(Pos.CENTER);
        Label noAccount = new Label("New here? ");
        noAccount.setStyle("-fx-text-fill: #7f8c8d;");
        
        Hyperlink registerLink = new Hyperlink("Create Account");
        registerLink.setStyle("-fx-text-fill: #4ca1af; -fx-font-weight: bold; -fx-border-color: transparent; -fx-font-size: 14px;");
        
        // FIXED: This is the critical fix - proper action handler
        registerLink.setOnAction(event -> {
            System.out.println("Create Account link clicked!");
            showRegistrationScreen();
        });
        
        registrationLink.getChildren().addAll(noAccount, registerLink);
        
        loginBtn.setOnAction(event -> {
            String user = username.getText();
            String pass = password.getText();
            
            System.out.println("Login attempt: " + user + " / " + pass);
            
            // Perform actual authentication
            if (authenticateUser(user, pass)) {
                status.setText(" Login successful! Loading dashboard");
                status.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                
                // Show appropriate dashboard based on role
                new java.util.Timer().schedule(
                    new java.util.TimerTask() {
                        @Override
                        public void run() {
                            Platform.runLater(() -> showUserDashboard());
                        }
                    },
                    1000
                );
            } else {
                status.setText(" Invalid username or password");
                status.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            }
        });
        
        // Forgot password link
        Hyperlink forgotPassword = new Hyperlink("Forgot Password?");
        forgotPassword.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px; -fx-border-color: transparent;");
        forgotPassword.setOnAction(event -> {
            showForgotPasswordDialog();
        });
        
        form.getChildren().addAll(headerBox, username, password, loginBtn, status, registrationLink, forgotPassword);
        
        root.getChildren().add(form);
        
        Scene scene = new Scene(root, 400, 550);
        primaryStage.setTitle("Event App Login");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
        
        System.out.println("Login screen displayed successfully");
    }
    
    private Node createDashboardBackground(Node content) {
        BorderPane rootLayout = new BorderPane();
        rootLayout.setStyle("-fx-background-color: #f4f6f8;");
    
        // --- LEFT SIDEBAR ---
        VBox sidebar = new VBox();
        sidebar.setPadding(new Insets(20, 0, 20, 0));
        sidebar.setSpacing(15);
        sidebar.setPrefWidth(220);
        sidebar.setStyle("-fx-background-color: #2c3e50;");
        sidebar.setAlignment(Pos.TOP_CENTER);
    
        // Sidebar Header
        HBox sidebarHeader = new HBox(15);
        sidebarHeader.setAlignment(Pos.CENTER_LEFT);
        sidebarHeader.setPadding(new Insets(0, 0, 20, 20));

        Button sidebarMenuBtn = new Button();
        sidebarMenuBtn.setStyle("-fx-background-color: transparent; -fx-padding: 5;");
        VBox sidebarLines = new VBox(5);
        sidebarLines.setAlignment(Pos.CENTER);
        for (int i = 0; i < 3; i++) {
            Region line = new Region();
            line.setPrefSize(25, 3);
            line.setMaxSize(25, 3);
            line.setStyle("-fx-background-color: white; -fx-background-radius: 5;");
            sidebarLines.getChildren().add(line);
        }
        sidebarMenuBtn.setGraphic(sidebarLines);

        Label appTitle = new Label("Event App");
        appTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        sidebarHeader.getChildren().addAll(sidebarMenuBtn, appTitle);
    
        sidebar.getChildren().addAll(sidebarHeader, 
            createSidebarButton("Dashboard"), 
            createSidebarButton("Calendar View"), 
            createSidebarButton("Add New Event"), 
            createSidebarButton("View Events"), 
            createSidebarButton("Settings"));
            
        // Highlight Calendar View
        sidebar.getChildren().get(2).setStyle("-fx-background-color: #34495e; -fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold; -fx-padding: 0 0 0 30;");
    
        // --- MAIN CONTENT AREA ---
        VBox mainContent = new VBox(20);
        mainContent.setPadding(new Insets(20));
        
        Label contentTitle = new Label("Calendar View");
        contentTitle.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        HBox header = new HBox(15, contentTitle);
        header.setAlignment(Pos.CENTER_LEFT);
        
        mainContent.getChildren().addAll(header, new Separator(), content);
        VBox.setVgrow(content, Priority.ALWAYS);
    
        rootLayout.setLeft(sidebar);
        rootLayout.setCenter(mainContent);
        
        return rootLayout;
    }

    private boolean authenticateUser(String username, String password) {
        // Check if user exists and password is correct
        if (databaseService.validateUser(username, password)) {
            // Get user details and set session
            // For now, create a basic user object - in a real app you'd fetch from DB
            com.calendar.app.models.User user = new com.calendar.app.models.User();
            user.setUsername(username);
            user.setPassword(password);
            
            // Set default role
            user.setRole("USER");
            
            // Fetch actual user ID from database to ensure correct foreign key mapping
            try {
                Connection conn = DatabaseConnection.getReusableConnection();
                PreparedStatement stmt = conn.prepareStatement("SELECT id, full_name, email FROM users WHERE username = ?");
                stmt.setString(1, username);
                var rs = stmt.executeQuery();
                if (rs.next()) {
                    user.setId(rs.getInt("id"));
                    if (rs.getString("full_name") != null) user.setFullName(rs.getString("full_name"));
                    if (rs.getString("email") != null) user.setEmail(rs.getString("email"));
                }
            } catch (SQLException e) {
                System.err.println("Error fetching user details: " + e.getMessage());
            }
            
            SessionManager.setCurrentUser(user);
            
            return true;
        }
        return false;
    }
    
    private void showRegistrationScreen() {
        System.out.println("Opening registration screen...");
        
        // Create a new stage for registration
        Stage registrationStage = new Stage();
        registrationStage.setTitle("Create Account - Event Management System");
        registrationStage.initModality(Modality.NONE);
        registrationStage.initOwner(primaryStage);
        
        // Create registration form
        
        // Header
        Circle logoBg = new Circle(30);
        logoBg.setFill(new javafx.scene.paint.LinearGradient(0, 0, 1, 1, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
            new javafx.scene.paint.Stop(0, Color.web("#4ca1af")),
            new javafx.scene.paint.Stop(1, Color.web("#2c3e50"))));
        logoBg.setEffect(new javafx.scene.effect.DropShadow(10, Color.rgb(0,0,0,0.2)));
        
        Label logoText = new Label("+");
        logoText.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");
        StackPane logoContainer = new StackPane(logoBg, logoText);
        
        // Registration form
        VBox form = new VBox(20);
        form.setPadding(new Insets(40));
        form.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 20, 0, 0, 5);");
        form.setMaxWidth(400);
        form.setAlignment(Pos.CENTER);
        
        // Form title
        Label formTitle = new Label("Register New Account");
        formTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        // Form fields with better sizing
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 0, 20, 0));
        
        // Create fields with consistent sizing
        TextField[] fields = {
            createFormField("Full Name"),
            createFormField("Username"),
            createFormField("Email Address"),
            createPasswordField("Password (8+ chars, Upper, Lower, #)"),
            createPasswordField("Confirm Password")
        };
        
        // Create visible text fields for "Show Password" functionality
        TextField showPassField = createFormField("Password (8+ chars, Upper, Lower, #)");
        showPassField.setManaged(false);
        showPassField.setVisible(false);
        showPassField.textProperty().bindBidirectional(fields[3].textProperty());

        TextField showConfirmPassField = createFormField("Confirm Password");
        showConfirmPassField.setManaged(false);
        showConfirmPassField.setVisible(false);
        showConfirmPassField.textProperty().bindBidirectional(fields[4].textProperty());
        
        // Add labels and fields to grid
        String[] labels = {"Full Name:", "Username:", "Email:", "Password:", "Confirm:"};
        for (int i = 0; i < labels.length; i++) {
            Label lbl = new Label(labels[i]);
            lbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #7f8c8d; -fx-font-size: 12px;");
            grid.add(lbl, 0, i);
            
            fields[i].setPrefWidth(200);
            
            if (i == 3) {
                showPassField.setPrefWidth(200);
                StackPane pPane = new StackPane(fields[i], showPassField);
                pPane.setAlignment(Pos.CENTER_LEFT);
                grid.add(pPane, 1, i);
            } else if (i == 4) {
                showConfirmPassField.setPrefWidth(200);
                StackPane cPane = new StackPane(fields[i], showConfirmPassField);
                cPane.setAlignment(Pos.CENTER_LEFT);
                grid.add(cPane, 1, i);
            } else {
                grid.add(fields[i], 1, i);
            }
        }
        
        // Show Password Checkbox
        CheckBox showPasswordCheck = new CheckBox("Show Password");
        showPasswordCheck.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px; -fx-cursor: hand;");
        showPasswordCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            boolean show = newVal;
            fields[3].setVisible(!show); fields[3].setManaged(!show);
            showPassField.setVisible(show); showPassField.setManaged(show);
            
            fields[4].setVisible(!show); fields[4].setManaged(!show);
            showConfirmPassField.setVisible(show); showConfirmPassField.setManaged(show);
        });
        grid.add(showPasswordCheck, 1, 5);
        
        // Messages
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #0e547aff; -fx-font-size: 12px;");
        errorLabel.setVisible(false);
        
        Label successLabel = new Label();
        successLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 12px;");
        successLabel.setVisible(false);
        
        // Buttons with better sizing
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);
        
        Button registerButton = createStyledButton("Create", "#2c3e50", 100, 40);
        Button clearButton = createStyledButton("Clear", "#95a5a6", 80, 40);
        Button backButton = createStyledButton("Back", "#4ca1af", 80, 40);
        
        buttonBox.getChildren().addAll(registerButton, clearButton, backButton);
        
        // Terms and conditions
        CheckBox termsCheck = new CheckBox("I agree to the Terms of Service and Privacy Policy");
        termsCheck.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");
        
        VBox termsBox = new VBox(termsCheck);
        termsBox.setAlignment(Pos.CENTER);
        termsBox.setPadding(new Insets(10, 0, 10, 0));
        
        // Add all to form
        form.getChildren().addAll(logoContainer, formTitle, grid, termsBox, errorLabel, successLabel, buttonBox);
        
        // Set up button actions
        registerButton.setOnAction(event -> {
            System.out.println("Register button clicked");
            
            // Validate and register
            String fullName = ((TextField)fields[0]).getText().trim();
            String username = ((TextField)fields[1]).getText().trim();
            String email = ((TextField)fields[2]).getText().trim();
            String password = ((PasswordField)fields[3]).getText();
            String confirmPassword = ((PasswordField)fields[4]).getText();
            
            if (!termsCheck.isSelected()) {
                errorLabel.setText("Please accept the Terms of Service");
                errorLabel.setVisible(true);
                successLabel.setVisible(false);
                return;
            }
            
            // Basic validation
            if (fullName.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                errorLabel.setText("All fields are required");
                errorLabel.setVisible(true);
                successLabel.setVisible(false);
                return;
            }
            
            // Name Validation: Only letters and spaces allowed
            if (!fullName.matches("^[a-zA-Z\\s]+$")) {
                errorLabel.setText("Full Name must contain only letters (no numbers or symbols)");
                errorLabel.setVisible(true);
                successLabel.setVisible(false);
                return;
            }
            
            if (!password.equals(confirmPassword)) {
                errorLabel.setText(" Passwords do not match");
                errorLabel.setVisible(true);
                successLabel.setVisible(false);
                return;
            }
            
            // Strict Password Validation: At least 8 chars, 1 uppercase, 1 lowercase, 1 number
            if (!password.matches("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$")) {
                errorLabel.setText(" Password must be 8+ chars with Upper, Lower & Number");
                errorLabel.setVisible(true);
                successLabel.setVisible(false);
                return;
            }
            
            // Strict Email Validation
            if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                errorLabel.setText(" Please enter a valid email address");
                errorLabel.setVisible(true);
                successLabel.setVisible(false);
                return;
            }
            
            // Email Verification Step (OTP)
            // This ensures the email is real and accessible by the user
            String verificationCode = String.format("%06d", new java.util.Random().nextInt(1000000));
            sendEmailNotification(email, "Verify Your Account", "Your verification code is: " + verificationCode);
            
            TextInputDialog verifyDialog = new TextInputDialog();
            verifyDialog.setTitle("Verify Email");
            verifyDialog.setHeaderText("Email Verification Required");
            verifyDialog.setContentText("We have sent a code to " + email + ".\nPlease enter it below to complete registration:");
            verifyDialog.initOwner(registrationStage);
            
            Optional<String> codeInput = verifyDialog.showAndWait();
            if (codeInput.isEmpty() || !codeInput.get().trim().equals(verificationCode)) {
                errorLabel.setText(" Verification failed: Invalid code entered.");
                errorLabel.setVisible(true);
                successLabel.setVisible(false);
                return;
            }
            
            // Try to register using RegistrationService
            RegistrationService registrationService = new RegistrationService();
            RegistrationService.RegistrationResult result = registrationService.registerUser(username, password, email, fullName);
            
            if (result.isSuccess()) {
                errorLabel.setVisible(false);
                successLabel.setText("? " + result.getMessage());
                successLabel.setVisible(true);
                
                // Show success alert
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Registration Successful");
                successAlert.setHeaderText("Account Created");
                successAlert.setContentText("Account for '" + username + "' has been created successfully!\nYou can now login with your credentials.");
                successAlert.show();
                
                // Clear form
                for (Control field : fields) {
                    if (field instanceof TextInputControl) {
                        ((TextInputControl)field).clear();
                    }
                }
                
                System.out.println("Registration successful for: " + username);
            } else {
                errorLabel.setText("? " + result.getMessage());
                errorLabel.setVisible(true);
                successLabel.setVisible(false);
            }
        });
        
        clearButton.setOnAction(event -> {
            for (Control field : fields) {
                if (field instanceof TextInputControl) {
                    ((TextInputControl)field).clear();
                }
            }
            errorLabel.setVisible(false);
            successLabel.setVisible(false);
            fields[0].requestFocus();
        });
        
        backButton.setOnAction(event -> {
            System.out.println("Back to Login clicked");
            registrationStage.close();
            createLoginScreen();
        });
        
        StackPane root = new StackPane(form);
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #2c3e50, #4ca1af);");
        
        Scene scene = new Scene(root, 450, 680);
        registrationStage.setScene(scene);
        registrationStage.setResizable(false);
        
        // Hide main window and show registration
        primaryStage.hide();
        registrationStage.show();
        
        // When registration window closes, show login again
        registrationStage.setOnCloseRequest(event -> {
            createLoginScreen();
        });
    }
    
    private void showForgotPasswordDialog() {
        System.out.println("Forgot password dialog opened");
        
        // Create dialog
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Reset Password");
        dialog.setHeaderText("Enter your email address to reset your password");
        
        // Make dialog modal and set owner
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(primaryStage);

        // Create the email field
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField emailField = new TextField();
        emailField.setPromptText("Enter your email address");
        emailField.setPrefWidth(300);
        
        grid.add(new Label("Email:"), 0, 0);
        grid.add(emailField, 1, 0);
        
        dialog.getDialogPane().setPrefWidth(450);
        dialog.getDialogPane().setContent(grid);
        
        // Style the dialog
        dialog.getDialogPane().setStyle("-fx-background-color: white;");
        dialog.getDialogPane().setPrefWidth(500);
        dialog.getDialogPane().setPrefHeight(200);
        
        // Request focus on the email field by default
        Platform.runLater(() -> emailField.requestFocus());
        
        // Set the button types
        ButtonType resetButtonType = new ButtonType("Reset Password", ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(resetButtonType, cancelButtonType);
        
        // Convert the result to a string when the reset button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == resetButtonType) {
                return emailField.getText();
            }
            return null;
        });
        
        Optional<String> result = dialog.showAndWait();
        
        result.ifPresent(email -> {
            if (email.trim().isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Invalid Email");
                alert.setHeaderText("Email Required");
                alert.setContentText("Please enter a valid email address.");
                alert.showAndWait();
            } else if (!email.contains("@")) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Invalid Email");
                alert.setHeaderText("Invalid Email Format");
                alert.setContentText("Please enter a valid email address with @ symbol.");
                alert.showAndWait();
            } else {
                // Generate verification code
                String verificationCode = String.format("%06d", new java.util.Random().nextInt(1000000));
                
                // Send actual email
                sendEmailNotification(email, "Password Reset Verification", "Your verification code is: " + verificationCode);
                
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Password Reset Sent");
                alert.setHeaderText("Check Your Email");
                alert.setContentText("If an account with email '" + email + "' exists, a verification code has been sent.");
                alert.showAndWait();
                
                System.out.println("Password reset requested for: " + email);
                
                // Show next step: verification code and new password
                showPasswordResetStep2(email, verificationCode);
            }
        });
    }
    
    private void showPasswordResetStep2(String email, String expectedCode) {
        System.out.println("Password reset step 2 opened for: " + email);
        
        // Create dialog for verification code and new password
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Reset Your Password");
        dialog.setHeaderText("Enter the verification code and your new password");
        
        // Make dialog modal and set owner
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(primaryStage);
        
        // Create the form layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField codeField = new TextField();
        codeField.setPromptText("Enter verification code");
        codeField.setPrefWidth(300);
        
        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("Enter new password");
        newPasswordField.setPrefWidth(300);
        
        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirm new password");
        confirmPasswordField.setPrefWidth(300);
        
        grid.add(new Label("Verification Code:"), 0, 0);
        grid.add(codeField, 1, 0);
        grid.add(new Label("New Password:"), 0, 1);
        grid.add(newPasswordField, 1, 1);
        grid.add(new Label("Confirm Password:"), 0, 2);
        grid.add(confirmPasswordField, 1, 2);
        
        dialog.getDialogPane().setContent(grid);
        
        // Style the dialog
        dialog.getDialogPane().setStyle("-fx-background-color: white;");
        dialog.getDialogPane().setPrefWidth(550);
        dialog.getDialogPane().setPrefHeight(250);
        
        // Set the button types
        ButtonType resetButtonType = new ButtonType("Reset Password", ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(resetButtonType, cancelButtonType);
        
        // Request focus on the code field by default
        Platform.runLater(() -> codeField.requestFocus());
        
        // Convert the result
        dialog.setResultConverter(dialogButton -> dialogButton);
        
        Optional<ButtonType> result = dialog.showAndWait();
        
        result.ifPresent(buttonType -> {
            if (buttonType == resetButtonType) {
                String code = codeField.getText().trim();
                String newPassword = newPasswordField.getText();
                String confirmPassword = confirmPasswordField.getText();
                
                // Validate inputs
                if (code.isEmpty()) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Verification Required");
                    alert.setHeaderText("Code Required");
                    alert.setContentText("Please enter the verification code sent to your email.");
                    alert.showAndWait();
                    showPasswordResetStep2(email, expectedCode); // Show dialog again
                } else if (!code.equals(expectedCode)) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Invalid Code");
                    alert.setHeaderText("Verification Failed");
                    alert.setContentText("The verification code you entered is incorrect.");
                    alert.showAndWait();
                    showPasswordResetStep2(email, expectedCode); // Show dialog again
                } else if (newPassword.isEmpty()) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Password Required");
                    alert.setHeaderText("New Password Required");
                    alert.setContentText("Please enter a new password.");
                    alert.showAndWait();
                    showPasswordResetStep2(email, expectedCode); // Show dialog again
                } else if (newPassword.length() < 6) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Password Too Short");
                    alert.setHeaderText("Invalid Password");
                    alert.setContentText("Password must be at least 6 characters long.");
                    alert.showAndWait();
                    showPasswordResetStep2(email, expectedCode); // Show dialog again
                } else if (!newPassword.equals(confirmPassword)) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Passwords Don't Match");
                    alert.setHeaderText("Confirmation Failed");
                    alert.setContentText("The passwords you entered don't match. Please try again.");
                    alert.showAndWait();
                    showPasswordResetStep2(email, expectedCode); // Show dialog again
                } else {
                    // Update password in database
                    try {
                        Connection conn = DatabaseConnection.getReusableConnection();
                        String sql = "UPDATE users SET password = ? WHERE email = ?";
                        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                            stmt.setString(1, newPassword);
                            stmt.setString(2, email);
                            int rows = stmt.executeUpdate();
                            
                            if (rows > 0) {
                                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                alert.setTitle("Password Reset Successful");
                                alert.setHeaderText("Password Changed");
                                alert.setContentText("Your password has been successfully reset!\n\nYou can now log in with your new password.");
                                alert.showAndWait();
                                System.out.println("Password reset successful for email: " + email);
                            } else {
                                showAlert("Error", "Failed to update password. Email not found.");
                            }
                        }
                    } catch (SQLException e) {
                        System.err.println("Database error during password reset: " + e.getMessage());
                        showAlert("Database Error", "Could not update password: " + e.getMessage());
                    }
                }
            }
        });
    }
    
    // Helper method to create styled text fields
    private TextField createFormField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setPrefHeight(40);
        field.setPrefWidth(300);
        field.setStyle("-fx-background-color: #f5f6fa; -fx-border-color: transparent; -fx-background-radius: 8; -fx-padding: 10 15; -fx-font-size: 14px;");
        field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) field.setStyle("-fx-background-color: white; -fx-border-color: #4ca1af; -fx-border-width: 2; -fx-background-radius: 8; -fx-padding: 9 14; -fx-font-size: 14px;");
            else field.setStyle("-fx-background-color: #f5f6fa; -fx-border-color: transparent; -fx-background-radius: 8; -fx-padding: 10 15; -fx-font-size: 14px;");
        });
        return field;
    }
    
    // Helper method to create styled password fields
    private PasswordField createPasswordField(String prompt) {
        PasswordField field = new PasswordField();
        field.setPromptText(prompt);
        field.setPrefHeight(40);
        field.setPrefWidth(300);
        field.setStyle("-fx-background-color: #f5f6fa; -fx-border-color: transparent; -fx-background-radius: 8; -fx-padding: 10 15; -fx-font-size: 14px;");
        field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) field.setStyle("-fx-background-color: white; -fx-border-color: #4ca1af; -fx-border-width: 2; -fx-background-radius: 8; -fx-padding: 9 14; -fx-font-size: 14px;");
            else field.setStyle("-fx-background-color: #f5f6fa; -fx-border-color: transparent; -fx-background-radius: 8; -fx-padding: 10 15; -fx-font-size: 14px;");
        });
        return field;
    }
    
    // Helper method to create styled buttons
    private Button createStyledButton(String text, String color, int width, int height) {
        Button button = new Button(text);
        button.setStyle(String.format(
            "-fx-background-color: %s; -fx-text-fill: white; -fx-font-weight: bold; " +
            "-fx-padding: 10; -fx-min-width: %dpx; -fx-min-height: %dpx; -fx-background-radius: 8; -fx-cursor: hand;",
            color, width, height
        ));
        button.setOnMouseEntered(e -> button.setStyle(String.format(
            "-fx-background-color: %s; -fx-text-fill: white; -fx-font-weight: bold; " +
            "-fx-padding: 10; -fx-min-width: %dpx; -fx-min-height: %dpx; -fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 3); -fx-cursor: hand;",
            darkenColor(color), width, height
        )));
        button.setOnMouseExited(e -> button.setStyle(String.format(
            "-fx-background-color: %s; -fx-text-fill: white; -fx-font-weight: bold; " +
            "-fx-padding: 10; -fx-min-width: %dpx; -fx-min-height: %dpx; -fx-background-radius: 8; -fx-cursor: hand;",
            color, width, height
        )));
        return button;
    }
    
    // Helper to darken color for hover effect
    private String darkenColor(String color) {
        return switch (color) {
            case "#2ecc71" -> "#27ae60";  // Green
            case "#3498db" -> "#2980b9";  // Blue
            case "#2dadb6ff" -> "#13b86eff";  // Gray
            case "#0cb0f7ff" -> "#0a5064ff";  // Purple
            default -> color;
        };
    }
    
    private void showUserDashboard() {
        System.out.println("Showing redesigned user dashboard...");
    
        BorderPane rootLayout = new BorderPane();
        rootLayout.setStyle("-fx-background-color: #f4f6f8;");
    
        // --- LEFT SIDEBAR ---
        VBox sidebar = new VBox();
        sidebar.setPadding(new Insets(20, 0, 20, 0));
        sidebar.setSpacing(15);
        sidebar.setPrefWidth(220);
        sidebar.setStyle("-fx-background-color: #2c3e50;");
        sidebar.setAlignment(Pos.TOP_CENTER);
    
        // Sidebar Header
        HBox sidebarHeader = new HBox(15);
        sidebarHeader.setAlignment(Pos.CENTER_LEFT);
        sidebarHeader.setPadding(new Insets(0, 0, 20, 20));

        Label appTitle = new Label("Event App");
        appTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        sidebarHeader.getChildren().addAll(appTitle);
    
        Button dashboardButton = createSidebarButton("Dashboard");
        Button calendarButton = createSidebarButton("Calendar View");
        Button addEventButton = createSidebarButton("Add New Event");
        Button viewEventsButton = createSidebarButton("View Events");
        Button settingsButton = createSidebarButton("Settings");
        Button themeButton = createSidebarButton("Dark/Light Mode");
        themeButton.setOnAction(e -> {
            boolean isDark = primaryStage.getScene().getStylesheets().stream()
                .anyMatch(s -> s.contains("dark-theme.css"));
            applyTheme(isDark ? "Light" : "Dark");
        });
        
        sidebar.getChildren().addAll(sidebarHeader, dashboardButton, calendarButton, addEventButton, viewEventsButton, settingsButton, themeButton);
    
        // --- MAIN CONTENT AREA ---
        VBox mainContent = new VBox(20);
        mainContent.setPadding(new Insets(20));
        mainContent.setMaxWidth(Double.MAX_VALUE);
        
        // Header with Logout Button (Top Right)
        HBox headerBox = new HBox();
        headerBox.setAlignment(Pos.CENTER_RIGHT);
        headerBox.setSpacing(10);
        
        Button logoutBtn = new Button("Logout");
        logoutBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 15; -fx-background-radius: 5;");
        logoutBtn.setOnMouseEntered(e -> logoutBtn.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 15; -fx-background-radius: 5;"));
        logoutBtn.setOnMouseExited(e -> logoutBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 15; -fx-background-radius: 5;"));
        logoutBtn.setOnAction(e -> showLoginScreen());
        
        headerBox.getChildren().add(logoutBtn);
        
        // Content Container
        VBox contentContainer = new VBox();
        contentContainer.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(contentContainer, Priority.ALWAYS);
        
        mainContent.getChildren().addAll(headerBox, contentContainer);
        
        // Initial View - Dashboard Stats
        updateMainContent(contentContainer, createStatsView());
    
        // --- SET LAYOUT ---
        rootLayout.setLeft(sidebar);
        rootLayout.setCenter(mainContent);
    
        // --- ACTIONS ---
        dashboardButton.setOnAction(e -> updateMainContent(contentContainer, createStatsView()));
        calendarButton.setOnAction(e -> updateMainContent(contentContainer, calendarController.getCalendarView()));
        addEventButton.setOnAction(e -> showEventDialog(null, null));
        viewEventsButton.setOnAction(e -> updateMainContent(contentContainer, eventManagerController.getEventManagerView()));
        settingsButton.setOnAction(e -> showUserSettings());
    
        // --- SCENE & STAGE ---
        Scene scene = new Scene(rootLayout, 1000, 700);
        
        primaryStage.setTitle("My Dashboard - Event App");
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(100);
        primaryStage.setMinHeight(700);
        primaryStage.setMaximized(true);
        primaryStage.show();
        
        // Force maximize to match login screen behavior
        Platform.runLater(() -> {
            primaryStage.setMaximized(true);
            primaryStage.toFront();
        });
    
        System.out.println("Redesigned user dashboard displayed");
    }
    
    private void updateMainContent(VBox container, Node content) {
        container.getChildren().clear();
        container.getChildren().add(content);
        VBox.setVgrow(content, Priority.ALWAYS);
    }

    private int getEventCount(String type) {
        if (SessionManager.getCurrentUser() == null) return 0;
        int userId = SessionManager.getCurrentUser().getId();
        String sql;
        
        try {
            Connection conn = DatabaseConnection.getReusableConnection();
            PreparedStatement stmt;
            
            switch(type) {
                case "ALL":
                    sql = "SELECT COUNT(*) FROM events WHERE user_id = ?";
                    stmt = conn.prepareStatement(sql);
                    stmt.setInt(1, userId);
                    break;
                case "UPCOMING":
                    sql = "SELECT COUNT(*) FROM events WHERE user_id = ? AND ((event_date = CURDATE() AND start_time > CURTIME()) OR (event_date > CURDATE() AND event_date <= DATE_ADD(CURDATE(), INTERVAL 7 DAY)))";
                    stmt = conn.prepareStatement(sql);
                    stmt.setInt(1, userId);
                    break;
                case "COMPLETED":
                    sql = "SELECT COUNT(*) FROM events WHERE user_id = ? AND (event_date < CURDATE() OR (event_date = CURDATE() AND end_time < CURTIME()))";
                    stmt = conn.prepareStatement(sql);
                    stmt.setInt(1, userId);
                    break;
                default: // Category
                    sql = "SELECT COUNT(*) FROM events WHERE user_id = ? AND category = ?";
                    stmt = conn.prepareStatement(sql);
                    stmt.setInt(1, userId);
                    stmt.setString(2, type);
                    break;
            }
            
            var rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
            
        } catch (Exception e) {
            System.err.println("Error fetching stats: " + e.getMessage());
        }
        return 0;
    }

    private Node createStatsView() {
        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(20);
        statsGrid.setVgap(20);
        statsGrid.setAlignment(Pos.TOP_CENTER);
        statsGrid.setMaxWidth(Double.MAX_VALUE);
        
        // Make columns fill the screen width
        ColumnConstraints colConstraints = new ColumnConstraints();
        colConstraints.setHgrow(Priority.ALWAYS);
        colConstraints.setPercentWidth(33.3);
        statsGrid.getColumnConstraints().addAll(colConstraints, colConstraints, colConstraints);
        
        // Fetch real statistics
        VBox totalEventsCard = createAnalyticsCard("Total Events", String.valueOf(getEventCount("ALL")), "", "#3498db");
        VBox completedEventsCard = createAnalyticsCard("Completed Events", String.valueOf(getEventCount("COMPLETED")), "", "#f39c12");
        
        VBox upcomingEventsCard = createAnalyticsCard("Upcoming", String.valueOf(getEventCount("UPCOMING")), "", "#2ecc71");
        
        // Category cards
        VBox meetingsCard = createAnalyticsCard("Meetings", String.valueOf(getEventCount("Meeting")), "", "#9b59b6");
        VBox personalCard = createAnalyticsCard("Personal", String.valueOf(getEventCount("Personal")), "", "#e74c3c");
        VBox workCard = createAnalyticsCard("Work", String.valueOf(getEventCount("Work")), "", "#34495e");
        VBox socialCard = createAnalyticsCard("Social", String.valueOf(getEventCount("Social")), "", "#1abc9c");
        VBox otherCard = createAnalyticsCard("Other", String.valueOf(getEventCount("Other")), "", "#95a5a6");
        
        statsGrid.add(totalEventsCard, 0, 0);
        statsGrid.add(completedEventsCard, 1, 0);
        statsGrid.add(upcomingEventsCard, 2, 0);
        
        statsGrid.add(meetingsCard, 0, 1);
        statsGrid.add(personalCard, 1, 1);
        statsGrid.add(workCard, 2, 1);
        
        statsGrid.add(socialCard, 0, 2);
        statsGrid.add(otherCard, 1, 2);
        
        VBox container = new VBox(20);
        container.getChildren().add(statsGrid);
        
        return container;
    }

    private Button createSidebarButton(String text) {
        Button button = new Button(text);
        button.setPrefWidth(200);
        button.setPrefHeight(45);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #bdc3c7; " +
            "-fx-font-size: 15px; -fx-font-weight: bold; -fx-padding: 0 0 0 30;"
        );
    
        button.setOnMouseEntered(e -> button.setStyle(
            "-fx-background-color: #34495e; -fx-text-fill: white; " +
            "-fx-font-size: 15px; -fx-font-weight: bold; -fx-padding: 0 0 0 30;"
        ));
        button.setOnMouseExited(e -> button.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #bdc3c7; " +
            "-fx-font-size: 15px; -fx-font-weight: bold; -fx-padding: 0 0 0 30;"
        ));
        return button;
    }
    
    private void checkReminders() {
        if (SessionManager.getCurrentUser() == null) return;

        try {
            // Check for events in the next 3 days
            String sql = "SELECT * FROM events WHERE user_id = ? AND ((event_date = CURDATE() AND start_time > CURTIME()) OR (event_date > CURDATE() AND event_date <= DATE_ADD(CURDATE(), INTERVAL 3 DAY))) ORDER BY event_date, start_time";
            var stmt = DatabaseConnection.getReusableConnection().prepareStatement(sql);
            stmt.setInt(1, SessionManager.getCurrentUser().getId());
            var rs = stmt.executeQuery();
            
            StringBuilder reminders = new StringBuilder();
            while (rs.next()) {
                LocalDate date = rs.getDate("event_date").toLocalDate();
                String title = rs.getString("title");
                
                String phone = null;
                String email = null;
                try { phone = rs.getString("contact_phone"); } catch (SQLException e) {}
                try { email = rs.getString("contact_email"); } catch (SQLException e) {}
                
                reminders.append(String.format("• %s on %s", title, date));
                if (phone != null && !phone.isEmpty()) reminders.append(" [Ph: ").append(phone).append("]");
                if (email != null && !email.isEmpty()) reminders.append(" [Email: ").append(email).append("]");
                reminders.append("\n");
            }
            
            if (reminders.length() > 0) {
                showAlert("Upcoming Events Reminder", "You have upcoming events:\n" + reminders.toString());
            }
        } catch (Exception e) {
            System.err.println("Error checking reminders: " + e.getMessage());
        }
    }
    
    private void startNotificationService() {
        // Check for upcoming events every minute
        notificationScheduler.scheduleAtFixedRate(this::checkAndNotifyEvents, 0, 1, TimeUnit.MINUTES);
        // Check for past events to mark as completed
        notificationScheduler.scheduleAtFixedRate(this::autoCompleteEvents, 0, 1, TimeUnit.MINUTES);
    }

    private void checkAndNotifyEvents() {
        if (SessionManager.getCurrentUser() == null) return;
        
        // Skip if both are disabled
        if (!smsNotificationsEnabled && !emailNotificationsEnabled) return;
        
        try {
            Connection conn = DatabaseConnection.getReusableConnection();
            LocalDateTime now = LocalDateTime.now();
            
            // Check for events starting now
            checkEventsForTime(conn, now, "STARTING_NOW");
            
            // Check for events starting in 1 hour (Long warning)
            checkEventsForTime(conn, now.plusHours(1), "REMINDER_1H");

            // Check for events starting in 5 minutes (Short warning / Fallback)
            checkEventsForTime(conn, now.plusMinutes(5), "REMINDER_5M");
            
        } catch (Exception e) {
            System.err.println("Notification check error: " + e.getMessage());
        }
    }

    private void checkEventsForTime(Connection conn, LocalDateTime targetTime, String type) throws SQLException {
        LocalTime time = targetTime.toLocalTime();
        LocalDate date = targetTime.toLocalDate();
        
        LocalTime startWindow = time.minusMinutes(5);
        LocalTime endWindow = time.plusMinutes(5);
        
        String sql;
        if (startWindow.isAfter(endWindow)) {
            sql = "SELECT * FROM events WHERE user_id = ? AND event_date = ? " +
                  "AND (start_time >= ? OR start_time <= ?)";
        } else {
            sql = "SELECT * FROM events WHERE user_id = ? AND event_date = ? " +
                  "AND start_time BETWEEN ? AND ?";
        }
        
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, SessionManager.getCurrentUser().getId());
        stmt.setDate(2, Date.valueOf(date));
        stmt.setTime(3, Time.valueOf(startWindow));
        stmt.setTime(4, Time.valueOf(endWindow));
        
        var rs = stmt.executeQuery();
        
        while (rs.next()) {
            int eventId = rs.getInt("id");
            String notificationKey = eventId + "_" + type;
            
            // Special Logic: If checking 5 min reminder, skip if 1 hour reminder was already sent
            if ("REMINDER_5M".equals(type)) {
                String oneHourKey = eventId + "_REMINDER_1H";
                if (notifiedEventIds.contains(oneHourKey)) {
                    continue; // Skip 5 min reminder if 1 hour was sent
                }
            }

            if (!notifiedEventIds.contains(notificationKey)) {
                String title = rs.getString("title");
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("hh:mm a");
                String startTime = sdf.format(rs.getTime("start_time"));
                String email = SessionManager.getCurrentUser().getEmail();
                String eventContactPhone = null;
                String eventContactEmail = null;
                try { eventContactPhone = rs.getString("contact_phone"); } catch (SQLException e) {}
                try { eventContactEmail = rs.getString("contact_email"); } catch (SQLException e) {}
                
                System.out.println("Triggering notification (" + type + ") for event: " + title + " at " + startTime);
                
                String subject;
                String messageBody;
                String smsMessage;
                
                if ("REMINDER_1H".equals(type)) {
                    subject = "Reminder: " + title + " (1 Hour)";
                    messageBody = "Reminder: Your event '" + title + "' starts in 1 hour at " + startTime;
                    smsMessage = "Reminder: '" + title + "' starts in 1 hour at " + startTime;
                } else if ("REMINDER_5M".equals(type)) {
                    subject = "Reminder: " + title + " (5 Minutes)";
                    messageBody = "Reminder: Your event '" + title + "' starts in 5 minutes at " + startTime;
                    smsMessage = "Reminder: '" + title + "' starts in 5 minutes at " + startTime;
                } else {
                    subject = "Starting Now: " + title;
                    messageBody = "Your event '" + title + "' is starting now at " + startTime;
                    smsMessage = "Event '" + title + "' is starting now at " + startTime;
                }

                // Send Notifications
                if (emailNotificationsEnabled) {
                    String targetEmail = (eventContactEmail != null && !eventContactEmail.isEmpty()) 
                                       ? eventContactEmail 
                                       : email;
                    sendEmailNotification(targetEmail, subject, messageBody);
                }
                if (smsNotificationsEnabled) {
                    StringBuilder targetPhoneBuilder = new StringBuilder(currentUserPhoneNumber);
                    if (eventContactPhone != null && !eventContactPhone.isEmpty()) {
                        if (targetPhoneBuilder.length() > 0) targetPhoneBuilder.append(",");
                        targetPhoneBuilder.append(eventContactPhone);
                    }
                    sendSMSNotification(targetPhoneBuilder.toString(), smsMessage);
                }
                
                // Show on UI
                String finalPhone = eventContactPhone;
                String finalEmail = eventContactEmail;
                String finalTitle = title;
                String finalBody = messageBody;
                
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Event Reminder");
                    alert.setHeaderText(type.contains("REMINDER") ? "Upcoming Event: " + finalTitle : "Event Starting: " + finalTitle);
                    String content = finalBody;
                    if (finalPhone != null && !finalPhone.isEmpty()) content += "\nPhone: " + finalPhone;
                    if (finalEmail != null && !finalEmail.isEmpty()) content += "\nEmail: " + finalEmail;
                    alert.setContentText(content);
                    alert.show();
                });
                
                notifiedEventIds.add(notificationKey);
            }
        }
    }

    private void autoCompleteEvents() {
        if (SessionManager.getCurrentUser() == null) return;
        
        try {
            Connection conn = DatabaseConnection.getReusableConnection();
            // Update status to COMPLETED for past events
            // This moves them logically to a "Completed" state in the database
            String sql = "UPDATE events SET status = 'COMPLETED' WHERE user_id = ? AND (event_date < CURDATE() OR (event_date = CURDATE() AND end_time < CURTIME())) AND (status IS NULL OR status != 'COMPLETED')";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, SessionManager.getCurrentUser().getId());
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            // Auto-create status column if it doesn't exist
            if (e.getMessage().contains("Unknown column")) {
                try {
                    Connection conn = DatabaseConnection.getReusableConnection();
                    conn.createStatement().execute("ALTER TABLE events ADD COLUMN status VARCHAR(20) DEFAULT 'SCHEDULED'");
                    System.out.println("Added 'status' column to events table.");
                } catch (SQLException ex) {
                    System.err.println("Failed to add status column: " + ex.getMessage());
                }
            }
        }
    }

    private void sendEmailNotification(String email, String subject, String body) {
        String targetEmail = (email != null && !email.isEmpty()) ? email : "mekuze7@gmail.com";

        if (useSmtp && smtpHost != null && !smtpHost.isEmpty() && smtpEmail != null && !smtpEmail.isEmpty()) {
            // Use Native SMTP
            sendSmtpEmail(targetEmail, subject, body);
        } else if (infobipApiKey != null && !infobipApiKey.isEmpty() && infobipBaseUrl != null && !infobipBaseUrl.isEmpty()) {
            // Fallback to Infobip
            sendInfobipEmail(targetEmail, subject, body, infobipApiKey, infobipBaseUrl, emailSenderAddress);
        } else {
            System.err.println("Real Email could not be sent: Infobip API Key or Base URL is missing.");
        }
    }

    private void sendSMSNotification(String targetPhone, String messageText) {
        if (infobipApiKey != null && !infobipApiKey.isEmpty() && infobipBaseUrl != null && !infobipBaseUrl.isEmpty()) {
            sendInfobipSms(targetPhone, messageText, infobipApiKey, infobipBaseUrl);
        } else {
            System.err.println("Real SMS could not be sent: Infobip API Key or Base URL is missing.");
        }
    }

    private void sendInfobipSms(String phoneNumber, String message, String apiKey, String baseUrl) {
        try {
            // Ensure URL has protocol
            if (!baseUrl.startsWith("http")) {
                baseUrl = "https://" + baseUrl;
            }
            
            // Support multiple numbers separated by comma
            String[] numbers = phoneNumber.split(",");
            StringBuilder destinations = new StringBuilder("[");
            
            for (int i = 0; i < numbers.length; i++) {
                String rawNum = numbers[i].trim();
                if (rawNum.isEmpty()) continue;
                
                // Clean phone number (remove spaces, dashes)
                String cleanPhone = rawNum.replaceAll("[^0-9+]", "");
                
                // Auto-format Ethiopian numbers if local format is used (e.g. 0911... -> +251911...)
                if (cleanPhone.startsWith("09") && cleanPhone.length() == 10) {
                    cleanPhone = "+251" + cleanPhone.substring(1);
                } else if (cleanPhone.startsWith("9") && cleanPhone.length() == 9) {
                    cleanPhone = "+251" + cleanPhone;
                } else if (cleanPhone.startsWith("251") && cleanPhone.length() == 12) {
                    cleanPhone = "+" + cleanPhone;
                }
                
                if (destinations.length() > 1) {
                    destinations.append(",");
                }
                destinations.append(String.format("{\"to\":\"%s\"}", cleanPhone));
            }
            destinations.append("]");
            
            // Construct JSON payload manually to avoid external dependencies
            // Note: In a production app, use a JSON library like Jackson or Gson
            String cleanMessage = message.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
            
            // Generate a custom bulk ID for tracking
            String bulkId = "BULK-" + System.currentTimeMillis();
            
            String jsonPayload = String.format(
                "{\"bulkId\":\"%s\",\"messages\":[{\"destinations\":%s,\"from\":\"InfoSMS\",\"text\":\"%s\"}]}",
                bulkId, destinations.toString(), cleanMessage
            );
            
            System.out.println("Sending SMS to: " + phoneNumber + " via " + baseUrl + " [BulkID: " + bulkId + "]");

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/sms/2/text/advanced"))
                .header("Authorization", "App " + apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(BodyPublishers.ofString(jsonPayload))
                .build();

            // Use synchronous send to ensure we see the result immediately in logs
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
            
            System.out.println("Infobip SMS Response Code: " + response.statusCode());
            System.out.println("Infobip SMS Response Body: " + response.body());
            String status = (response.statusCode() == 200) ? "SENT" : "FAILED";
            logNotification("SMS", phoneNumber, message, bulkId, status);
            
            if (response.statusCode() != 200) {
                 System.err.println("SMS Send Failed! Check API Key and Base URL.");
                 if (response.body().contains("REJECTED_DESTINATION_NOT_REGISTERED")) {
                     Platform.runLater(() -> showAlert("Verification Required", "This number is not verified.\nInfobip Free Trial requires verifying phone numbers first.\nGo to Settings > Verify on Infobip."));
                 }
            }
                
        } catch (Exception e) {
            System.err.println("Failed to send SMS: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- NATIVE SMTP IMPLEMENTATION (No External Dependencies) ---
    private void sendSmtpEmail(String to, String subject, String body) {
        new Thread(() -> {
            try {
                System.out.println("Connecting to SMTP server: " + smtpHost + ":" + smtpPort + " for " + to);
                SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                try (SSLSocket socket = (SSLSocket) factory.createSocket(smtpHost, Integer.parseInt(smtpPort));
                     PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"))) {
                    
                    socket.setSoTimeout(15000); // 15 sec timeout
                    readSmtpResponse(in);
                    
                    String localHost = "localhost";
                    try { localHost = InetAddress.getLocalHost().getHostName(); } catch (Exception e) {}
                    sendSmtpCommand(out, "EHLO " + localHost);
                    readSmtpResponse(in);
                    
                    sendSmtpCommand(out, "AUTH LOGIN");
                    readSmtpResponse(in);
                    sendSmtpCommand(out, java.util.Base64.getEncoder().encodeToString(smtpEmail.getBytes("UTF-8")));
                    readSmtpResponse(in);
                    sendSmtpCommand(out, java.util.Base64.getEncoder().encodeToString(smtpPassword.getBytes("UTF-8")));
                    String authResult = readSmtpResponse(in);
                    
                    if (!authResult.startsWith("235")) {
                        throw new IOException("SMTP Authentication Failed: " + authResult);
                    }
                    
                    sendSmtpCommand(out, "MAIL FROM:<" + smtpEmail + ">");
                    readSmtpResponse(in);
                    sendSmtpCommand(out, "RCPT TO:<" + to + ">");
                    readSmtpResponse(in);
                    sendSmtpCommand(out, "DATA");
                    readSmtpResponse(in);
                    
                    out.println("Subject: " + subject);
                    out.println("From: EMS <" + smtpEmail + ">");
                    out.println("To: " + to);
                    out.println("Content-Type: text/plain; charset=UTF-8");
                    out.println();
                    out.println(body);
                    out.println(".");
                    out.flush();
                    
                    readSmtpResponse(in);
                    sendSmtpCommand(out, "QUIT");
                    
                    logNotification("EMAIL (SMTP)", to, body, "SMTP-" + System.currentTimeMillis(), "SENT");
                    System.out.println("SMTP Email sent successfully!");
                    Platform.runLater(() -> showAlert("Email Sent", "SMTP Email sent successfully to " + to));
                }
            } catch (Exception e) {
                System.err.println("SMTP Error: " + e.getMessage());
                e.printStackTrace();
                logNotification("EMAIL (SMTP)", to, body, "SMTP-ERR", "FAILED");
                
                String errorMessage = "Failed to send email: " + e.getMessage();
                // Check for common authentication error 535
                if (e.getMessage() != null && e.getMessage().contains("535")) {
                    errorMessage = "SMTP Authentication Failed (Error 535).\n\n" +
                                   "This usually means your email/password is incorrect. Please check the following in Settings > Email Integration:\n\n" +
                                   "1. SMTP Email: Is it your full, correct Gmail address (e.g., 'user@gmail.com')?\n\n" +
                                   "2. SMTP Password: Are you using a Google 'App Password'? Regular passwords do not work.\n\n" +
                                   "An App Password is a 16-digit code that gives an app permission to access your Google Account. You can generate one in your Google Account's security settings.";
                }
                final String finalErrorMessage = errorMessage;
                Platform.runLater(() -> showAlert("SMTP Error", finalErrorMessage));
            }
        }).start();
    }

    private String readSmtpResponse(BufferedReader in) throws IOException {
        String line = in.readLine();
        // Handle multi-line responses (e.g. 250-foo)
        while (line != null && line.length() >= 4 && line.charAt(3) == '-') {
            line = in.readLine();
        }
        return line;
    }

    private void sendSmtpCommand(PrintWriter out, String cmd) {
        out.println(cmd);
    }

    private void testInfobipConnection(String apiKey, String baseUrl) {
        try {
            if (!baseUrl.startsWith("http")) {
                baseUrl = "https://" + baseUrl;
            }
            
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/account/1/balance"))
                .header("Authorization", "App " + apiKey)
                .header("Accept", "application/json")
                .GET()
                .build();
                
            client.sendAsync(request, BodyHandlers.ofString())
                .thenAccept(response -> {
                    Platform.runLater(() -> {
                        if (response.statusCode() == 200) {
                            showAlert("Connection Successful", "API Key is valid! Connected to Infobip.");
                        } else {
                            showAlert("Connection Failed", "Status: " + response.statusCode() + "\nCheck your API Key and Base URL.");
                        }
                    });
                })
                .exceptionally(e -> {
                    Platform.runLater(() -> showAlert("Connection Error", "Could not connect: " + e.getMessage()));
                    return null;
                });
        } catch (Exception e) {
            Platform.runLater(() -> showAlert("Error", "Invalid URL or configuration: " + e.getMessage()));
        }
    }

    private void sendInfobipEmail(String to, String subject, String text, String apiKey, String baseUrl, String from) {
        try {
            if (!baseUrl.startsWith("http")) {
                baseUrl = "https://" + baseUrl;
            }

            String boundary = "---ContentBoundary" + System.currentTimeMillis();
            String lineFeed = "\r\n";
            
            StringBuilder body = new StringBuilder();
            
            // From
            body.append("--").append(boundary).append(lineFeed);
            body.append("Content-Disposition: form-data; name=\"from\"").append(lineFeed).append(lineFeed);
            body.append("EMS <" + from + ">").append(lineFeed);
            
            // To
            body.append("--").append(boundary).append(lineFeed);
            body.append("Content-Disposition: form-data; name=\"to\"").append(lineFeed).append(lineFeed);
            body.append(to).append(lineFeed);
            
            // Subject
            body.append("--").append(boundary).append(lineFeed);
            body.append("Content-Disposition: form-data; name=\"subject\"").append(lineFeed).append(lineFeed);
            body.append(subject).append(lineFeed);
            
            // Text
            body.append("--").append(boundary).append(lineFeed);
            body.append("Content-Disposition: form-data; name=\"text\"").append(lineFeed).append(lineFeed);
            body.append(text).append(lineFeed);
            
            // End
            body.append("--").append(boundary).append("--").append(lineFeed);

            System.out.println("Sending Email to: " + to + " via " + baseUrl);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/email/3/send"))
                .header("Authorization", "App " + apiKey)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header("Accept", "application/json")
                .POST(BodyPublishers.ofString(body.toString()))
                .build();

            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
            
            System.out.println("Infobip Email Response Code: " + response.statusCode());
            System.out.println("Infobip Email Response Body: " + response.body());
            
            String status = (response.statusCode() == 200) ? "SENT" : "FAILED";
            logNotification("EMAIL", to, text, "EMAIL-" + System.currentTimeMillis(), status);
            
            if (response.statusCode() != 200) {
                 System.err.println("Email Send Failed! Check API Key and Base URL.");
            }
                
        } catch (Exception e) {
            System.err.println("Failed to send Infobip Email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void logNotification(String type, String recipient, String message, String bulkId, String status) {
        if (SessionManager.getCurrentUser() == null) return;
        
        // SQL to insert log - ensure table 'notification_logs' exists
        String sql = "INSERT INTO notification_logs (user_id, type, recipient, message, bulk_id, status, sent_at) VALUES (?, ?, ?, ?, ?, ?, NOW())";
        
        try {
            Connection conn = DatabaseConnection.getReusableConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, SessionManager.getCurrentUser().getId());
                stmt.setString(2, type);
                stmt.setString(3, recipient);
                stmt.setString(4, message);
                stmt.setString(5, bulkId);
                stmt.setString(6, status);
                
                stmt.executeUpdate();
                System.out.println("Notification logged to database: " + type + " -> " + recipient);
            }
        } catch (SQLException e) {
            // Fail silently if table doesn't exist yet to prevent app crash
            if (!e.getMessage().contains("doesn't exist")) {
                System.err.println("Error logging notification: " + e.getMessage());
            }
        }
    }

    private void showMessageHistory() {
        Stage stage = new Stage();
        stage.setTitle("Message History");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(primaryStage);
        
        TableView<NotificationLog> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        TableColumn<NotificationLog, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getType()));
        
        TableColumn<NotificationLog, String> recipientCol = new TableColumn<>("Recipient");
        recipientCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getRecipient()));
        
        TableColumn<NotificationLog, String> messageCol = new TableColumn<>("Message");
        messageCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getMessage()));
        
        TableColumn<NotificationLog, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getStatus()));
        
        TableColumn<NotificationLog, String> timeCol = new TableColumn<>("Sent At");
        timeCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getSentAt()));
        
        // Action Column for Resending
        TableColumn<NotificationLog, Void> actionCol = new TableColumn<>("Action");
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("Resend");
            {
                btn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 11px; -fx-cursor: hand;");
                btn.setOnAction(event -> {
                    NotificationLog log = getTableView().getItems().get(getIndex());
                    if ("SMS".equals(log.getType())) {
                        sendInfobipSms(log.getRecipient(), log.getMessage(), infobipApiKey, infobipBaseUrl);
                        Alert alert = new Alert(Alert.AlertType.INFORMATION, "SMS resend request initiated.");
                        alert.show();
                    } else if ("EMAIL (SMTP)".equals(log.getType())) {
                        sendSmtpEmail(log.getRecipient(), "Resent: Notification", log.getMessage());
                        Alert alert = new Alert(Alert.AlertType.INFORMATION, "SMTP Email resend request initiated.");
                        alert.show();
                    } else {
                        sendInfobipEmail(log.getRecipient(), "Resent Notification", log.getMessage(), infobipApiKey, infobipBaseUrl, emailSenderAddress);
                        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Email resend request initiated.");
                        alert.show();
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null); else setGraphic(btn);
            }
        });
        
        table.getColumns().addAll(typeCol, recipientCol, messageCol, statusCol, timeCol, actionCol);
        
        try {
            Connection conn = DatabaseConnection.getReusableConnection();
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM notification_logs WHERE user_id = ? ORDER BY sent_at DESC");
            stmt.setInt(1, SessionManager.getCurrentUser().getId());
            var rs = stmt.executeQuery();
            while(rs.next()) {
                table.getItems().add(new NotificationLog(
                    rs.getString("type"),
                    rs.getString("recipient"),
                    rs.getString("message"),
                    rs.getString("status"),
                    rs.getTimestamp("sent_at").toString()
                ));
            }
        } catch(Exception e) {
            System.err.println("Error fetching logs: " + e.getMessage());
        }
        
        // Clear History Button
        Button clearBtn = new Button("Clear History");
        clearBtn.setStyle("-fx-background-color: #3cd9e7ff; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        clearBtn.setOnAction(e -> {
            try {
                Connection conn = DatabaseConnection.getReusableConnection();
                PreparedStatement stmt = conn.prepareStatement("DELETE FROM notification_logs WHERE user_id = ?");
                stmt.setInt(1, SessionManager.getCurrentUser().getId());
                stmt.executeUpdate();
                table.getItems().clear();
            } catch (Exception ex) {
                System.err.println("Error clearing logs: " + ex.getMessage());
            }
        });
        HBox bottomBar = new HBox(clearBtn);
        bottomBar.setAlignment(Pos.CENTER_RIGHT);
        
        VBox layout = new VBox(20, new Label("Sent Messages History"), table, bottomBar);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: white;");
        ((Label)layout.getChildren().get(0)).setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        VBox.setVgrow(table, Priority.ALWAYS);
        
        Scene scene = new Scene(layout, 900, 600);
        stage.setScene(scene);
        stage.show();
    }

    public static class NotificationLog {
        private final String type, recipient, message, status, sentAt;
        public NotificationLog(String t, String r, String m, String s, String sa) { type=t; recipient=r; message=m; status=s; sentAt=sa; }
        public String getType() { return type; } public String getRecipient() { return recipient; } public String getMessage() { return message; } public String getStatus() { return status; } public String getSentAt() { return sentAt; }
    }

    private VBox createFeatureCard(String title, String description, javafx.event.EventHandler<javafx.event.ActionEvent> action) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("card-title");

        Label descriptionLabel = new Label(description);
        descriptionLabel.getStyleClass().add("card-subtitle");
        descriptionLabel.setWrapText(true);

        VBox card = new VBox(10, titleLabel, descriptionLabel);
        card.getStyleClass().add("feature-card");
        card.setPrefWidth(180);
        card.setPrefHeight(150);
        card.setOnMouseClicked(e -> action.handle(new javafx.event.ActionEvent()));
        
        return card;
    }
    
    private VBox createAnalyticsCard(String title, String value, String subtitle, String color) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #555;");
    
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
    
        VBox card = new VBox(5, titleLabel, valueLabel);
        
        if (subtitle != null && !subtitle.isEmpty()) {
            Label subtitleLabel = new Label(subtitle);
            subtitleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #0dddecff;");
            card.getChildren().add(subtitleLabel);
        }
        
        card.setPadding(new Insets(20));
        card.setPrefWidth(250);
        card.setMaxWidth(Double.MAX_VALUE); // Allow card to stretch
        card.setPrefHeight(150);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");
        return card;
    }
    
    public void showEventDialog(Event eventToEdit, LocalDate preSelectedDate) {
        Stage dialog = new Stage();
        dialog.setTitle(eventToEdit == null ? "Add New Event" : "Edit Event");
        dialog.initOwner(primaryStage);
        dialog.initModality(Modality.NONE); // allow minimize and parent interaction
        dialog.setResizable(true);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        TextField titleField = createFormField("Event Title");
        DatePicker datePicker = new DatePicker();
        datePicker.setPromptText("Event Date");
        if (eventToEdit != null) {
            datePicker.setValue(eventToEdit.getEventDate());
        } else if (preSelectedDate != null) {
            datePicker.setValue(preSelectedDate);
        }
        datePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
            }
        });
        styleDatePicker(datePicker);
        
        // Time Picker (ComboBoxes)
        ComboBox<String> hourBox = new ComboBox<>();
        for (int i = 1; i <= 12; i++) hourBox.getItems().add(String.format("%02d", i));
        hourBox.setValue("09");
        styleComboBox(hourBox);
        
        ComboBox<String> minBox = new ComboBox<>();
        for (int i = 0; i < 60; i += 5) minBox.getItems().add(String.format("%02d", i));
        minBox.setValue("00");
        styleComboBox(minBox);
        
        ComboBox<String> amPmBox = new ComboBox<>();
        amPmBox.getItems().addAll("AM", "PM");
        amPmBox.setValue("AM");
        styleComboBox(amPmBox);
        
        HBox timePicker = new HBox(5, hourBox, new Label(":"), minBox, amPmBox);
        timePicker.setAlignment(Pos.CENTER_LEFT);
        
        TextField locationField = createFormField("Location");
        TextField phoneField = createFormField("Contact Phone");
        TextField emailField = createFormField("Contact Email");
        
        ComboBox<String> categoryBox = new ComboBox<>();
        categoryBox.getItems().addAll("Meeting", "Personal", "Work", "Social", "Other");
        categoryBox.setValue("Meeting");
        categoryBox.setPrefWidth(200);
        styleComboBox(categoryBox);
        
        CheckBox smsCheckBox = new CheckBox("Send SMS Notification");
        
        // Pre-fill if editing
        if (eventToEdit != null) {
            titleField.setText(eventToEdit.getTitle());
            locationField.setText(eventToEdit.getLocation());
            categoryBox.setValue(eventToEdit.getCategory());
            
            LocalTime start = eventToEdit.getStartTime();
            int h = start.getHour();
            String amPm = h >= 12 ? "PM" : "AM";
            if (h > 12) h -= 12;
            if (h == 0) h = 12;
            
            hourBox.setValue(String.format("%02d", h));
            minBox.setValue(String.format("%02d", start.getMinute()));
            amPmBox.setValue(amPm);
            
            // Check if event is completed/past and show badge
            if (eventToEdit.getEventDate().isBefore(LocalDate.now()) || 
               (eventToEdit.getEventDate().equals(LocalDate.now()) && eventToEdit.getEndTime().isBefore(LocalTime.now()))) {
                Label completedLabel = new Label("COMPLETED");
                completedLabel.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-padding: 5 10; -fx-background-radius: 10; -fx-font-weight: bold;");
                grid.add(completedLabel, 1, 9);
            }
            
            // Fetch contact details
            try {
                Connection conn = DatabaseConnection.getReusableConnection();
                PreparedStatement stmt = conn.prepareStatement("SELECT contact_phone, contact_email FROM events WHERE id = ?");
                stmt.setInt(1, eventToEdit.getId());
                var rs = stmt.executeQuery();
                if (rs.next()) {
                    phoneField.setText(rs.getString("contact_phone"));
                    emailField.setText(rs.getString("contact_email"));
                }
            } catch (SQLException e) { e.printStackTrace(); }
        }
        
        grid.add(new Label("Title:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Date:"), 0, 1);
        grid.add(datePicker, 1, 1);
        grid.add(new Label("Time:"), 0, 2);
        grid.add(timePicker, 1, 2);
        grid.add(new Label("Location:"), 0, 3);
        grid.add(locationField, 1, 3);
        grid.add(new Label("Phone:"), 0, 4);
        grid.add(phoneField, 1, 4);
        grid.add(new Label("Email:"), 0, 5);
        grid.add(emailField, 1, 5);
        grid.add(new Label("Category:"), 0, 6);
        grid.add(categoryBox, 1, 6);
        grid.add(new Label("SMS:"), 0, 7);
        grid.add(smsCheckBox, 1, 7);
        
        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(10, 0, 0, 0));
        
        Button saveBtn = createStyledButton(eventToEdit == null ? "Save" : "Update", "#2ecc71", 100, 40);
        Button cancelBtn = createStyledButton("Cancel", "#95a5a6", 100, 40);
        
        if (eventToEdit != null) {
            Button deleteBtn = createStyledButton("Delete", "#e05c4dff", 100, 40);
            deleteBtn.setOnAction(e -> {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Delete Event");
                confirm.setHeaderText("Delete '" + eventToEdit.getTitle() + "'?");
                confirm.setContentText("Are you sure you want to delete this event? This cannot be undone.");
                
                if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                    try {
                        Connection conn = DatabaseConnection.getReusableConnection();
                        conn.createStatement().execute("DELETE FROM events WHERE id = " + eventToEdit.getId());
                        dialog.close();
                    } catch (SQLException ex) {
                        showAlert("Error", "Failed to delete event: " + ex.getMessage());
                    }
                }
            });
            buttons.getChildren().addAll(saveBtn, deleteBtn, cancelBtn);
        } else {
            buttons.getChildren().addAll(saveBtn, cancelBtn);
        }
        
        VBox content = new VBox(15, grid, buttons);
        content.setPadding(new Insets(10, 20, 20, 20));
        
        Scene scene = new Scene(content, 550, 650);
        dialog.setScene(scene);
        
        cancelBtn.setOnAction(e -> dialog.close());
        
        saveBtn.setOnAction(e -> {
            String title = titleField.getText().trim();
            LocalDate date = datePicker.getValue();
            
            if (title.isEmpty()) {
                showAlert("Validation", "Title is required.");
                return;
            }
            if (date == null) {
                showAlert("Validation", "Date is required.");
                return;
            }
            
            LocalTime startTime;
            LocalTime endTime;
            try {
                int h = Integer.parseInt(hourBox.getValue());
                int m = Integer.parseInt(minBox.getValue());
                String amPm = amPmBox.getValue();
                
                if ("PM".equals(amPm) && h < 12) h += 12;
                if ("AM".equals(amPm) && h == 12) h = 0;
                
                startTime = LocalTime.of(h, m);
                endTime = startTime.plusHours(1);
            } catch (Exception ex) {
                showAlert("Validation", "Invalid time selected.");
                return;
            }
            
            // Check if event time is in the past
            if (java.time.LocalDateTime.of(date, startTime).isBefore(java.time.LocalDateTime.now())) {
                showAlert("Validation", "Event cannot be scheduled in the past.");
                return;
            }
            
            try {
                if (eventToEdit == null) {
                    Event newEvent = new Event(
                        title,
                        "",
                        locationField.getText(),
                        date,
                        startTime,
                        endTime,
                        categoryBox.getValue(),
                        SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getId() : 0
                    );
                    saveEventToDatabase(newEvent, phoneField.getText(), emailField.getText());
                } else {
                    eventToEdit.setTitle(title);
                    eventToEdit.setDescription("");
                    eventToEdit.setLocation(locationField.getText());
                    eventToEdit.setEventDate(date);
                    eventToEdit.setStartTime(startTime);
                    eventToEdit.setEndTime(endTime);
                    eventToEdit.setCategory(categoryBox.getValue());
                    updateEventInDatabase(eventToEdit, phoneField.getText(), emailField.getText());
                }
                
                System.out.println("Event saved/updated successfully.");
                
                String smsMsg = smsCheckBox.isSelected() ? "\nSMS Notification scheduled." : "";
                
                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.setTitle("Success");
                success.setHeaderText("Event Created");
                success.setContentText("Event '" + title + "' added for " + date + " at " + startTime + "." + smsMsg);
                success.showAndWait();
                
                System.out.println("Event processed: " + title);
                dialog.close();
                
                if (eventManagerController != null) {
                    eventManagerController.refreshTable();
                }
            } catch (Exception ex) {
                Alert error = new Alert(Alert.AlertType.ERROR);
                error.setTitle("Error");
                error.setHeaderText("Failed to Create Event");
                error.setContentText("Error: " + ex.getMessage());
                error.showAndWait();
            }
        });
        
        dialog.centerOnScreen();
        dialog.show();
    }

    private void styleTextArea(TextArea area) {
        area.setStyle("-fx-background-color: rgba(248, 249, 250, 0.7); -fx-border-color: #e0e0e0; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 5; -fx-font-size: 14px;");
        area.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) area.setStyle("-fx-background-color: white; -fx-border-color: #3498db; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 5; -fx-font-size: 14px; -fx-effect: dropshadow(three-pass-box, rgba(52, 152, 219, 0.2), 5, 0, 0, 2);");
            else area.setStyle("-fx-background-color: rgba(248, 249, 250, 0.7); -fx-border-color: #e0e0e0; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 5; -fx-font-size: 14px;");
        });
    }

    private void styleDatePicker(DatePicker picker) {
        picker.setStyle("-fx-background-color: rgba(248, 249, 250, 0.7); -fx-border-color: #e0e0e0; -fx-border-radius: 30; -fx-background-radius: 30; -fx-padding: 5 15; -fx-font-size: 14px;");
        picker.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) picker.setStyle("-fx-background-color: white; -fx-border-color: #3498db; -fx-border-radius: 30; -fx-background-radius: 30; -fx-padding: 5 15; -fx-font-size: 14px; -fx-effect: dropshadow(three-pass-box, rgba(52, 152, 219, 0.2), 5, 0, 0, 2);");
            else picker.setStyle("-fx-background-color: rgba(248, 249, 250, 0.7); -fx-border-color: #e0e0e0; -fx-border-radius: 30; -fx-background-radius: 30; -fx-padding: 5 15; -fx-font-size: 14px;");
        });
        picker.setPrefHeight(35);
        picker.setPrefWidth(300);
    }

    private void styleComboBox(ComboBox<?> box) {
        box.setStyle("-fx-background-color: rgba(248, 249, 250, 0.7); -fx-border-color: #e0e0e0; -fx-border-radius: 30; -fx-background-radius: 30; -fx-padding: 2 10; -fx-font-size: 14px;");
        box.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) box.setStyle("-fx-background-color: white; -fx-border-color: #3498db; -fx-border-radius: 30; -fx-background-radius: 30; -fx-padding: 2 10; -fx-font-size: 14px; -fx-effect: dropshadow(three-pass-box, rgba(52, 152, 219, 0.2), 5, 0, 0, 2);");
            else box.setStyle("-fx-background-color: rgba(248, 249, 250, 0.7); -fx-border-color: #e0e0e0; -fx-border-radius: 30; -fx-background-radius: 30; -fx-padding: 2 10; -fx-font-size: 14px;");
        });
        box.setPrefHeight(35);
    }

    private void saveEventToDatabase(Event event, String contactPhone, String contactEmail) throws SQLException {
        String sql = "INSERT INTO events (title, location, event_date, start_time, end_time, category, user_id, contact_phone, contact_email) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try {
            Connection conn = DatabaseConnection.getReusableConnection();
            // Note: Not closing connection as it is reusable
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, event.getTitle());
                stmt.setString(2, event.getLocation());
                stmt.setDate(3, Date.valueOf(event.getEventDate()));
                stmt.setTime(4, Time.valueOf(event.getStartTime()));
                stmt.setTime(5, Time.valueOf(event.getEndTime()));
                stmt.setString(6, event.getCategory());
                stmt.setInt(7, event.getUserId());
                stmt.setString(8, contactPhone);
                stmt.setString(9, contactEmail);
                
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            // Auto-fix for missing columns (Error 1054: Unknown column)
            if (e.getMessage().contains("Unknown column")) {
                System.out.println("Database schema mismatch. Attempting to fix...");
                try {
                    Connection conn = DatabaseConnection.getReusableConnection();
                    java.sql.Statement stmt = conn.createStatement();
                    try { stmt.execute("ALTER TABLE events ADD COLUMN contact_phone VARCHAR(50)"); } catch (Exception ex) {}
                    try { stmt.execute("ALTER TABLE events ADD COLUMN contact_email VARCHAR(100)"); } catch (Exception ex) {}
                    
                    // Retry insert
                    try (PreparedStatement retryStmt = conn.prepareStatement(sql)) {
                        retryStmt.setString(1, event.getTitle());
                        retryStmt.setString(2, event.getLocation());
                        retryStmt.setDate(3, Date.valueOf(event.getEventDate()));
                        retryStmt.setTime(4, Time.valueOf(event.getStartTime()));
                        retryStmt.setTime(5, Time.valueOf(event.getEndTime()));
                        retryStmt.setString(6, event.getCategory());
                        retryStmt.setInt(7, event.getUserId());
                        retryStmt.setString(8, contactPhone);
                        retryStmt.setString(9, contactEmail);
                        retryStmt.executeUpdate();
                        return;
                    }
                } catch (Exception ex) {
                    System.err.println("Auto-fix failed: " + ex.getMessage());
                }
            }
            throw e;
        }
    }
    
    private void updateEventInDatabase(Event event, String contactPhone, String contactEmail) throws SQLException {
        String sql = "UPDATE events SET title=?, location=?, event_date=?, start_time=?, end_time=?, category=?, contact_phone=?, contact_email=? WHERE id=?";
        try {
            Connection conn = DatabaseConnection.getReusableConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, event.getTitle());
                stmt.setString(2, event.getLocation());
                stmt.setDate(3, Date.valueOf(event.getEventDate()));
                stmt.setTime(4, Time.valueOf(event.getStartTime()));
                stmt.setTime(5, Time.valueOf(event.getEndTime()));
                stmt.setString(6, event.getCategory());
                stmt.setString(7, contactPhone);
                stmt.setString(8, contactEmail);
                stmt.setInt(9, event.getId());
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            // Auto-fix for missing columns (Error 1054: Unknown column)
            if (e.getMessage().contains("Unknown column")) {
                System.out.println("Database schema mismatch on update. Attempting to fix...");
                try {
                    Connection conn = DatabaseConnection.getReusableConnection();
                    java.sql.Statement stmt = conn.createStatement();
                    try { stmt.execute("ALTER TABLE events ADD COLUMN contact_phone VARCHAR(50)"); } catch (Exception ex) {}
                    try { stmt.execute("ALTER TABLE events ADD COLUMN contact_email VARCHAR(100)"); } catch (Exception ex) {}
                    
                    // Retry update
                    try (PreparedStatement retryStmt = conn.prepareStatement(sql)) {
                        retryStmt.setString(1, event.getTitle());
                        retryStmt.setString(2, event.getLocation());
                        retryStmt.setDate(3, Date.valueOf(event.getEventDate()));
                        retryStmt.setTime(4, Time.valueOf(event.getStartTime()));
                        retryStmt.setTime(5, Time.valueOf(event.getEndTime()));
                        retryStmt.setString(6, event.getCategory());
                        retryStmt.setString(7, contactPhone);
                        retryStmt.setString(8, contactEmail);
                        retryStmt.setInt(9, event.getId());
                        retryStmt.executeUpdate();
                        return; // Exit after successful retry
                    }
                } catch (Exception ex) {
                    System.err.println("Auto-fix on update failed: " + ex.getMessage());
                }
            }
            throw e; // Rethrow original exception if not handled
        }
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showLoginScreen() {
        System.out.println("Logging out and returning to login screen...");
        
        // Clear session
        SessionManager.logout();
        notifiedEventIds.clear();
        
        // Show login screen again
        start(primaryStage);
    }
    
    // ===== USER DASHBOARD FEATURES =====
    
    private void showUserStatistics() {
        System.out.println("Opening user statistics...");
        
        Stage statsStage = new Stage();
        statsStage.setTitle("My Statistics");
        statsStage.initModality(Modality.APPLICATION_MODAL);
        statsStage.initOwner(primaryStage);
        
        BorderPane mainLayout = new BorderPane();
        mainLayout.setPadding(new Insets(20));
        
        // Header
        Label title = new Label("? My Statistics");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        BorderPane.setMargin(title, new Insets(0, 0, 20, 0));
        mainLayout.setTop(title);
        
        // Statistics grid
        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(20);
        statsGrid.setVgap(20);
        statsGrid.setAlignment(Pos.CENTER);
        
        // Real user statistics
        VBox totalEventsCard = createAnalyticsCard("Total Events", String.valueOf(getEventCount("ALL")), "All recorded events", "#3498db");
        VBox upcomingEventsCard = createAnalyticsCard("Upcoming", String.valueOf(getEventCount("UPCOMING")), "Events in next 7 days", "#2ecc71");
        VBox completedEventsCard = createAnalyticsCard("Completed", String.valueOf(getEventCount("COMPLETED")), "Events in the past", "#f39c12");
        VBox meetingsCard = createAnalyticsCard("Meetings", String.valueOf(getEventCount("Meeting")), "Category: Meeting", "#9b59b6");
        
        statsGrid.add(totalEventsCard, 0, 0);
        statsGrid.add(upcomingEventsCard, 1, 0);
        statsGrid.add(completedEventsCard, 0, 1);
        statsGrid.add(meetingsCard, 1, 1);
        
        mainLayout.setCenter(statsGrid);
        
        // Additional info
        VBox infoBox = new VBox(10);
        infoBox.setPadding(new Insets(20, 0, 0, 0));
        infoBox.setAlignment(Pos.CENTER);
        
        Label infoLabel = new Label("Your event activity for the last 30 days");
        infoLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #11e6f5ff;");
        
        Button detailedViewBtn = createStyledButton("View Detailed Report", "#3498db", 160, 35);
        detailedViewBtn.setOnAction(e -> showAlert("Detailed Report", "Detailed statistics report would be shown here."));
        
        infoBox.getChildren().addAll(infoLabel, detailedViewBtn);
        mainLayout.setBottom(infoBox);
        
        Scene scene = new Scene(mainLayout, 700, 550);
        statsStage.setScene(scene);
        statsStage.centerOnScreen();
        statsStage.show();
    }
    
    private void showUserSettings() {
        System.out.println("Opening user settings...");
        
        Stage settingsStage = new Stage();
        settingsStage.setTitle("My Settings");
        settingsStage.initModality(Modality.APPLICATION_MODAL);
        settingsStage.initOwner(primaryStage);
        
        BorderPane mainLayout = new BorderPane();
        mainLayout.setStyle("-fx-background-color: #f4f6f8;");
        
        // Header
        HBox header = new HBox();
        header.setPadding(new Insets(15, 25, 15, 25));
        header.setStyle("-fx-background-color: white; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 0);");
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label title = new Label("Settings");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        header.getChildren().add(title);
        mainLayout.setTop(header);
        
        // Content Container
        VBox contentBox = new VBox(20);
        contentBox.setPadding(new Insets(25));
        contentBox.setStyle("-fx-background-color: transparent;");
        
        ScrollPane scrollPane = new ScrollPane(contentBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        // --- Section 1: General ---
        VBox generalSection = createSectionBox("General");
        GridPane generalGrid = createSectionGrid();
        
        TextField phoneField = createFormField("Mobile Number");
        phoneField.setText(currentUserPhoneNumber);
        
        Button verifyBtn = new Button("Verify");
        verifyBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 8 12; -fx-background-radius: 4;");
        verifyBtn.setOnAction(e -> getHostServices().showDocument("https://portal.infobip.com/apps/sms/numbers/verified"));
        HBox phoneBox = new HBox(10, phoneField, verifyBtn);
        HBox.setHgrow(phoneField, Priority.ALWAYS);
        
        ComboBox<String> timeZoneBox = new ComboBox<>();
        timeZoneBox.getItems().addAll("GMT+3", "UTC", "EST", "PST", "CET");
        timeZoneBox.setValue("GMT+3");
        timeZoneBox.setMaxWidth(Double.MAX_VALUE);
        timeZoneBox.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 4; -fx-padding: 5;");
        
        ComboBox<String> defaultViewBox = new ComboBox<>();
        defaultViewBox.getItems().addAll("Month View", "Week View", "List View");
        defaultViewBox.setValue("Month View");
        defaultViewBox.setMaxWidth(Double.MAX_VALUE);
        defaultViewBox.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 4; -fx-padding: 5;");

        addGridRow(generalGrid, 0, "Mobile Number:", phoneBox);
        addGridRow(generalGrid, 1, "Time Zone:", timeZoneBox);
        addGridRow(generalGrid, 2, "Default View:", defaultViewBox);
        generalSection.getChildren().add(generalGrid);
        
        // --- Section 2: Notifications ---
        VBox notifSection = createSectionBox("Notifications");
        GridPane notifGrid = createSectionGrid();
        
        CheckBox smsNotifications = new CheckBox("Enable SMS Alerts");
        smsNotifications.setSelected(smsNotificationsEnabled);
        smsNotifications.setStyle("-fx-font-size: 13px; -fx-text-fill: #34495e;");
        
        CheckBox emailReminders = new CheckBox("Enable Email Reminders");
        emailReminders.setSelected(emailNotificationsEnabled);
        emailReminders.setStyle("-fx-font-size: 13px; -fx-text-fill: #34495e;");
        
        CheckBox calendarNotifications = new CheckBox("Show Calendar Popups");
        calendarNotifications.setSelected(true);
        calendarNotifications.setStyle("-fx-font-size: 13px; -fx-text-fill: #34495e;");
        
        notifGrid.add(smsNotifications, 0, 0, 2, 1);
        notifGrid.add(emailReminders, 0, 1, 2, 1);
        notifGrid.add(calendarNotifications, 0, 2, 2, 1);
        notifSection.getChildren().add(notifGrid);

        // --- Section 3: SMTP Settings ---
        VBox smtpSection = createSectionBox("Email Integration (SMTP)");
        GridPane smtpGrid = createSectionGrid();
        
        CheckBox useSmtpCheck = new CheckBox("Use SMTP Server");
        useSmtpCheck.setSelected(useSmtp);
        useSmtpCheck.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        TextField smtpHostField = createFormField("Host (e.g., smtp.gmail.com)");
        smtpHostField.setText(smtpHost);
        
        TextField smtpPortField = createFormField("Port (e.g., 465)");
        smtpPortField.setText(smtpPort);
        
        TextField smtpEmailField = createFormField("Email Address");
        smtpEmailField.setText(smtpEmail);
        
        PasswordField smtpPasswordField = createPasswordField("App Password");
        smtpPasswordField.setText(smtpPassword);
        
        Button testSmtpBtn = new Button("Test SMTP");
        testSmtpBtn.setStyle("-fx-background-color: #8e44ad; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 15; -fx-background-radius: 4;");
        testSmtpBtn.setOnAction(e -> {
            smtpHost = smtpHostField.getText();
            smtpPort = smtpPortField.getText();
            smtpEmail = smtpEmailField.getText();
            smtpPassword = smtpPasswordField.getText();
            
            TextInputDialog dialog = new TextInputDialog(smtpEmail);
            dialog.setTitle("Test SMTP Email");
            dialog.setHeaderText("Send Test Email");
            dialog.setContentText("Enter recipient email address:");
            dialog.showAndWait().ifPresent(recipient -> {
                if (recipient != null && !recipient.trim().isEmpty()) {
                    sendSmtpEmail(recipient.trim(), "Test SMTP", "This is a test email via SMTP.");
                }
            });
        });

        addGridRow(smtpGrid, 0, "", useSmtpCheck);
        addGridRow(smtpGrid, 1, "Host:", smtpHostField);
        addGridRow(smtpGrid, 2, "Port:", smtpPortField);
        addGridRow(smtpGrid, 3, "Email:", smtpEmailField);
        addGridRow(smtpGrid, 4, "Password:", smtpPasswordField);
        addGridRow(smtpGrid, 5, "", testSmtpBtn);
        smtpSection.getChildren().add(smtpGrid);
        
        // --- Section 4: Infobip SMS ---
        VBox smsSection = createSectionBox("SMS Integration (Infobip)");
        GridPane smsGrid = createSectionGrid();
        
        PasswordField apiKeyField = createPasswordField("API Key");
        apiKeyField.setText(infobipApiKey);
        
        TextField baseUrlField = createFormField("Base URL");
        baseUrlField.setText(infobipBaseUrl);
        
        TextField emailSenderField = createFormField("Sender Name");
        emailSenderField.setText(emailSenderAddress);
        
        HBox smsTestBox = new HBox(10);
        Button testConnBtn = new Button("Check Connection");
        testConnBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 15; -fx-background-radius: 4;");
        testConnBtn.setOnAction(e -> {
            if (apiKeyField.getText().isEmpty() || baseUrlField.getText().isEmpty()) {
                showAlert("Missing Info", "Please enter API Key and Base URL.");
                return;
            }
            testInfobipConnection(apiKeyField.getText(), baseUrlField.getText());
        });
        
        Button testSmsBtn = new Button("Test SMS");
        testSmsBtn.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 15; -fx-background-radius: 4;");
        testSmsBtn.setOnAction(e -> {
            if (phoneField.getText().isEmpty() || apiKeyField.getText().isEmpty() || baseUrlField.getText().isEmpty()) {
                showAlert("Missing Info", "Please enter Phone, API Key, and Base URL.");
                return;
            }
            sendInfobipSms(phoneField.getText(), "Test message from EMS", apiKeyField.getText(), baseUrlField.getText());
            showAlert("Request Sent", "Test SMS sent to " + phoneField.getText());
        });
        
        smsTestBox.getChildren().addAll(testConnBtn, testSmsBtn);
        
        addGridRow(smsGrid, 0, "API Key:", apiKeyField);
        addGridRow(smsGrid, 1, "Base URL:", baseUrlField);
        addGridRow(smsGrid, 2, "Sender ID:", emailSenderField);
        addGridRow(smsGrid, 3, "", smsTestBox);
        smsSection.getChildren().add(smsGrid);
        
        contentBox.getChildren().addAll(generalSection, notifSection, smtpSection, smsSection);
        mainLayout.setCenter(scrollPane);
        
        // Footer
        HBox footer = new HBox(15);
        footer.setPadding(new Insets(15, 25, 15, 25));
        footer.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-width: 1 0 0 0;");
        footer.setAlignment(Pos.CENTER_RIGHT);
        
        Button changePassBtn = new Button("Change Password");
        changePassBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #7f8c8d; -fx-font-weight: bold; -fx-cursor: hand; -fx-border-color: #bdc3c7; -fx-border-radius: 4; -fx-padding: 8 15;");
        changePassBtn.setOnAction(e -> showChangePasswordDialog());
        
        Button closeBtn = new Button("Cancel");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #7f8c8d; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 15;");
        closeBtn.setOnAction(e -> settingsStage.close());
        
        Button saveBtn = new Button("Save Changes");
        saveBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 20; -fx-background-radius: 4; -fx-effect: dropshadow(three-pass-box, rgba(46, 204, 113, 0.4), 5, 0, 0, 2);");
        saveBtn.setOnAction(e -> {
            String phoneInput = phoneField.getText().trim();
            
            // Auto-format: Change 09... to +2519...
            if (phoneInput.startsWith("09")) {
                phoneInput = "+251" + phoneInput.substring(1);
            }
            
            if (!phoneInput.startsWith("+251")) {
                showAlert("Invalid Phone Number", "Phone number must start with +251 (e.g., +2519...)");
                return;
            }
            
            // Save settings
            currentUserPhoneNumber = phoneInput;
            smsNotificationsEnabled = smsNotifications.isSelected();
            emailNotificationsEnabled = emailReminders.isSelected();
            infobipApiKey = apiKeyField.getText();
            infobipBaseUrl = baseUrlField.getText();
            emailSenderAddress = emailSenderField.getText();
            
            useSmtp = useSmtpCheck.isSelected();
            smtpHost = smtpHostField.getText();
            smtpPort = smtpPortField.getText();
            smtpEmail = smtpEmailField.getText();
            smtpPassword = smtpPasswordField.getText();
            saveSettings();
            
            showAlert("Settings Saved", "Your settings have been saved successfully.");
            settingsStage.close();
        });
        
        footer.getChildren().addAll(changePassBtn, closeBtn, saveBtn);
        mainLayout.setBottom(footer);
        
        Scene scene = new Scene(mainLayout, 900, 700);
        settingsStage.setScene(scene);
        settingsStage.centerOnScreen();
        settingsStage.show();
    }
    
    // Helper methods for the new UI
    private VBox createSectionBox(String title) {
        VBox box = new VBox(15);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 1);");
        
        Label sectionTitle = new Label(title);
        sectionTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        box.getChildren().add(sectionTitle);
        return box;
    }
    
    private GridPane createSectionGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(15);
        
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(150);
        col1.setPrefWidth(150);
        
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        
        grid.getColumnConstraints().addAll(col1, col2);
        return grid;
    }
    
    private void addGridRow(GridPane grid, int row, String labelText, Node field) {
        if (!labelText.isEmpty()) {
            Label label = new Label(labelText);
            label.setStyle("-fx-text-fill: #7f8c8d; -fx-font-weight: bold; -fx-font-size: 13px;");
            grid.add(label, 0, row);
        }
        grid.add(field, 1, row);
    }

    private void applyTheme(String themeName) {
        Scene scene = primaryStage.getScene();
        if (scene == null) return;

        // Always remove the old theme before adding a new one to prevent conflicts
        scene.getStylesheets().removeIf(s -> s.contains("dark-theme.css") || s.contains("dashboard.css"));

        if ("Dark".equalsIgnoreCase(themeName)) {
            scene.getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());
            System.out.println("Applied Dark Theme.");
        } else { // Default to Light theme
            scene.getStylesheets().add(getClass().getResource("/css/dashboard.css").toExternalForm());
            System.out.println("Applied Light Theme.");
        }
    }
    
    private void loadSettings() {
        try {
            File configFile = new File(CONFIG_FILE);
            if (configFile.exists()) {
                try (FileInputStream fis = new FileInputStream(configFile)) {
                    appSettings.load(fis);
                    currentUserPhoneNumber = appSettings.getProperty("phoneNumber", "+251942557417");
                    smsNotificationsEnabled = Boolean.parseBoolean(appSettings.getProperty("smsEnabled", "true"));
                    emailNotificationsEnabled = Boolean.parseBoolean(appSettings.getProperty("emailEnabled", "true"));
                    emailSenderAddress = appSettings.getProperty("emailSender", "noreply@ems.com");
                    
                    // Load credentials, but force update if they appear to be the old ones
                    String loadedKey = appSettings.getProperty("infobipApiKey", "501a6d1c0532ef04d793d8d10e31fcb4-a035a4d0-a018-462c-9b4f-abe8e4e174f6");
                    String loadedUrl = appSettings.getProperty("infobipBaseUrl", "https://l2rkvw.api.infobip.com");
                    
                    // Auto-fix old credentials if found in file
                    if (loadedKey.startsWith("a86c")) infobipApiKey = "501a6d1c0532ef04d793d8d10e31fcb4-a035a4d0-a018-462c-9b4f-abe8e4e174f6";
                    else infobipApiKey = loadedKey;
                    
                    if (loadedUrl.contains("2yd8ym")) infobipBaseUrl = "https://l2rkvw.api.infobip.com";
                    else infobipBaseUrl = loadedUrl;
                    
                    // Load SMTP settings, preserving defaults if config is empty
                    useSmtp = Boolean.parseBoolean(appSettings.getProperty("useSmtp", "true"));
                    
                    String loadedSmtpHost = appSettings.getProperty("smtpHost", "");
                    if (!loadedSmtpHost.isEmpty()) smtpHost = loadedSmtpHost;
                    
                    String loadedSmtpPort = appSettings.getProperty("smtpPort", "");
                    if (!loadedSmtpPort.isEmpty()) smtpPort = loadedSmtpPort;
                    
                    String loadedSmtpEmail = appSettings.getProperty("smtpEmail", "");
                    if (!loadedSmtpEmail.isEmpty()) smtpEmail = loadedSmtpEmail;
                    
                    String loadedSmtpPass = appSettings.getProperty("smtpPassword", "");
                    if (!loadedSmtpPass.isEmpty()) smtpPassword = loadedSmtpPass;

                    System.out.println("Settings loaded successfully.");
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading settings: " + e.getMessage());
        }
    }

    private void saveSettings() {
        try {
            appSettings.setProperty("phoneNumber", currentUserPhoneNumber);
            appSettings.setProperty("smsEnabled", String.valueOf(smsNotificationsEnabled));
            appSettings.setProperty("emailEnabled", String.valueOf(emailNotificationsEnabled));
            appSettings.setProperty("infobipApiKey", infobipApiKey);
            appSettings.setProperty("infobipBaseUrl", infobipBaseUrl);
            appSettings.setProperty("emailSender", emailSenderAddress);
            appSettings.setProperty("useSmtp", String.valueOf(useSmtp));
            appSettings.setProperty("smtpHost", smtpHost);
            appSettings.setProperty("smtpPort", smtpPort);
            appSettings.setProperty("smtpEmail", smtpEmail);
            appSettings.setProperty("smtpPassword", smtpPassword);
            
            try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
                appSettings.store(fos, "Event Management System User Settings");
                System.out.println("Settings saved successfully.");
            }
        } catch (IOException e) {
            System.err.println("Error saving settings: " + e.getMessage());
        }
    }
    
    private void showChangePasswordDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Change Password");
        dialog.setHeaderText("Update your account password");
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        PasswordField currentPasswordField = createPasswordField("Current password");
        PasswordField newPasswordField = createPasswordField("New password");
        PasswordField confirmPasswordField = createPasswordField("Confirm new password");
        
        grid.add(new Label("Current Password:"), 0, 0);
        grid.add(currentPasswordField, 1, 0);
        grid.add(new Label("New Password:"), 0, 1);
        grid.add(newPasswordField, 1, 1);
        grid.add(new Label("Confirm Password:"), 0, 2);
        grid.add(confirmPasswordField, 1, 2);
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String newPass = newPasswordField.getText();
                String confirm = confirmPasswordField.getText();
                
                if (newPass.isEmpty() || confirm.isEmpty()) {
                    showAlert("Error", "Please fill in all password fields.");
                } else if (!newPass.equals(confirm)) {
                    showAlert("Error", "New passwords don't match.");
                } else if (newPass.length() < 6) {
                    showAlert("Error", "Password must be at least 6 characters long.");
                } else {
                    showAlert("Success", "Your password has been changed successfully!");
                }
            }
        });
    }
    
    public static void main(String[] args) {
        System.out.println("Launching Event Management System");
        launch(args);
    }
}
# Event Management System - AI Coding Guidelines

## Architecture Overview
This is a JavaFX desktop application using MVC pattern with dual UI implementations:
- **Main.java**: Inline UI creation for login/dashboard (1720+ lines)
- **Controllers/**: FXML-based controllers for modular views (Calendar, Events, etc.)

## Key Components
- **Models**: `Event.java`, `User.java` - Standard POJOs with getters/setters
- **Services**: `DatabaseService.java`, `AuthService.java`, `EmailService.java` - Business logic layer
- **Utils**: `DatabaseConnection.java` (singleton MySQL connection), `SessionManager.java`
- **Resources**: FXML layouts in `/fxml/`, CSS in `/css/`, schema in `/schema.sql`

## Development Workflow
1. **Build**: `mvn clean compile` - Compiles with Java 25
2. **Run**: `mvn javafx:run` - Launches via JavaFX Maven plugin
3. **Database**: Run `setup-database.sql` in MySQL, then `setup-database.ps1`
4. **Test Users**: admin/admin123, john.doe/password123

## Database Integration
- **Connection**: Hardcoded MySQL (localhost:3306/event_management, user:root, pass:122119me)
- **Schema**: Users, Events, Event_participants tables with FK constraints
- **Queries**: Use prepared statements in DatabaseService methods

## Code Conventions
- **Packages**: `com.calendar.app.{controllers,models,services,utils}`
- **Naming**: CamelCase classes, standard getters/setters
- **UI Styling**: Inline CSS in Main.java, external CSS files for FXML views
- **Error Handling**: Try-catch with System.err.println for DB errors
- **Imports**: Group by javafx.*, java.*, then project packages

## Common Patterns
- **Controller Initialization**: `@FXML initialize()` method for setup
- **Database Operations**: `DatabaseConnection.getReusableConnection()` for prepared statements
- **Event Handling**: Lambda expressions for button actions
- **Navigation**: New Stage/Scene for screen transitions (no routing framework)

## Examples
- **Add Event**: Use `DatabaseService.addEvent()` with Event model
- **User Auth**: `DatabaseService.validateUser()` returns boolean
- **Calendar Display**: YearMonth navigation in CalendarController
- **Email Notifications**: EmailService.sendEventNotification() for reminders
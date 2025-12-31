# Event Management System

A robust desktop application built with Java and JavaFX for managing personal and professional events. This system provides a user-friendly interface for scheduling, tracking, and organizing events with a visual calendar and detailed list views.

##  Features

*   **User Authentication**
    *   Secure Login and Registration system.
    *   Session management for user-specific data privacy.

*   **Dashboard**
    *   Centralized hub for quick navigation.
    *   System status indicators.

*   **Event Manager**
    *   **CRUD Operations**: Create, Read, Update, and Delete events.
    *   **Tabular View**: Detailed list of events with columns for Title, Date, Time, Location, Phone, and Category.
    *   **Search & Filter**: Real-time filtering of events by title, location, or category.
    *   **Export**: Export event lists to CSV format for external use.

*   **Calendar View**
    *   Monthly grid view to visualize schedule.
    *   Quick navigation between months.
    *   Side panel showing specific events for the selected month.

##  Tech Stack

*   **Language**: Java (JDK 17+)
*   **UI Framework**: JavaFX
*   **Database**: MySQL
*   **Connectivity**: JDBC (MySQL Connector/J)
*   **Architecture**: MVC (Model-View-Controller)

##  Project Structure

```text
src/main/
├── java/com/calendar/app/
│   ├── controllers/      # JavaFX Controllers (Main, EventManager, Calendar, Login, etc.)
│   ├── models/           # Data models (Event, User)
│   ├── services/         # Business logic and Database services
│   └── utils/            # Utilities (SessionManager, DatabaseConnection)
└── resources/fxml/       # FXML UI layout files
    ├── main.fxml
    ├── events.fxml
    ├── calendar.fxml
    ├── login.fxml
    └── registration.fxml
```

##  Setup & Installation

### Prerequisites
*   Java Development Kit (JDK) 17 or higher.
*   MySQL Server.
*   Maven or Gradle (depending on your build setup).

### Database Configuration
1.  Create a MySQL database named `event_management`.
2.  Run the following SQL script to create the necessary tables:

```sql
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    email VARCHAR(100)
);

CREATE TABLE events (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    title VARCHAR(100) NOT NULL,
    description TEXT,
    location VARCHAR(100),
    event_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    category VARCHAR(50),
    contact_phone VARCHAR(20),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

3.  Update the database credentials in `src/main/java/com/calendar/app/utils/DatabaseConnection.java` if they differ from the defaults.

### Running the Application

1.  **Clone the repository**
2.  **Build the project** using your IDE or build tool.
3.  **Run** the main application class (typically `App.java` or `Main.java`).

##  Usage

1.  **Register/Login**: Create a new account or log in.
2.  **Dashboard**: Navigate to **Calendar View** or **Event Manager**.
3.  **Manage Events**: Add new events, edit details, or delete them. Use the search bar to find specific items.
4.  **Export**: Click "Export Events" in the Event Manager to save your data as a `.csv` file.

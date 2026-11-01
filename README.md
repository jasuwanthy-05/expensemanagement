# Expense Management System

A Java 21 desktop expense tracker built with JavaFX, MySQL 8, and JDBC.

## Requirements

- JDK 21
- Maven 3.9+
- MySQL Server 8

## Setup

1. Install and start MySQL 8.
2. In MySQL Workbench or the MySQL command line, run `src/main/resources/sql/schema.sql`. The script creates the `expense_manager` database and its tables.
3. Edit `src/main/resources/config/db.properties` with your MySQL JDBC URL, username, and password. The supplied URL expects MySQL on `localhost:3306`.
4. From the project root, run:

   ```text
   mvn clean test
   mvn javafx:run
   ```

The application starts with the registration/login screen. Registering a new account seeds the eight default categories.

## Features

- Salted SHA-256 password hashing, account registration and login
- Add, edit, delete, search, filter, and sort expenses
- Per-user categories with names and colors
- Monthly per-category budgets with 80% warning and 100% limit indicators
- Dashboard metrics, spending-by-category pie chart, six-month bar chart, and recent expenses
- Monthly and yearly category reports with CSV export
- Monthly recurring bills generated once when their due day is reached
- Profile and password changes, light/dark theme
- Confirmations for destructive actions and error alerts
- Database work runs in JavaFX background tasks

## Common errors

- **Cannot connect to MySQL:** Confirm the MySQL service is running, the JDBC URL/port is correct, and the server accepts TCP connections.
- **Access denied:** Update `db.username` and `db.password` in `src/main/resources/config/db.properties` to match a MySQL account with access to `expense_manager`.
- **Unknown database:** Run `src/main/resources/sql/schema.sql` before launching the application.
- **Communications link failure:** Check that MySQL listens on the configured host and port and that local firewall rules permit the connection.
- **JavaFX runtime or Java version error:** Install JDK 21, confirm `java -version` reports 21, and run Maven from a terminal using that JDK.
- **Email already registered:** Sign in with the existing account or register with a different email address.
- **Category name conflict:** Category names must be unique within an account; choose a different name.

## Project structure

```text
expense-manager/
├── pom.xml
├── README.md
├── src/
│   ├── main/
│   │   ├── java/expensemanagement/
│   │   │   ├── dao/
│   │   │   ├── exception/
│   │   │   ├── model/
│   │   │   ├── service/
│   │   │   ├── ui/
│   │   │   └── util/
│   │   └── resources/
│   │       ├── config/db.properties
│   │       ├── css/style.css
│   │       └── sql/schema.sql
│   └── test/java/expensemanagement/
│       ├── service/
│       └── util/
└── target/
```

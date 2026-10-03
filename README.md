# Internship Management Portal

A Java Swing desktop application for managing internships, backed by MySQL.

## Tech stack
- Java 17, Maven
- Swing + [FlatLaf](https://www.formdev.com/flatlaf/) look and feel
- MySQL (JDBC via `mysql-connector-j`)
- BCrypt password hashing (jBCrypt)

## Setup
1. Create the database and tables:
   ```sql
   SOURCE db/schema.sql;
   ```
2. Copy `src/main/resources/db.properties.example` to
   `src/main/resources/db.properties` and set your MySQL credentials.
3. Build and run:
   ```bash
   mvn compile exec:java
   ```

On first run a default admin is created:
- Email: `admin@portal.com`
- Password: `Admin@123`  (change it after your first login)

CREATE DATABASE IF NOT EXISTS internship_portal
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE internship_portal;

CREATE TABLE IF NOT EXISTS users (
  user_id       INT AUTO_INCREMENT PRIMARY KEY,
  full_name     VARCHAR(100) NOT NULL,
  email         VARCHAR(100) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  role          ENUM('ADMIN','STUDENT') NOT NULL,
  is_active     BOOLEAN NOT NULL DEFAULT TRUE,
  created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS students (
  student_id  INT PRIMARY KEY,
  phone       VARCHAR(15),
  course      VARCHAR(50),
  skills      TEXT,
  resume_path VARCHAR(255),
  FOREIGN KEY (student_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS companies (
  company_id    INT AUTO_INCREMENT PRIMARY KEY,
  company_name  VARCHAR(100) NOT NULL UNIQUE,
  industry      VARCHAR(50),
  website       VARCHAR(100),
  contact_email VARCHAR(100),
  created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS internships (
  internship_id  INT AUTO_INCREMENT PRIMARY KEY,
  company_id     INT NOT NULL,
  title          VARCHAR(100) NOT NULL,
  description    TEXT,
  stipend        DECIMAL(10,2) NOT NULL DEFAULT 0,
  duration_weeks INT NOT NULL,
  deadline       DATE NOT NULL,
  status         ENUM('OPEN','CLOSED') NOT NULL DEFAULT 'OPEN',
  created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (company_id) REFERENCES companies(company_id),
  INDEX idx_internships_status (status)
);

CREATE TABLE IF NOT EXISTS applications (
  application_id INT AUTO_INCREMENT PRIMARY KEY,
  student_id     INT NOT NULL,
  internship_id  INT NOT NULL,
  status         ENUM('APPLIED','SHORTLISTED','SELECTED','REJECTED','COMPLETED')
                 NOT NULL DEFAULT 'APPLIED',
  applied_on     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_student_internship (student_id, internship_id),
  FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE,
  FOREIGN KEY (internship_id) REFERENCES internships(internship_id),
  INDEX idx_applications_status (status)
);

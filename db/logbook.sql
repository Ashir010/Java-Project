USE internship_portal;

CREATE TABLE IF NOT EXISTS logbook_entries (
  entry_id       INT AUTO_INCREMENT PRIMARY KEY,
  application_id INT NOT NULL,
  week_no        INT NOT NULL,
  summary        TEXT NOT NULL,
  submitted_on   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  grade          INT NULL,
  remarks        TEXT NULL,
  graded_by      INT NULL,
  graded_on      TIMESTAMP NULL,
  UNIQUE KEY uq_application_week (application_id, week_no),
  CONSTRAINT chk_logbook_grade CHECK (grade IS NULL OR grade BETWEEN 0 AND 10),
  FOREIGN KEY (application_id) REFERENCES applications(application_id) ON DELETE CASCADE,
  FOREIGN KEY (graded_by) REFERENCES users(user_id) ON DELETE SET NULL
);

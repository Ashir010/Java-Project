USE internship_portal;

CREATE TABLE IF NOT EXISTS application_history (
  history_id     INT AUTO_INCREMENT PRIMARY KEY,
  application_id INT NOT NULL,
  status         ENUM('APPLIED','SHORTLISTED','SELECTED','REJECTED','COMPLETED') NOT NULL,
  changed_on     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (application_id) REFERENCES applications(application_id) ON DELETE CASCADE,
  INDEX idx_history_application (application_id)
);

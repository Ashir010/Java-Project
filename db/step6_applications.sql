USE internship_portal;

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

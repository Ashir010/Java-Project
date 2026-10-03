USE internship_portal;

CREATE TABLE IF NOT EXISTS students (
  student_id  INT PRIMARY KEY,
  phone       VARCHAR(15),
  course      VARCHAR(50),
  skills      TEXT,
  resume_path VARCHAR(255),
  FOREIGN KEY (student_id) REFERENCES users(user_id) ON DELETE CASCADE
);

USE internship_portal;

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

USE internship_portal;

CREATE TABLE IF NOT EXISTS documents (
  document_id    INT AUTO_INCREMENT PRIMARY KEY,
  application_id INT NOT NULL,
  doc_type       ENUM('OFFER_LETTER','COMPLETION_CERTIFICATE') NOT NULL,
  reference_no   VARCHAR(30) NOT NULL UNIQUE,
  file_path      VARCHAR(255) NOT NULL,
  issued_on      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  issued_by      INT NULL,
  UNIQUE KEY uq_application_doctype (application_id, doc_type),
  FOREIGN KEY (application_id) REFERENCES applications(application_id) ON DELETE CASCADE,
  FOREIGN KEY (issued_by) REFERENCES users(user_id) ON DELETE SET NULL
);

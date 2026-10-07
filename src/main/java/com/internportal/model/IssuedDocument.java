package com.internportal.model;

import java.time.LocalDateTime;

/** A PDF that has been issued, with the details shown in the document tables. */
public class IssuedDocument {

    private int documentId;
    private int applicationId;
    private DocumentType type;
    private String referenceNo;
    private String filePath;
    private LocalDateTime issuedOn;
    private String studentName;
    private String internshipTitle;
    private String companyName;

    public int getDocumentId() { return documentId; }
    public void setDocumentId(int documentId) { this.documentId = documentId; }

    public int getApplicationId() { return applicationId; }
    public void setApplicationId(int applicationId) { this.applicationId = applicationId; }

    public DocumentType getType() { return type; }
    public void setType(DocumentType type) { this.type = type; }

    public String getReferenceNo() { return referenceNo; }
    public void setReferenceNo(String referenceNo) { this.referenceNo = referenceNo; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public LocalDateTime getIssuedOn() { return issuedOn; }
    public void setIssuedOn(LocalDateTime issuedOn) { this.issuedOn = issuedOn; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getInternshipTitle() { return internshipTitle; }
    public void setInternshipTitle(String internshipTitle) { this.internshipTitle = internshipTitle; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
}

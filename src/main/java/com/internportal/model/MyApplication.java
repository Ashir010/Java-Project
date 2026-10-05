package com.internportal.model;

import java.time.LocalDateTime;

/** One application as the student who made it sees it. */
public class MyApplication {

    private int applicationId;
    private int internshipId;
    private String internshipTitle;
    private String companyName;
    private LocalDateTime appliedOn;
    private LocalDateTime lastUpdate;
    private ApplicationStatus status;

    public int getApplicationId() { return applicationId; }
    public void setApplicationId(int applicationId) { this.applicationId = applicationId; }

    public int getInternshipId() { return internshipId; }
    public void setInternshipId(int internshipId) { this.internshipId = internshipId; }

    public String getInternshipTitle() { return internshipTitle; }
    public void setInternshipTitle(String internshipTitle) { this.internshipTitle = internshipTitle; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public LocalDateTime getAppliedOn() { return appliedOn; }
    public void setAppliedOn(LocalDateTime appliedOn) { this.appliedOn = appliedOn; }

    public LocalDateTime getLastUpdate() { return lastUpdate; }
    public void setLastUpdate(LocalDateTime lastUpdate) { this.lastUpdate = lastUpdate; }

    public ApplicationStatus getStatus() { return status; }
    public void setStatus(ApplicationStatus status) { this.status = status; }
}

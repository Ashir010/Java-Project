package com.internportal.model;

import java.time.LocalDate;

/** One of a student's selected (or completed) internships, as shown in the logbook drop-down. */
public class LogbookInternship {

    private int applicationId;
    private int internshipId;
    private String title;
    private String companyName;
    private LocalDate startDate;
    private int durationWeeks;
    private ApplicationStatus status;

    public int getApplicationId() { return applicationId; }
    public void setApplicationId(int applicationId) { this.applicationId = applicationId; }

    public int getInternshipId() { return internshipId; }
    public void setInternshipId(int internshipId) { this.internshipId = internshipId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public int getDurationWeeks() { return durationWeeks; }
    public void setDurationWeeks(int durationWeeks) { this.durationWeeks = durationWeeks; }

    public ApplicationStatus getStatus() { return status; }
    public void setStatus(ApplicationStatus status) { this.status = status; }

    /** Used by JComboBox to display the internship. */
    @Override
    public String toString() {
        String suffix = status == ApplicationStatus.COMPLETED ? "  [Completed]" : "";
        return title + " (" + companyName + ")" + suffix;
    }
}

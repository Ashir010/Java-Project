package com.internportal.model;

import java.time.LocalDateTime;

/** One submitted logbook entry as the admin sees it, with the student and internship details. */
public class LogbookRecord {

    private int entryId;
    private int applicationId;
    private String studentName;
    private String internshipTitle;
    private String companyName;
    private int weekNo;
    private String summary;
    private LocalDateTime submittedOn;
    private Integer grade;          // null until graded
    private String remarks;
    private LocalDateTime gradedOn;
    private ApplicationStatus applicationStatus;
    private int durationWeeks;

    public int getEntryId() { return entryId; }
    public void setEntryId(int entryId) { this.entryId = entryId; }

    public int getApplicationId() { return applicationId; }
    public void setApplicationId(int applicationId) { this.applicationId = applicationId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getInternshipTitle() { return internshipTitle; }
    public void setInternshipTitle(String internshipTitle) { this.internshipTitle = internshipTitle; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public int getWeekNo() { return weekNo; }
    public void setWeekNo(int weekNo) { this.weekNo = weekNo; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public LocalDateTime getSubmittedOn() { return submittedOn; }
    public void setSubmittedOn(LocalDateTime submittedOn) { this.submittedOn = submittedOn; }

    public Integer getGrade() { return grade; }
    public void setGrade(Integer grade) { this.grade = grade; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public LocalDateTime getGradedOn() { return gradedOn; }
    public void setGradedOn(LocalDateTime gradedOn) { this.gradedOn = gradedOn; }

    public ApplicationStatus getApplicationStatus() { return applicationStatus; }
    public void setApplicationStatus(ApplicationStatus applicationStatus) { this.applicationStatus = applicationStatus; }

    public int getDurationWeeks() { return durationWeeks; }
    public void setDurationWeeks(int durationWeeks) { this.durationWeeks = durationWeeks; }
}

package com.internportal.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Everything the PDF generator needs about one application. */
public class DocumentSource {

    private int applicationId;
    private ApplicationStatus status;
    private String studentName;
    private String studentEmail;
    private String course;
    private String internshipTitle;
    private BigDecimal stipend;
    private int durationWeeks;
    private LocalDate startDate;
    private String companyName;
    private String industry;
    private String website;
    private int gradedWeeks;
    private Double averageGrade;     // null when no week has been graded

    public int getApplicationId() { return applicationId; }
    public void setApplicationId(int applicationId) { this.applicationId = applicationId; }

    public ApplicationStatus getStatus() { return status; }
    public void setStatus(ApplicationStatus status) { this.status = status; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }

    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }

    public String getInternshipTitle() { return internshipTitle; }
    public void setInternshipTitle(String internshipTitle) { this.internshipTitle = internshipTitle; }

    public BigDecimal getStipend() { return stipend; }
    public void setStipend(BigDecimal stipend) { this.stipend = stipend; }

    public int getDurationWeeks() { return durationWeeks; }
    public void setDurationWeeks(int durationWeeks) { this.durationWeeks = durationWeeks; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public int getGradedWeeks() { return gradedWeeks; }
    public void setGradedWeeks(int gradedWeeks) { this.gradedWeeks = gradedWeeks; }

    public Double getAverageGrade() { return averageGrade; }
    public void setAverageGrade(Double averageGrade) { this.averageGrade = averageGrade; }

    /** The last day of the internship: the start date plus the duration in weeks, minus one day. */
    public LocalDate getEndDate() {
        return startDate.plusWeeks(durationWeeks).minusDays(1);
    }
}

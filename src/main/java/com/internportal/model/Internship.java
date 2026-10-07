package com.internportal.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Internship {

    private int internshipId;
    private int companyId;
    private String companyName;      // filled by the join query, for display only
    private String title;
    private String description;
    private BigDecimal stipend;
    private int durationWeeks;
    private LocalDate startDate;
    private LocalDate deadline;
    private InternshipStatus status = InternshipStatus.OPEN;

    public int getInternshipId() { return internshipId; }
    public void setInternshipId(int internshipId) { this.internshipId = internshipId; }

    public int getCompanyId() { return companyId; }
    public void setCompanyId(int companyId) { this.companyId = companyId; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getStipend() { return stipend; }
    public void setStipend(BigDecimal stipend) { this.stipend = stipend; }

    public int getDurationWeeks() { return durationWeeks; }
    public void setDurationWeeks(int durationWeeks) { this.durationWeeks = durationWeeks; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }

    public InternshipStatus getStatus() { return status; }
    public void setStatus(InternshipStatus status) { this.status = status; }

    /** The last day of the internship: the start date plus the duration in weeks, minus one day. */
    public LocalDate getEndDate() {
        return startDate == null ? null : startDate.plusWeeks(durationWeeks).minusDays(1);
    }

    /** Start and end dates together, for showing in tables. */
    public InternshipPeriod getPeriod() {
        return startDate == null ? null : new InternshipPeriod(startDate, getEndDate());
    }
}

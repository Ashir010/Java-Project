package com.internportal.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** One week of a student's logbook: its dates, and the entry if one was written. */
public class LogbookWeek {

    private int weekNo;
    private LocalDate weekStart;
    private LocalDate weekEnd;
    private WeekStatus status;
    private String summary;
    private Integer grade;          // null until graded
    private String remarks;
    private LocalDateTime submittedOn;

    public int getWeekNo() { return weekNo; }
    public void setWeekNo(int weekNo) { this.weekNo = weekNo; }

    public LocalDate getWeekStart() { return weekStart; }
    public void setWeekStart(LocalDate weekStart) { this.weekStart = weekStart; }

    public LocalDate getWeekEnd() { return weekEnd; }
    public void setWeekEnd(LocalDate weekEnd) { this.weekEnd = weekEnd; }

    public WeekStatus getStatus() { return status; }
    public void setStatus(WeekStatus status) { this.status = status; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public Integer getGrade() { return grade; }
    public void setGrade(Integer grade) { this.grade = grade; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public LocalDateTime getSubmittedOn() { return submittedOn; }
    public void setSubmittedOn(LocalDateTime submittedOn) { this.submittedOn = submittedOn; }
}

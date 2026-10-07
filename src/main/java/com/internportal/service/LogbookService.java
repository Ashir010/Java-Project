package com.internportal.service;

import com.internportal.dao.LogbookDAO;
import com.internportal.model.ApplicationStatus;
import com.internportal.model.LogbookInternship;
import com.internportal.model.LogbookRecord;
import com.internportal.model.LogbookWeek;
import com.internportal.model.WeekStatus;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

/** Business rules for the weekly logbook: who can write which week, grading, and completing an internship. */
public class LogbookService {

    private static final String DB_ERROR =
            "Cannot reach the database. Check that MySQL is running and db.properties is correct.";
    private static final int MYSQL_DUPLICATE_ENTRY = 1062;
    private static final int MIN_SUMMARY = 20;
    private static final int MAX_SUMMARY = 2000;
    private static final int MAX_REMARKS = 500;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final LogbookDAO dao = new LogbookDAO();

    // ---------------------------------------------------------------- Student side

    public List<LogbookInternship> listMyInternships(int studentId) throws ServiceException {
        try {
            return dao.findInternshipsFor(studentId);
        } catch (SQLException e) {
            throw dbError(e);
        }
    }

    /** One row for every week of the internship, with its dates and status. */
    public List<LogbookWeek> weeks(int studentId, int applicationId) throws ServiceException {
        try {
            LogbookInternship internship = findOwned(studentId, applicationId);

            Map<Integer, LogbookWeek> entries = new HashMap<>();
            for (LogbookWeek entry : dao.findEntries(applicationId)) {
                entries.put(entry.getWeekNo(), entry);
            }

            LocalDate today = LocalDate.now();
            List<LogbookWeek> weeks = new ArrayList<>();
            for (int n = 1; n <= internship.getDurationWeeks(); n++) {
                LocalDate start = internship.getStartDate().plusWeeks(n - 1);
                LogbookWeek week = new LogbookWeek();
                week.setWeekNo(n);
                week.setWeekStart(start);
                week.setWeekEnd(start.plusDays(6));

                LogbookWeek entry = entries.get(n);
                if (entry != null) {
                    week.setSummary(entry.getSummary());
                    week.setGrade(entry.getGrade());
                    week.setRemarks(entry.getRemarks());
                    week.setSubmittedOn(entry.getSubmittedOn());
                    week.setStatus(entry.getGrade() == null ? WeekStatus.SUBMITTED : WeekStatus.GRADED);
                } else {
                    week.setStatus(start.isAfter(today) ? WeekStatus.NOT_STARTED : WeekStatus.PENDING);
                }
                weeks.add(week);
            }
            return weeks;
        } catch (SQLException e) {
            throw dbError(e);
        }
    }

    /** Saves a week's entry. Every rule is checked here, not only in the screen. */
    public void submit(int studentId, int applicationId, int weekNo, String summary) throws ServiceException {
        String text = summary == null ? "" : summary.trim();
        if (text.length() < MIN_SUMMARY) {
            throw new ServiceException("Please describe your week in at least " + MIN_SUMMARY + " characters.");
        }
        if (text.length() > MAX_SUMMARY) {
            throw new ServiceException("The entry must be " + MAX_SUMMARY + " characters or fewer.");
        }

        try {
            LogbookInternship internship = findOwned(studentId, applicationId);
            if (internship.getStatus() != ApplicationStatus.SELECTED) {
                throw new ServiceException("This internship is completed, so its logbook can no longer be changed.");
            }
            if (weekNo < 1 || weekNo > internship.getDurationWeeks()) {
                throw new ServiceException("This internship has weeks 1 to " + internship.getDurationWeeks() + ".");
            }
            LocalDate weekStart = internship.getStartDate().plusWeeks(weekNo - 1);
            if (weekStart.isAfter(LocalDate.now())) {
                throw new ServiceException("Week " + weekNo + " starts on " + weekStart.format(DATE_FORMAT)
                        + ", so it cannot be written yet.");
            }

            if (!dao.saveEntry(applicationId, weekNo, text)) {
                throw new ServiceException("This entry has already been graded and can no longer be changed.");
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == MYSQL_DUPLICATE_ENTRY) {
                throw new ServiceException("This week was just saved from somewhere else. Please reload.", e);
            }
            throw dbError(e);
        }
    }

    private LogbookInternship findOwned(int studentId, int applicationId) throws SQLException, ServiceException {
        Optional<LogbookInternship> found = dao.findInternship(applicationId, studentId);
        if (!found.isPresent()) {
            throw new ServiceException("You can only write a logbook for an internship you have been selected for.");
        }
        return found.get();
    }

    // ---------------------------------------------------------------- Admin side

    /** graded null means all entries, false means awaiting grade, true means graded. */
    public List<LogbookRecord> listRecords(int internshipId, Boolean graded) throws ServiceException {
        try {
            return dao.findRecords(internshipId, graded);
        } catch (SQLException e) {
            throw dbError(e);
        }
    }

    public void grade(int entryId, int grade, String remarks, int adminId) throws ServiceException {
        if (grade < 0 || grade > 10) {
            throw new ServiceException("The grade must be between 0 and 10.");
        }
        String cleanRemarks = remarks == null ? "" : remarks.trim();
        if (cleanRemarks.length() > MAX_REMARKS) {
            throw new ServiceException("Remarks must be " + MAX_REMARKS + " characters or fewer.");
        }
        try {
            boolean saved = dao.grade(entryId, grade, cleanRemarks.isEmpty() ? null : cleanRemarks, adminId);
            if (!saved) {
                throw new ServiceException(
                        "This entry can no longer be graded (the internship is completed, or the entry was removed).");
            }
        } catch (SQLException e) {
            throw dbError(e);
        }
    }

    /** Marks an internship COMPLETED. Explains exactly what is missing when it cannot. */
    public void complete(int applicationId) throws ServiceException {
        try {
            if (dao.complete(applicationId)) {
                return;
            }

            Optional<ApplicationStatus> status = dao.findApplicationStatus(applicationId);
            if (!status.isPresent()) {
                throw new ServiceException("Application not found.");
            }
            if (status.get() == ApplicationStatus.COMPLETED) {
                throw new ServiceException("This internship is already completed.");
            }
            if (status.get() != ApplicationStatus.SELECTED) {
                throw new ServiceException("Only a selected student's internship can be completed. Current status: "
                        + status.get() + ".");
            }

            int duration = dao.findDurationWeeks(applicationId);
            Set<Integer> graded = new HashSet<>(dao.findGradedWeeks(applicationId));
            StringJoiner missing = new StringJoiner(", ");
            for (int week = 1; week <= duration; week++) {
                if (!graded.contains(week)) {
                    missing.add(String.valueOf(week));
                }
            }
            throw new ServiceException("Cannot complete yet. Every week must be submitted and graded. "
                    + "Weeks not graded: " + missing + ".");
        } catch (SQLException e) {
            throw dbError(e);
        }
    }

    private static ServiceException dbError(SQLException e) {
        String state = e.getSQLState();
        if (state != null && state.startsWith("08")) {   // connection problems
            return new ServiceException(DB_ERROR, e);
        }
        return new ServiceException("Database error: " + e.getMessage(), e);
    }
}

package com.internportal.dao;

import com.internportal.model.ApplicationStatus;
import com.internportal.model.LogbookInternship;
import com.internportal.model.LogbookRecord;
import com.internportal.model.LogbookWeek;
import com.internportal.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LogbookDAO {

    private static final String INTERNSHIP_SELECT =
            "SELECT a.application_id, i.internship_id, i.title, c.company_name, i.start_date, "
          + "i.duration_weeks, a.status "
          + "FROM applications a "
          + "JOIN internships i ON i.internship_id = a.internship_id "
          + "JOIN companies c ON c.company_id = i.company_id "
          + "WHERE a.student_id = ? AND a.status IN ('SELECTED', 'COMPLETED')";

    // ---------------------------------------------------------------- Student side

    /** The student's selected and completed internships, newest first. */
    public List<LogbookInternship> findInternshipsFor(int studentId) throws SQLException {
        String sql = INTERNSHIP_SELECT + " ORDER BY a.applied_on DESC, a.application_id DESC";
        List<LogbookInternship> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapInternship(rs));
                }
            }
        }
        return list;
    }

    /** One application of this student. The student_id condition means nobody can reach another student's. */
    public Optional<LogbookInternship> findInternship(int applicationId, int studentId) throws SQLException {
        String sql = INTERNSHIP_SELECT + " AND a.application_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, applicationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapInternship(rs));
                }
            }
        }
        return Optional.empty();
    }

    /** The entries written so far for an application (only the weeks that have one), ordered by week. */
    public List<LogbookWeek> findEntries(int applicationId) throws SQLException {
        String sql = "SELECT week_no, summary, grade, remarks, submitted_on "
                   + "FROM logbook_entries WHERE application_id = ? ORDER BY week_no";
        List<LogbookWeek> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, applicationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LogbookWeek w = new LogbookWeek();
                    w.setWeekNo(rs.getInt("week_no"));
                    w.setSummary(rs.getString("summary"));
                    w.setGrade(readGrade(rs));
                    w.setRemarks(rs.getString("remarks"));
                    w.setSubmittedOn(toLocal(rs.getTimestamp("submitted_on")));
                    list.add(w);
                }
            }
        }
        return list;
    }

    /**
     * Saves a week's entry: updates it if it exists and is not graded yet, otherwise inserts it.
     * Returns false if the entry exists but is already graded (it is locked).
     * A race between two saves of the same new week throws SQLException with error code 1062.
     */
    public boolean saveEntry(int applicationId, int weekNo, String summary) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE logbook_entries SET summary = ?, submitted_on = CURRENT_TIMESTAMP "
                  + "WHERE application_id = ? AND week_no = ? AND grade IS NULL")) {
                ps.setString(1, summary);
                ps.setInt(2, applicationId);
                ps.setInt(3, weekNo);
                if (ps.executeUpdate() > 0) {
                    return true;
                }
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT 1 FROM logbook_entries WHERE application_id = ? AND week_no = ?")) {
                ps.setInt(1, applicationId);
                ps.setInt(2, weekNo);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return false;   // it exists but was not updated, so it is graded
                    }
                }
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO logbook_entries (application_id, week_no, summary) VALUES (?, ?, ?)")) {
                ps.setInt(1, applicationId);
                ps.setInt(2, weekNo);
                ps.setString(3, summary);
                ps.executeUpdate();
                return true;
            }
        }
    }

    // ---------------------------------------------------------------- Admin side

    /**
     * Submitted entries, ungraded first and then newest first.
     * internshipId 0 means all internships; graded null means all, false = awaiting grade, true = graded.
     */
    public List<LogbookRecord> findRecords(int internshipId, Boolean graded) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT e.entry_id, e.application_id, u.full_name, i.title, c.company_name, e.week_no, "
              + "e.summary, e.submitted_on, e.grade, e.remarks, e.graded_on, a.status, i.duration_weeks "
              + "FROM logbook_entries e "
              + "JOIN applications a ON a.application_id = e.application_id "
              + "JOIN users u ON u.user_id = a.student_id "
              + "JOIN internships i ON i.internship_id = a.internship_id "
              + "JOIN companies c ON c.company_id = i.company_id "
              + "WHERE 1 = 1");
        if (internshipId > 0) {
            sql.append(" AND a.internship_id = ?");
        }
        if (graded != null) {
            sql.append(graded ? " AND e.grade IS NOT NULL" : " AND e.grade IS NULL");
        }
        sql.append(" ORDER BY (e.grade IS NULL) DESC, e.submitted_on DESC, e.entry_id DESC");

        List<LogbookRecord> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            if (internshipId > 0) {
                ps.setInt(1, internshipId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LogbookRecord r = new LogbookRecord();
                    r.setEntryId(rs.getInt("entry_id"));
                    r.setApplicationId(rs.getInt("application_id"));
                    r.setStudentName(rs.getString("full_name"));
                    r.setInternshipTitle(rs.getString("title"));
                    r.setCompanyName(rs.getString("company_name"));
                    r.setWeekNo(rs.getInt("week_no"));
                    r.setSummary(rs.getString("summary"));
                    r.setSubmittedOn(toLocal(rs.getTimestamp("submitted_on")));
                    r.setGrade(readGrade(rs));
                    r.setRemarks(rs.getString("remarks"));
                    r.setGradedOn(toLocal(rs.getTimestamp("graded_on")));
                    r.setApplicationStatus(ApplicationStatus.valueOf(rs.getString("status")));
                    r.setDurationWeeks(rs.getInt("duration_weeks"));
                    list.add(r);
                }
            }
        }
        return list;
    }

    /** Saves a grade, but only while the internship is still SELECTED. Returns false if it could not be saved. */
    public boolean grade(int entryId, int grade, String remarks, int adminId) throws SQLException {
        String sql = "UPDATE logbook_entries e JOIN applications a ON a.application_id = e.application_id "
                   + "SET e.grade = ?, e.remarks = ?, e.graded_by = ?, e.graded_on = CURRENT_TIMESTAMP "
                   + "WHERE e.entry_id = ? AND a.status = 'SELECTED'";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, grade);
            ps.setString(2, remarks);
            ps.setInt(3, adminId);
            ps.setInt(4, entryId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Marks the application COMPLETED and records it in application_history, in one transaction, but only if
     * it is SELECTED and every week of the internship has a graded entry. Returns false otherwise.
     */
    public boolean complete(int applicationId) throws SQLException {
        String update = "UPDATE applications a JOIN internships i ON i.internship_id = a.internship_id "
                      + "SET a.status = 'COMPLETED' "
                      + "WHERE a.application_id = ? AND a.status = 'SELECTED' "
                      + "AND (SELECT COUNT(*) FROM logbook_entries e "
                      + "     WHERE e.application_id = a.application_id AND e.grade IS NOT NULL) >= i.duration_weeks";
        String history = "INSERT INTO application_history (application_id, status) VALUES (?, 'COMPLETED')";

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                int changed;
                try (PreparedStatement ps = con.prepareStatement(update)) {
                    ps.setInt(1, applicationId);
                    changed = ps.executeUpdate();
                }
                if (changed == 0) {
                    con.rollback();
                    return false;
                }
                try (PreparedStatement ps = con.prepareStatement(history)) {
                    ps.setInt(1, applicationId);
                    ps.executeUpdate();
                }
                con.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    /** Used to explain why an application could not be completed. */
    public Optional<ApplicationStatus> findApplicationStatus(int applicationId) throws SQLException {
        String sql = "SELECT status FROM applications WHERE application_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, applicationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(ApplicationStatus.valueOf(rs.getString(1)));
                }
            }
        }
        return Optional.empty();
    }

    /** The internship's length in weeks, or 0 if the application does not exist. */
    public int findDurationWeeks(int applicationId) throws SQLException {
        String sql = "SELECT i.duration_weeks FROM applications a "
                   + "JOIN internships i ON i.internship_id = a.internship_id WHERE a.application_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, applicationId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public List<Integer> findGradedWeeks(int applicationId) throws SQLException {
        String sql = "SELECT week_no FROM logbook_entries "
                   + "WHERE application_id = ? AND grade IS NOT NULL ORDER BY week_no";
        List<Integer> weeks = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, applicationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    weeks.add(rs.getInt(1));
                }
            }
        }
        return weeks;
    }

    // ---------------------------------------------------------------- Helpers

    private LogbookInternship mapInternship(ResultSet rs) throws SQLException {
        LogbookInternship x = new LogbookInternship();
        x.setApplicationId(rs.getInt("application_id"));
        x.setInternshipId(rs.getInt("internship_id"));
        x.setTitle(rs.getString("title"));
        x.setCompanyName(rs.getString("company_name"));
        x.setStartDate(rs.getDate("start_date").toLocalDate());
        x.setDurationWeeks(rs.getInt("duration_weeks"));
        x.setStatus(ApplicationStatus.valueOf(rs.getString("status")));
        return x;
    }

    private static Integer readGrade(ResultSet rs) throws SQLException {
        int value = rs.getInt("grade");
        return rs.wasNull() ? null : value;
    }

    private static LocalDateTime toLocal(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}

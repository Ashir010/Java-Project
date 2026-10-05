package com.internportal.dao;

import com.internportal.model.ApplicationStatus;
import com.internportal.model.MyApplication;
import com.internportal.model.StatusChange;
import com.internportal.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/** Queries for a student's own applications. */
public class MyApplicationDAO {

    /** All applications of one student, newest first, each with the date of its latest status change. */
    public List<MyApplication> findByStudent(int studentId) throws SQLException {
        String sql = "SELECT a.application_id, a.internship_id, i.title, c.company_name, "
                   + "a.applied_on, a.status, COALESCE(h.last_change, a.applied_on) AS last_update "
                   + "FROM applications a "
                   + "JOIN internships i ON i.internship_id = a.internship_id "
                   + "JOIN companies c ON c.company_id = i.company_id "
                   + "LEFT JOIN (SELECT application_id, MAX(changed_on) AS last_change "
                   + "           FROM application_history GROUP BY application_id) h "
                   + "       ON h.application_id = a.application_id "
                   + "WHERE a.student_id = ? "
                   + "ORDER BY a.applied_on DESC, a.application_id DESC";
        List<MyApplication> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MyApplication a = new MyApplication();
                    a.setApplicationId(rs.getInt("application_id"));
                    a.setInternshipId(rs.getInt("internship_id"));
                    a.setInternshipTitle(rs.getString("title"));
                    a.setCompanyName(rs.getString("company_name"));
                    Timestamp appliedOn = rs.getTimestamp("applied_on");
                    a.setAppliedOn(appliedOn == null ? null : appliedOn.toLocalDateTime());
                    Timestamp lastUpdate = rs.getTimestamp("last_update");
                    a.setLastUpdate(lastUpdate == null ? null : lastUpdate.toLocalDateTime());
                    a.setStatus(ApplicationStatus.valueOf(rs.getString("status")));
                    list.add(a);
                }
            }
        }
        return list;
    }

    /**
     * Status changes of one application, oldest first. The student_id condition makes sure a student
     * can only ever read the history of their own application.
     */
    public List<StatusChange> findHistory(int applicationId, int studentId) throws SQLException {
        String sql = "SELECT h.status, h.changed_on "
                   + "FROM application_history h "
                   + "JOIN applications a ON a.application_id = h.application_id "
                   + "WHERE h.application_id = ? AND a.student_id = ? "
                   + "ORDER BY h.changed_on, h.history_id";
        List<StatusChange> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, applicationId);
            ps.setInt(2, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp changedOn = rs.getTimestamp("changed_on");
                    list.add(new StatusChange(
                            ApplicationStatus.valueOf(rs.getString("status")),
                            changedOn == null ? null : changedOn.toLocalDateTime()));
                }
            }
        }
        return list;
    }
}

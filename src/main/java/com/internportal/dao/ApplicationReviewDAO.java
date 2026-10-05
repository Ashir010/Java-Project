package com.internportal.dao;

import com.internportal.model.ApplicationRecord;
import com.internportal.model.ApplicationStatus;
import com.internportal.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/** Admin-side queries on applications. (The student side is in ApplicationDAO.) */
public class ApplicationReviewDAO {

    private static final String SELECT_BASE =
            "SELECT a.application_id, a.student_id, u.full_name, u.email, s.phone, s.course, s.skills, s.resume_path, "
          + "a.internship_id, i.title, c.company_name, a.applied_on, a.status "
          + "FROM applications a "
          + "JOIN users u ON u.user_id = a.student_id "
          + "JOIN students s ON s.student_id = a.student_id "
          + "JOIN internships i ON i.internship_id = a.internship_id "
          + "JOIN companies c ON c.company_id = i.company_id "
          + "WHERE 1 = 1";

    /** internshipId 0 means all internships; status null means all statuses. Newest first. */
    public List<ApplicationRecord> find(int internshipId, ApplicationStatus status) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_BASE);
        if (internshipId > 0) {
            sql.append(" AND a.internship_id = ?");
        }
        if (status != null) {
            sql.append(" AND a.status = ?");
        }
        sql.append(" ORDER BY a.applied_on DESC, a.application_id DESC");

        List<ApplicationRecord> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            int index = 1;
            if (internshipId > 0) {
                ps.setInt(index++, internshipId);
            }
            if (status != null) {
                ps.setString(index, status.name());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    /**
     * Changes the status only if it is still the one the admin saw (expected), and records the change
     * in application_history, in one transaction. Returns false if someone changed it in the meantime.
     */
    public boolean updateStatus(int applicationId, ApplicationStatus expected, ApplicationStatus next)
            throws SQLException {
        String update = "UPDATE applications SET status = ? WHERE application_id = ? AND status = ?";
        String history = "INSERT INTO application_history (application_id, status) VALUES (?, ?)";

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                int changed;
                try (PreparedStatement ps = con.prepareStatement(update)) {
                    ps.setString(1, next.name());
                    ps.setInt(2, applicationId);
                    ps.setString(3, expected.name());
                    changed = ps.executeUpdate();
                }
                if (changed == 0) {
                    con.rollback();
                    return false;
                }
                try (PreparedStatement ps = con.prepareStatement(history)) {
                    ps.setInt(1, applicationId);
                    ps.setString(2, next.name());
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

    private ApplicationRecord map(ResultSet rs) throws SQLException {
        ApplicationRecord r = new ApplicationRecord();
        r.setApplicationId(rs.getInt("application_id"));
        r.setStudentId(rs.getInt("student_id"));
        r.setStudentName(rs.getString("full_name"));
        r.setStudentEmail(rs.getString("email"));
        r.setPhone(rs.getString("phone"));
        r.setCourse(rs.getString("course"));
        r.setSkills(rs.getString("skills"));
        r.setResumePath(rs.getString("resume_path"));
        r.setInternshipId(rs.getInt("internship_id"));
        r.setInternshipTitle(rs.getString("title"));
        r.setCompanyName(rs.getString("company_name"));
        Timestamp appliedOn = rs.getTimestamp("applied_on");
        r.setAppliedOn(appliedOn == null ? null : appliedOn.toLocalDateTime());
        r.setStatus(ApplicationStatus.valueOf(rs.getString("status")));
        return r;
    }
}

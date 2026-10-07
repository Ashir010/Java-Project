package com.internportal.dao;

import com.internportal.model.Internship;
import com.internportal.model.InternshipListing;
import com.internportal.model.InternshipStatus;
import com.internportal.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ApplicationDAO {

    /** OPEN internships whose deadline has not passed, each flagged if this student already applied. */
    public List<InternshipListing> findAvailableFor(int studentId) throws SQLException {
        String sql = "SELECT i.internship_id, i.company_id, c.company_name, i.title, i.description, "
                   + "i.stipend, i.duration_weeks, i.start_date, i.deadline, i.status, "
                   + "(a.application_id IS NOT NULL) AS applied "
                   + "FROM internships i "
                   + "JOIN companies c ON c.company_id = i.company_id "
                   + "LEFT JOIN applications a ON a.internship_id = i.internship_id AND a.student_id = ? "
                   + "WHERE i.status = 'OPEN' AND i.deadline >= CURDATE() "
                   + "ORDER BY i.deadline, i.title";
        List<InternshipListing> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Internship i = new Internship();
                    i.setInternshipId(rs.getInt("internship_id"));
                    i.setCompanyId(rs.getInt("company_id"));
                    i.setCompanyName(rs.getString("company_name"));
                    i.setTitle(rs.getString("title"));
                    i.setDescription(rs.getString("description"));
                    i.setStipend(rs.getBigDecimal("stipend"));
                    i.setDurationWeeks(rs.getInt("duration_weeks"));
                    i.setStartDate(rs.getDate("start_date").toLocalDate());
                    i.setDeadline(rs.getDate("deadline").toLocalDate());
                    i.setStatus(InternshipStatus.valueOf(rs.getString("status")));
                    list.add(new InternshipListing(i, rs.getBoolean("applied")));
                }
            }
        }
        return list;
    }

    /**
     * Saves the application only if the internship is still OPEN and before its deadline,
     * in one atomic statement. Returns false if the internship is no longer available.
     * A second application to the same internship throws SQLException with error code 1062.
     */
    public boolean insertIfAvailable(int studentId, int internshipId) throws SQLException {
        String sql = "INSERT INTO applications (student_id, internship_id) "
                   + "SELECT ?, i.internship_id FROM internships i "
                   + "WHERE i.internship_id = ? AND i.status = 'OPEN' AND i.deadline >= CURDATE()";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, internshipId);
            return ps.executeUpdate() > 0;
        }
    }
}

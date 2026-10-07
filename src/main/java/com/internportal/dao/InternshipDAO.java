package com.internportal.dao;

import com.internportal.model.Internship;
import com.internportal.model.InternshipStatus;
import com.internportal.util.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class InternshipDAO {

    public List<Internship> findAll() throws SQLException {
        String sql = "SELECT i.internship_id, i.company_id, c.company_name, i.title, i.description, "
                   + "i.stipend, i.duration_weeks, i.start_date, i.deadline, i.status "
                   + "FROM internships i JOIN companies c ON c.company_id = i.company_id "
                   + "ORDER BY i.internship_id DESC";
        List<Internship> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    public int insert(Internship i) throws SQLException {
        String sql = "INSERT INTO internships (company_id, title, description, stipend, "
                   + "duration_weeks, start_date, deadline, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, i);
            ps.setString(8, i.getStatus().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    /** Updates everything except the status (use updateStatus for that). */
    public void update(Internship i) throws SQLException {
        String sql = "UPDATE internships SET company_id = ?, title = ?, description = ?, "
                   + "stipend = ?, duration_weeks = ?, start_date = ?, deadline = ? "
                   + "WHERE internship_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, i);
            ps.setInt(8, i.getInternshipId());
            ps.executeUpdate();
        }
    }

    public void updateStatus(int internshipId, InternshipStatus status) throws SQLException {
        String sql = "UPDATE internships SET status = ? WHERE internship_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, internshipId);
            ps.executeUpdate();
        }
    }

    private void bind(PreparedStatement ps, Internship i) throws SQLException {
        ps.setInt(1, i.getCompanyId());
        ps.setString(2, i.getTitle());
        ps.setString(3, i.getDescription());
        ps.setBigDecimal(4, i.getStipend());
        ps.setInt(5, i.getDurationWeeks());
        ps.setDate(6, Date.valueOf(i.getStartDate()));
        ps.setDate(7, Date.valueOf(i.getDeadline()));
    }

    private Internship map(ResultSet rs) throws SQLException {
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
        return i;
    }
}

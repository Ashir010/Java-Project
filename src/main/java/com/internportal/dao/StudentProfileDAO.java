package com.internportal.dao;

import com.internportal.model.StudentProfile;
import com.internportal.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class StudentProfileDAO {

    public Optional<StudentProfile> find(int studentId) throws SQLException {
        String sql = "SELECT u.user_id, u.full_name, u.email, s.phone, s.course, s.skills, s.resume_path "
                   + "FROM users u JOIN students s ON s.student_id = u.user_id "
                   + "WHERE u.user_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    StudentProfile p = new StudentProfile();
                    p.setStudentId(rs.getInt("user_id"));
                    p.setFullName(rs.getString("full_name"));
                    p.setEmail(rs.getString("email"));
                    p.setPhone(rs.getString("phone"));
                    p.setCourse(rs.getString("course"));
                    p.setSkills(rs.getString("skills"));
                    p.setResumePath(rs.getString("resume_path"));
                    return Optional.of(p);
                }
            }
        }
        return Optional.empty();
    }

    /** Updates the name (users) and the details (students) in one transaction. */
    public void update(int studentId, String fullName, String phone, String course, String skills)
            throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE users SET full_name = ? WHERE user_id = ?")) {
                    ps.setString(1, fullName);
                    ps.setInt(2, studentId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE students SET phone = ?, course = ?, skills = ? WHERE student_id = ?")) {
                    ps.setString(1, phone);
                    ps.setString(2, course);
                    ps.setString(3, skills);
                    ps.setInt(4, studentId);
                    ps.executeUpdate();
                }
                con.commit();
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    /** The stored resume path of a student, or null if none. */
    public String findResumePath(int studentId) throws SQLException {
        String sql = "SELECT resume_path FROM students WHERE student_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    public void updateResumePath(int studentId, String resumePath) throws SQLException {
        String sql = "UPDATE students SET resume_path = ? WHERE student_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, resumePath);
            ps.setInt(2, studentId);
            ps.executeUpdate();
        }
    }
}

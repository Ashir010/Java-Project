package com.internportal.dao;

import com.internportal.model.Student;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class StudentDAO {

    /** Inserts a student row using the caller's connection (part of the registration transaction). */
    public void insert(Connection con, Student student) throws SQLException {
        String sql = "INSERT INTO students (student_id, phone, course, skills, resume_path) "
                   + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, student.getStudentId());
            ps.setString(2, student.getPhone());
            ps.setString(3, student.getCourse());
            ps.setString(4, student.getSkills());
            ps.setString(5, student.getResumePath());
            ps.executeUpdate();
        }
    }
}

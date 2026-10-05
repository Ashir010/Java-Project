package com.internportal.service;

import com.internportal.dao.StudentProfileDAO;
import com.internportal.model.ResumeData;
import com.internportal.model.StudentProfile;
import com.internportal.util.Courses;
import com.internportal.util.ResumeParser;
import com.internportal.util.ResumeStorage;
import com.internportal.util.Validator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Optional;

/** Business rules for a student's profile and resume. */
public class ProfileService {

    private static final String DB_ERROR =
            "Cannot reach the database. Check that MySQL is running and db.properties is correct.";
    private static final int MAX_SKILLS_LENGTH = 1000;

    private final StudentProfileDAO dao = new StudentProfileDAO();

    public StudentProfile load(int studentId) throws ServiceException {
        try {
            Optional<StudentProfile> profile = dao.find(studentId);
            if (!profile.isPresent()) {
                throw new ServiceException("Your profile could not be found. Please contact the admin.");
            }
            return profile.get();
        } catch (SQLException e) {
            throw dbError(e);
        }
    }

    public void save(int studentId, String fullName, String phone, String course, String skills)
            throws ServiceException {
        String name = fullName == null ? "" : fullName.trim();
        String mobile = phone == null ? "" : phone.trim();
        // Skills typed on separate lines become a comma-separated list
        String cleanSkills = skills == null ? "" : skills.trim().replaceAll("\\s*[\\r\\n]+\\s*", ", ");

        if (!Validator.isValidName(name)) {
            throw new ServiceException("Please enter a valid full name.");
        }
        if (!Validator.isValidPhone(mobile)) {
            throw new ServiceException("Enter a valid 10-digit mobile number.");
        }
        if (!Courses.isValid(course)) {
            throw new ServiceException("Please choose a course from the list.");
        }
        if (cleanSkills.length() > MAX_SKILLS_LENGTH) {
            throw new ServiceException("Skills must be " + MAX_SKILLS_LENGTH + " characters or fewer.");
        }

        try {
            dao.update(studentId, name, mobile, course, cleanSkills.isEmpty() ? null : cleanSkills);
        } catch (SQLException e) {
            throw dbError(e);
        }
    }

    /** Validates and stores a new resume, replaces the old one, and returns the stored path. */
    public String uploadResume(int studentId, File file) throws ServiceException {
        String newPath;
        try {
            newPath = ResumeStorage.save(studentId, file);
        } catch (IllegalArgumentException e) {
            throw new ServiceException(e.getMessage(), e);
        } catch (IOException e) {
            throw new ServiceException("Could not save the file: " + e.getMessage(), e);
        }

        String oldPath;
        try {
            oldPath = dao.findResumePath(studentId);
            dao.updateResumePath(studentId, newPath);
        } catch (SQLException e) {
            ResumeStorage.delete(newPath);   // do not leave an orphan file behind
            throw dbError(e);
        }

        if (oldPath != null && !oldPath.equals(newPath)) {
            ResumeStorage.delete(oldPath);
        }
        return newPath;
    }

    /** Reads the student's stored resume and extracts the phone number and skills. */
    public ResumeData readResume(int studentId) throws ServiceException {
        String stored;
        try {
            stored = dao.findResumePath(studentId);
        } catch (SQLException e) {
            throw dbError(e);
        }
        if (stored == null || stored.trim().isEmpty()) {
            throw new ServiceException("Upload your resume first.");
        }
        Path file = ResumeStorage.resolve(stored);
        if (file == null || !Files.exists(file)) {
            throw new ServiceException("Your resume file could not be found. Please upload it again.");
        }
        try {
            return ResumeParser.fromFile(file);
        } catch (Exception e) {
            throw new ServiceException("Could not read text from this resume. Try another file.", e);
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

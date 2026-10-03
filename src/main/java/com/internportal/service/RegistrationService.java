package com.internportal.service;

import com.internportal.dao.StudentDAO;
import com.internportal.dao.UserDAO;
import com.internportal.model.Role;
import com.internportal.model.Student;
import com.internportal.model.User;
import com.internportal.util.DBConnection;
import com.internportal.util.PasswordUtil;
import com.internportal.util.Validator;

import java.sql.Connection;
import java.sql.SQLException;

public class RegistrationService {

    private static final String DB_ERROR =
            "Cannot reach the database. Check that MySQL is running and db.properties is correct.";
    private static final String DUPLICATE_EMAIL = "An account with this email already exists.";
    private static final int MYSQL_DUPLICATE_ENTRY = 1062;

    private final UserDAO userDAO = new UserDAO();
    private final StudentDAO studentDAO = new StudentDAO();

    /** Creates a STUDENT account. Throws AuthException with a user-friendly message on any problem. */
    public void registerStudent(String fullName, String email, String phone,
                                String course, String password) throws AuthException {
        String name = fullName.trim();
        String mail = email.trim().toLowerCase();
        String mobile = phone.trim();

        // Validate again here so the rules hold even if the UI changes
        if (!Validator.isValidName(name)) {
            throw new AuthException("Please enter a valid full name.");
        }
        if (!Validator.isValidEmail(mail)) {
            throw new AuthException("Please enter a valid email address.");
        }
        if (!Validator.isValidPhone(mobile)) {
            throw new AuthException("Enter a valid 10-digit mobile number.");
        }
        String passwordError = Validator.passwordError(password);
        if (passwordError != null) {
            throw new AuthException(passwordError);
        }

        try {
            if (userDAO.emailExists(mail)) {
                throw new AuthException(DUPLICATE_EMAIL);
            }

            User user = new User();
            user.setFullName(name);
            user.setEmail(mail);
            user.setPasswordHash(PasswordUtil.hash(password));
            user.setRole(Role.STUDENT);

            // One transaction: either both rows are saved, or neither
            try (Connection con = DBConnection.getConnection()) {
                con.setAutoCommit(false);
                try {
                    int userId = userDAO.insert(con, user);
                    if (userId <= 0) {
                        throw new SQLException("Could not create the user record.");
                    }

                    Student student = new Student();
                    student.setStudentId(userId);
                    student.setPhone(mobile);
                    student.setCourse(course);
                    studentDAO.insert(con, student);

                    con.commit();
                } catch (SQLException | RuntimeException e) {
                    con.rollback();
                    throw e;
                }
            }
        } catch (SQLException e) {
            // Two people registering the same email at the same moment
            if (e.getErrorCode() == MYSQL_DUPLICATE_ENTRY) {
                throw new AuthException(DUPLICATE_EMAIL, e);
            }
            throw new AuthException(DB_ERROR, e);
        }
    }
}

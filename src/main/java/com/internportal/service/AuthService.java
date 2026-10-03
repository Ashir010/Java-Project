package com.internportal.service;

import com.internportal.dao.UserDAO;
import com.internportal.model.Role;
import com.internportal.model.User;
import com.internportal.util.PasswordUtil;

import java.sql.SQLException;
import java.util.Optional;

public class AuthService {

    // Default admin created on first run. Change this password after your first login.
    public static final String DEFAULT_ADMIN_EMAIL = "admin@portal.com";
    private static final String DEFAULT_ADMIN_PASSWORD = "Admin@123";

    private static final String DB_ERROR =
            "Cannot reach the database. Check that MySQL is running and db.properties is correct.";

    private final UserDAO userDAO = new UserDAO();

    /** Verifies the credentials and returns the user, or throws AuthException with a friendly message. */
    public User login(String email, String password) throws AuthException {
        try {
            Optional<User> found = userDAO.findByEmail(email.trim().toLowerCase());

            // Same message for "no such email" and "wrong password" so we don't reveal which emails exist
            if (!found.isPresent() || !PasswordUtil.verify(password, found.get().getPasswordHash())) {
                throw new AuthException("Invalid email or password.");
            }

            User user = found.get();
            if (!user.isActive()) {
                throw new AuthException("Your account is deactivated. Please contact the admin.");
            }

            user.setPasswordHash(null);   // do not keep the hash in memory after login
            return user;
        } catch (SQLException e) {
            throw new AuthException(DB_ERROR, e);
        }
    }

    /** Creates the default admin account if no admin exists yet. Safe to call on every startup. */
    public void ensureDefaultAdmin() throws AuthException {
        try {
            if (!userDAO.adminExists()) {
                User admin = new User();
                admin.setFullName("System Admin");
                admin.setEmail(DEFAULT_ADMIN_EMAIL);
                admin.setPasswordHash(PasswordUtil.hash(DEFAULT_ADMIN_PASSWORD));
                admin.setRole(Role.ADMIN);
                userDAO.insert(admin);
            }
        } catch (SQLException e) {
            throw new AuthException(DB_ERROR, e);
        }
    }
}

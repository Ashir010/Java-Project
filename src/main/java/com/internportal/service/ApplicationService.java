package com.internportal.service;

import com.internportal.dao.ApplicationDAO;
import com.internportal.model.InternshipListing;

import java.sql.SQLException;
import java.util.List;

/** Business rules for students browsing and applying to internships. */
public class ApplicationService {

    private static final String DB_ERROR =
            "Cannot reach the database. Check that MySQL is running and db.properties is correct.";
    private static final int MYSQL_DUPLICATE_ENTRY = 1062;

    private final ApplicationDAO applicationDAO = new ApplicationDAO();

    public List<InternshipListing> listAvailable(int studentId) throws ServiceException {
        try {
            return applicationDAO.findAvailableFor(studentId);
        } catch (SQLException e) {
            throw dbError(e);
        }
    }

    /** Applies for an internship. Re-checks that it is still open, since the student's list may be stale. */
    public void apply(int studentId, int internshipId) throws ServiceException {
        try {
            boolean saved = applicationDAO.insertIfAvailable(studentId, internshipId);
            if (!saved) {
                throw new ServiceException("This internship is no longer open for applications.");
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == MYSQL_DUPLICATE_ENTRY) {
                throw new ServiceException("You have already applied to this internship.", e);
            }
            throw dbError(e);
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

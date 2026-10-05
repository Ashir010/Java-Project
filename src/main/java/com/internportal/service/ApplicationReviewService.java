package com.internportal.service;

import com.internportal.dao.ApplicationReviewDAO;
import com.internportal.model.ApplicationRecord;
import com.internportal.model.ApplicationStatus;

import java.sql.SQLException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

/** Business rules for the admin reviewing applications: who can move to which status. */
public class ApplicationReviewService {

    private static final String DB_ERROR =
            "Cannot reach the database. Check that MySQL is running and db.properties is correct.";

    private static final Map<ApplicationStatus, Set<ApplicationStatus>> ALLOWED =
            new EnumMap<>(ApplicationStatus.class);

    static {
        ALLOWED.put(ApplicationStatus.APPLIED,
                EnumSet.of(ApplicationStatus.SHORTLISTED, ApplicationStatus.REJECTED));
        ALLOWED.put(ApplicationStatus.SHORTLISTED,
                EnumSet.of(ApplicationStatus.SELECTED, ApplicationStatus.REJECTED));
        // A selected student can still be rejected (offer withdrawn).
        // COMPLETED is set later, from the Logbooks screen.
        ALLOWED.put(ApplicationStatus.SELECTED, EnumSet.of(ApplicationStatus.REJECTED));
        ALLOWED.put(ApplicationStatus.REJECTED, EnumSet.noneOf(ApplicationStatus.class));
        ALLOWED.put(ApplicationStatus.COMPLETED, EnumSet.noneOf(ApplicationStatus.class));
    }

    private final ApplicationReviewDAO dao = new ApplicationReviewDAO();

    /** internshipId 0 means all internships; status null means all statuses. */
    public List<ApplicationRecord> listApplications(int internshipId, ApplicationStatus status)
            throws ServiceException {
        try {
            return dao.find(internshipId, status);
        } catch (SQLException e) {
            throw dbError(e);
        }
    }

    public void changeStatus(int applicationId, ApplicationStatus current, ApplicationStatus next)
            throws ServiceException {
        if (!isAllowed(current, next)) {
            throw new ServiceException(transitionMessage(current, next));
        }
        try {
            boolean changed = dao.updateStatus(applicationId, current, next);
            if (!changed) {
                throw new ServiceException(
                        "This application was changed by someone else. The list has been reloaded.");
            }
        } catch (SQLException e) {
            throw dbError(e);
        }
    }

    public static boolean isAllowed(ApplicationStatus from, ApplicationStatus to) {
        return ALLOWED.get(from).contains(to);
    }

    /** A friendly explanation of why a change is not allowed, and what is. */
    public static String transitionMessage(ApplicationStatus from, ApplicationStatus to) {
        Set<ApplicationStatus> next = ALLOWED.get(from);
        if (next.isEmpty()) {
            return "The application is " + from + ", which is final and cannot be changed.";
        }
        StringJoiner names = new StringJoiner(" or ");
        for (ApplicationStatus s : next) {
            names.add(s.name());
        }
        return "The application is " + from + ", so it cannot be changed to " + to
                + ". Next step: " + names + ".";
    }

    private static ServiceException dbError(SQLException e) {
        String state = e.getSQLState();
        if (state != null && state.startsWith("08")) {   // connection problems
            return new ServiceException(DB_ERROR, e);
        }
        return new ServiceException("Database error: " + e.getMessage(), e);
    }
}

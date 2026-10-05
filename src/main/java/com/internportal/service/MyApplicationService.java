package com.internportal.service;

import com.internportal.dao.MyApplicationDAO;
import com.internportal.model.ApplicationStatus;
import com.internportal.model.MyApplication;
import com.internportal.model.StatusChange;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Business rules for a student viewing and tracking their own applications. */
public class MyApplicationService {

    private static final String DB_ERROR =
            "Cannot reach the database. Check that MySQL is running and db.properties is correct.";

    private final MyApplicationDAO dao = new MyApplicationDAO();

    public List<MyApplication> listForStudent(int studentId) throws ServiceException {
        try {
            return dao.findByStudent(studentId);
        } catch (SQLException e) {
            throw dbError(e);
        }
    }

    /**
     * The timeline of one application: "Applied" first, then every recorded status change.
     * If the current status is missing from the history (for example it was changed directly in the
     * database), it is added as the last step with no date, so the timeline always ends at the truth.
     */
    public List<StatusChange> timeline(int studentId, MyApplication application) throws ServiceException {
        try {
            List<StatusChange> steps = new ArrayList<>();
            steps.add(new StatusChange(ApplicationStatus.APPLIED, application.getAppliedOn()));
            steps.addAll(dao.findHistory(application.getApplicationId(), studentId));

            ApplicationStatus last = steps.get(steps.size() - 1).getStatus();
            if (last != application.getStatus()) {
                steps.add(new StatusChange(application.getStatus(), null));
            }
            return steps;
        } catch (SQLException e) {
            throw dbError(e);
        }
    }

    /** The stages still ahead for an application in this status. Final statuses have none. */
    public static List<ApplicationStatus> upcomingSteps(ApplicationStatus current) {
        switch (current) {
            case APPLIED:
                return Arrays.asList(ApplicationStatus.SHORTLISTED, ApplicationStatus.SELECTED,
                        ApplicationStatus.COMPLETED);
            case SHORTLISTED:
                return Arrays.asList(ApplicationStatus.SELECTED, ApplicationStatus.COMPLETED);
            case SELECTED:
                return Collections.singletonList(ApplicationStatus.COMPLETED);
            default:
                return Collections.emptyList();
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

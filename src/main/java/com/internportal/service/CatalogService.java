package com.internportal.service;

import com.internportal.dao.CompanyDAO;
import com.internportal.dao.InternshipDAO;
import com.internportal.model.Company;
import com.internportal.model.Internship;
import com.internportal.model.InternshipStatus;
import com.internportal.util.Validator;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** Business rules for companies and internships. */
public class CatalogService {

    private static final String DB_ERROR =
            "Cannot reach the database. Check that MySQL is running and db.properties is correct.";
    private static final int MYSQL_DUPLICATE_ENTRY = 1062;
    private static final int MYSQL_PARENT_ROW_REFERENCED = 1451;
    private static final BigDecimal MAX_STIPEND = new BigDecimal("9999999.99");

    private final CompanyDAO companyDAO = new CompanyDAO();
    private final InternshipDAO internshipDAO = new InternshipDAO();

    // ---------------------------------------------------------------- Companies

    public List<Company> listCompanies() throws ServiceException {
        try {
            return companyDAO.findAll();
        } catch (SQLException e) {
            throw dbError(e);
        }
    }

    public void addCompany(Company c) throws ServiceException {
        cleanAndValidate(c);
        try {
            companyDAO.insert(c);
        } catch (SQLException e) {
            throw companyError(e);
        }
    }

    public void updateCompany(Company c) throws ServiceException {
        cleanAndValidate(c);
        try {
            companyDAO.update(c);
        } catch (SQLException e) {
            throw companyError(e);
        }
    }

    public void deleteCompany(int companyId) throws ServiceException {
        try {
            companyDAO.delete(companyId);
        } catch (SQLException e) {
            if (e.getErrorCode() == MYSQL_PARENT_ROW_REFERENCED) {
                throw new ServiceException("This company has internships, so it cannot be deleted.", e);
            }
            throw dbError(e);
        }
    }

    // ---------------------------------------------------------------- Internships

    public List<Internship> listInternships() throws ServiceException {
        try {
            return internshipDAO.findAll();
        } catch (SQLException e) {
            throw dbError(e);
        }
    }

    public void addInternship(Internship i) throws ServiceException {
        cleanAndValidate(i, true);
        try {
            internshipDAO.insert(i);
        } catch (SQLException e) {
            throw dbError(e);
        }
    }

    public void updateInternship(Internship i) throws ServiceException {
        cleanAndValidate(i, false);
        try {
            internshipDAO.update(i);
        } catch (SQLException e) {
            throw dbError(e);
        }
    }

    public void setInternshipStatus(int internshipId, InternshipStatus status) throws ServiceException {
        try {
            internshipDAO.updateStatus(internshipId, status);
        } catch (SQLException e) {
            throw dbError(e);
        }
    }

    // ---------------------------------------------------------------- Validation

    private void cleanAndValidate(Company c) throws ServiceException {
        c.setCompanyName(clean(c.getCompanyName()));
        c.setIndustry(clean(c.getIndustry()));
        c.setWebsite(clean(c.getWebsite()));
        c.setContactEmail(clean(c.getContactEmail()));

        String name = c.getCompanyName();
        if (name == null || name.length() < 2 || name.length() > 100) {
            throw new ServiceException("Company name must be 2 to 100 characters.");
        }
        if (c.getIndustry() != null && c.getIndustry().length() > 50) {
            throw new ServiceException("Industry must be 50 characters or fewer.");
        }
        if (c.getWebsite() != null && c.getWebsite().length() > 100) {
            throw new ServiceException("Website must be 100 characters or fewer.");
        }
        if (c.getContactEmail() != null && !Validator.isValidEmail(c.getContactEmail())) {
            throw new ServiceException("Please enter a valid contact email.");
        }
    }

    /** newPosting = true also rejects a deadline in the past. */
    private void cleanAndValidate(Internship i, boolean newPosting) throws ServiceException {
        i.setTitle(clean(i.getTitle()));
        i.setDescription(clean(i.getDescription()));

        if (i.getCompanyId() <= 0) {
            throw new ServiceException("Please choose a company.");
        }
        String title = i.getTitle();
        if (title == null || title.length() < 3 || title.length() > 100) {
            throw new ServiceException("Title must be 3 to 100 characters.");
        }
        if (i.getDescription() != null && i.getDescription().length() > 2000) {
            throw new ServiceException("Description must be 2000 characters or fewer.");
        }
        BigDecimal stipend = i.getStipend();
        if (stipend == null || stipend.signum() < 0 || stipend.compareTo(MAX_STIPEND) > 0) {
            throw new ServiceException("Enter a valid stipend (0 or more).");
        }
        if (i.getDurationWeeks() < 1 || i.getDurationWeeks() > 52) {
            throw new ServiceException("Duration must be between 1 and 52 weeks.");
        }
        if (i.getDeadline() == null) {
            throw new ServiceException("Please choose an application deadline.");
        }
        if (newPosting && i.getDeadline().isBefore(LocalDate.now())) {
            throw new ServiceException("The deadline cannot be in the past.");
        }
    }

    /** Trims the text; blank becomes null. */
    private static String clean(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    // ---------------------------------------------------------------- Errors

    private static ServiceException companyError(SQLException e) {
        if (e.getErrorCode() == MYSQL_DUPLICATE_ENTRY) {
            return new ServiceException("A company with this name already exists.", e);
        }
        return dbError(e);
    }

    private static ServiceException dbError(SQLException e) {
        String state = e.getSQLState();
        if (state != null && state.startsWith("08")) {   // connection problems
            return new ServiceException(DB_ERROR, e);
        }
        // Other problems (for example a missing table) are shown as they are, so they are easy to diagnose
        return new ServiceException("Database error: " + e.getMessage(), e);
    }
}

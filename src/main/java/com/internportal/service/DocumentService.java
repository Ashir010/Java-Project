package com.internportal.service;

import com.internportal.dao.DocumentDAO;
import com.internportal.model.ApplicationStatus;
import com.internportal.model.DocumentSource;
import com.internportal.model.DocumentType;
import com.internportal.model.IssuedDocument;
import com.internportal.util.DocumentStorage;
import com.internportal.util.PdfGenerator;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Business rules for issuing offer letters and completion certificates. */
public class DocumentService {

    private static final String DB_ERROR =
            "Cannot reach the database. Check that MySQL is running and db.properties is correct.";
    private static final int MYSQL_DUPLICATE_ENTRY = 1062;

    private final DocumentDAO dao = new DocumentDAO();

    public IssuedDocument generateOfferLetter(int applicationId, int adminId) throws ServiceException {
        return generate(DocumentType.OFFER_LETTER, applicationId, adminId);
    }

    public IssuedDocument generateCertificate(int applicationId, int adminId) throws ServiceException {
        return generate(DocumentType.COMPLETION_CERTIFICATE, applicationId, adminId);
    }

    public List<IssuedDocument> listAll() throws ServiceException {
        try {
            return dao.findAll();
        } catch (SQLException e) {
            throw dbError(e);
        }
    }

    public List<IssuedDocument> listForStudent(int studentId) throws ServiceException {
        try {
            return dao.findForStudent(studentId);
        } catch (SQLException e) {
            throw dbError(e);
        }
    }

    /** Removes the record and the PDF file. The document can be issued again afterwards. */
    public void deleteDocument(int documentId) throws ServiceException {
        try {
            Optional<IssuedDocument> doc = dao.findById(documentId);
            if (!doc.isPresent()) {
                throw new ServiceException("This document no longer exists. The list has been reloaded.");
            }
            dao.delete(documentId);
            DocumentStorage.delete(doc.get().getFilePath());
        } catch (SQLException e) {
            throw dbError(e);
        }
    }

    // ---------------------------------------------------------------- Generating

    private IssuedDocument generate(DocumentType type, int applicationId, int adminId) throws ServiceException {
        try {
            Optional<DocumentSource> found = dao.findSource(applicationId);
            if (!found.isPresent()) {
                throw new ServiceException("Application not found.");
            }
            DocumentSource source = found.get();
            checkEligible(type, source.getStatus());

            Optional<IssuedDocument> existing = dao.findByApplicationAndType(applicationId, type);
            if (existing.isPresent()) {
                throw new ServiceException("A " + type.getLabel().toLowerCase()
                        + " has already been issued for this application (reference "
                        + existing.get().getReferenceNo() + "). Open it from the Documents page.");
            }

            LocalDate today = LocalDate.now();
            String reference = type.getPrefix() + "-" + today.getYear() + "-" + String.format("%05d", applicationId);
            String storedPath = DocumentStorage.storedPathFor(reference);

            try {
                DocumentStorage.ensureFolder();
                Path target = DocumentStorage.resolve(storedPath);
                if (target == null) {
                    throw new IOException("invalid storage path");
                }
                if (type == DocumentType.OFFER_LETTER) {
                    PdfGenerator.offerLetter(source, reference, today, target);
                } else {
                    PdfGenerator.certificate(source, reference, today, target);
                }
            } catch (IOException e) {
                DocumentStorage.delete(storedPath);   // do not leave a half-written file behind
                throw new ServiceException("Could not create the PDF: " + e.getMessage(), e);
            }

            try {
                dao.insert(applicationId, type, reference, storedPath, adminId);
            } catch (SQLException e) {
                if (e.getErrorCode() == MYSQL_DUPLICATE_ENTRY) {
                    // Someone else issued it at the same moment. Their file has the same name, so keep it.
                    throw new ServiceException("This document was just issued from somewhere else. Please reload.", e);
                }
                DocumentStorage.delete(storedPath);   // do not leave an orphan file behind
                throw dbError(e);
            }

            Optional<IssuedDocument> saved = dao.findByApplicationAndType(applicationId, type);
            if (!saved.isPresent()) {
                throw new ServiceException("The document was created but could not be loaded. Please reload.");
            }
            return saved.get();
        } catch (SQLException e) {
            throw dbError(e);
        }
    }

    private static void checkEligible(DocumentType type, ApplicationStatus status) throws ServiceException {
        if (type == DocumentType.OFFER_LETTER) {
            if (status != ApplicationStatus.SELECTED && status != ApplicationStatus.COMPLETED) {
                throw new ServiceException("An offer letter can only be issued to a selected student. "
                        + "This application is " + status + ".");
            }
        } else if (status != ApplicationStatus.COMPLETED) {
            throw new ServiceException("A completion certificate can only be issued after the internship is "
                    + "completed. This application is " + status + ".");
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

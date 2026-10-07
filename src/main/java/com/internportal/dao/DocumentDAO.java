package com.internportal.dao;

import com.internportal.model.ApplicationStatus;
import com.internportal.model.DocumentSource;
import com.internportal.model.DocumentType;
import com.internportal.model.IssuedDocument;
import com.internportal.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DocumentDAO {

    private static final String SELECT_BASE =
            "SELECT d.document_id, d.application_id, d.doc_type, d.reference_no, d.file_path, d.issued_on, "
          + "u.full_name, i.title, c.company_name "
          + "FROM documents d "
          + "JOIN applications a ON a.application_id = d.application_id "
          + "JOIN users u ON u.user_id = a.student_id "
          + "JOIN internships i ON i.internship_id = a.internship_id "
          + "JOIN companies c ON c.company_id = i.company_id "
          + "WHERE 1 = 1";

    /** Everything needed to build a PDF for one application. */
    public Optional<DocumentSource> findSource(int applicationId) throws SQLException {
        String sql = "SELECT a.application_id, a.status, u.full_name, u.email, s.course, "
                   + "i.title, i.stipend, i.duration_weeks, i.start_date, "
                   + "c.company_name, c.industry, c.website, "
                   + "(SELECT COUNT(*) FROM logbook_entries e "
                   + "   WHERE e.application_id = a.application_id AND e.grade IS NOT NULL) AS graded_weeks, "
                   + "(SELECT AVG(e.grade) FROM logbook_entries e "
                   + "   WHERE e.application_id = a.application_id AND e.grade IS NOT NULL) AS avg_grade "
                   + "FROM applications a "
                   + "JOIN users u ON u.user_id = a.student_id "
                   + "JOIN students s ON s.student_id = a.student_id "
                   + "JOIN internships i ON i.internship_id = a.internship_id "
                   + "JOIN companies c ON c.company_id = i.company_id "
                   + "WHERE a.application_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, applicationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    DocumentSource s = new DocumentSource();
                    s.setApplicationId(rs.getInt("application_id"));
                    s.setStatus(ApplicationStatus.valueOf(rs.getString("status")));
                    s.setStudentName(rs.getString("full_name"));
                    s.setStudentEmail(rs.getString("email"));
                    s.setCourse(rs.getString("course"));
                    s.setInternshipTitle(rs.getString("title"));
                    s.setStipend(rs.getBigDecimal("stipend"));
                    s.setDurationWeeks(rs.getInt("duration_weeks"));
                    s.setStartDate(rs.getDate("start_date").toLocalDate());
                    s.setCompanyName(rs.getString("company_name"));
                    s.setIndustry(rs.getString("industry"));
                    s.setWebsite(rs.getString("website"));
                    s.setGradedWeeks(rs.getInt("graded_weeks"));
                    double average = rs.getDouble("avg_grade");
                    s.setAverageGrade(rs.wasNull() ? null : average);
                    return Optional.of(s);
                }
            }
        }
        return Optional.empty();
    }

    /** Saves the record of an issued document. A second one of the same type throws error code 1062. */
    public int insert(int applicationId, DocumentType type, String referenceNo, String filePath, int adminId)
            throws SQLException {
        String sql = "INSERT INTO documents (application_id, doc_type, reference_no, file_path, issued_by) "
                   + "VALUES (?, ?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, applicationId);
            ps.setString(2, type.name());
            ps.setString(3, referenceNo);
            ps.setString(4, filePath);
            ps.setInt(5, adminId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    public Optional<IssuedDocument> findByApplicationAndType(int applicationId, DocumentType type)
            throws SQLException {
        List<IssuedDocument> list = query(" AND d.application_id = ? AND d.doc_type = ?",
                applicationId, type.name());
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public Optional<IssuedDocument> findById(int documentId) throws SQLException {
        List<IssuedDocument> list = query(" AND d.document_id = ?", documentId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /** All issued documents, newest first. */
    public List<IssuedDocument> findAll() throws SQLException {
        return query("");
    }

    /** The documents of one student, newest first. */
    public List<IssuedDocument> findForStudent(int studentId) throws SQLException {
        return query(" AND a.student_id = ?", studentId);
    }

    public boolean delete(int documentId) throws SQLException {
        String sql = "DELETE FROM documents WHERE document_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, documentId);
            return ps.executeUpdate() > 0;
        }
    }

    private List<IssuedDocument> query(String extraWhere, Object... params) throws SQLException {
        String sql = SELECT_BASE + extraWhere + " ORDER BY d.issued_on DESC, d.document_id DESC";
        List<IssuedDocument> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    IssuedDocument d = new IssuedDocument();
                    d.setDocumentId(rs.getInt("document_id"));
                    d.setApplicationId(rs.getInt("application_id"));
                    d.setType(DocumentType.valueOf(rs.getString("doc_type")));
                    d.setReferenceNo(rs.getString("reference_no"));
                    d.setFilePath(rs.getString("file_path"));
                    Timestamp issuedOn = rs.getTimestamp("issued_on");
                    d.setIssuedOn(issuedOn == null ? null : issuedOn.toLocalDateTime());
                    d.setStudentName(rs.getString("full_name"));
                    d.setInternshipTitle(rs.getString("title"));
                    d.setCompanyName(rs.getString("company_name"));
                    list.add(d);
                }
            }
        }
        return list;
    }
}

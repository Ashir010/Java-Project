package com.internportal.dao;

import com.internportal.model.Company;
import com.internportal.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CompanyDAO {

    public List<Company> findAll() throws SQLException {
        String sql = "SELECT company_id, company_name, industry, website, contact_email "
                   + "FROM companies ORDER BY company_name";
        List<Company> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    public int insert(Company c) throws SQLException {
        String sql = "INSERT INTO companies (company_name, industry, website, contact_email) "
                   + "VALUES (?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, c);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    public void update(Company c) throws SQLException {
        String sql = "UPDATE companies SET company_name = ?, industry = ?, website = ?, "
                   + "contact_email = ? WHERE company_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, c);
            ps.setInt(5, c.getCompanyId());
            ps.executeUpdate();
        }
    }

    public void delete(int companyId) throws SQLException {
        String sql = "DELETE FROM companies WHERE company_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, companyId);
            ps.executeUpdate();
        }
    }

    private void bind(PreparedStatement ps, Company c) throws SQLException {
        ps.setString(1, c.getCompanyName());
        ps.setString(2, c.getIndustry());
        ps.setString(3, c.getWebsite());
        ps.setString(4, c.getContactEmail());
    }

    private Company map(ResultSet rs) throws SQLException {
        Company c = new Company();
        c.setCompanyId(rs.getInt("company_id"));
        c.setCompanyName(rs.getString("company_name"));
        c.setIndustry(rs.getString("industry"));
        c.setWebsite(rs.getString("website"));
        c.setContactEmail(rs.getString("contact_email"));
        return c;
    }
}

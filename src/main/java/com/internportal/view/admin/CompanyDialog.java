package com.internportal.view.admin;

import com.internportal.model.Company;
import com.internportal.service.CatalogService;
import com.internportal.service.ServiceTask;
import com.internportal.util.Validator;
import com.internportal.view.common.FormDialog;

import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Window;

/** Add / edit a company. Pass existing = null to add. */
public class CompanyDialog extends FormDialog {

    private final CatalogService service = new CatalogService();
    private final Company existing;

    private final JTextField nameField = new JTextField();
    private final JTextField industryField = new JTextField();
    private final JTextField websiteField = new JTextField();
    private final JTextField emailField = new JTextField();

    public CompanyDialog(Window owner, Company existing) {
        super(owner, existing == null ? "Add Company" : "Edit Company");
        this.existing = existing;

        if (existing != null) {
            nameField.setText(existing.getCompanyName());
            industryField.setText(existing.getIndustry());
            websiteField.setText(existing.getWebsite());
            emailField.setText(existing.getContactEmail());
        }

        nameField.putClientProperty("JTextField.placeholderText", "e.g. Infosys");
        industryField.putClientProperty("JTextField.placeholderText", "e.g. IT Services");
        websiteField.putClientProperty("JTextField.placeholderText", "e.g. www.infosys.com");
        emailField.putClientProperty("JTextField.placeholderText", "hr@company.com");
        for (JTextField field : new JTextField[]{nameField, industryField, websiteField, emailField}) {
            field.setPreferredSize(new Dimension(300, 34));
        }

        JPanel form = new JPanel(new GridBagLayout());
        addRow(form, 0, "Company name *", nameField);
        addRow(form, 1, "Industry", industryField);
        addRow(form, 2, "Website", websiteField);
        addRow(form, 3, "Contact email", emailField);
        buildDialog(form);
    }

    @Override
    protected String validateInput() {
        if (nameField.getText().trim().length() < 2) {
            nameField.requestFocusInWindow();
            return "Enter the company name (at least 2 characters).";
        }
        String email = emailField.getText().trim();
        if (!email.isEmpty() && !Validator.isValidEmail(email)) {
            emailField.requestFocusInWindow();
            return "Please enter a valid contact email, or leave it empty.";
        }
        return null;
    }

    @Override
    protected ServiceTask buildSaveTask() {
        Company company = new Company();
        if (existing != null) {
            company.setCompanyId(existing.getCompanyId());
        }
        company.setCompanyName(nameField.getText());
        company.setIndustry(industryField.getText());
        company.setWebsite(websiteField.getText());
        company.setContactEmail(emailField.getText());

        return () -> {
            if (existing == null) {
                service.addCompany(company);
            } else {
                service.updateCompany(company);
            }
        };
    }
}

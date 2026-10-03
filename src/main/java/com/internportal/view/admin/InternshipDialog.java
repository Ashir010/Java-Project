package com.internportal.view.admin;

import com.internportal.model.Company;
import com.internportal.model.Internship;
import com.internportal.model.InternshipStatus;
import com.internportal.service.CatalogService;
import com.internportal.service.ServiceTask;
import com.internportal.view.common.FormDialog;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/** Add / edit an internship. Pass existing = null to add. */
public class InternshipDialog extends FormDialog {

    private final CatalogService service = new CatalogService();
    private final Internship existing;

    private final JComboBox<Company> companyBox = new JComboBox<>();
    private final JTextField titleField = new JTextField();
    private final JTextArea descriptionArea = new JTextArea(5, 28);
    private final JSpinner stipendSpinner = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 1000000.0, 500.0));
    private final JSpinner weeksSpinner = new JSpinner(new SpinnerNumberModel(8, 1, 52, 1));
    private final JSpinner deadlineSpinner = new JSpinner(new SpinnerDateModel(
            toDate(LocalDate.now().plusDays(30)), null, null, Calendar.DAY_OF_MONTH));

    public InternshipDialog(Window owner, List<Company> companies, Internship existing) {
        super(owner, existing == null ? "Add Internship" : "Edit Internship");
        this.existing = existing;

        for (Company company : companies) {
            companyBox.addItem(company);
        }
        stipendSpinner.setEditor(new JSpinner.NumberEditor(stipendSpinner, "#,##0"));
        deadlineSpinner.setEditor(new JSpinner.DateEditor(deadlineSpinner, "dd MMM yyyy"));
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        titleField.putClientProperty("JTextField.placeholderText", "e.g. Java Developer Intern");

        if (existing != null) {
            selectCompany(existing.getCompanyId());
            titleField.setText(existing.getTitle());
            descriptionArea.setText(existing.getDescription() == null ? "" : existing.getDescription());
            stipendSpinner.setValue(existing.getStipend().doubleValue());
            weeksSpinner.setValue(existing.getDurationWeeks());
            deadlineSpinner.setValue(toDate(existing.getDeadline()));
        }

        companyBox.setPreferredSize(new Dimension(300, 34));
        titleField.setPreferredSize(new Dimension(300, 34));
        stipendSpinner.setPreferredSize(new Dimension(300, 34));
        weeksSpinner.setPreferredSize(new Dimension(300, 34));
        deadlineSpinner.setPreferredSize(new Dimension(300, 34));
        JScrollPane descriptionScroll = new JScrollPane(descriptionArea);
        descriptionScroll.setPreferredSize(new Dimension(300, 110));

        JPanel form = new JPanel(new GridBagLayout());
        addRow(form, 0, "Company *", companyBox);
        addRow(form, 1, "Title *", titleField);
        addRow(form, 2, "Description", descriptionScroll);
        addRow(form, 3, "Stipend (INR / month)", stipendSpinner);
        addRow(form, 4, "Duration (weeks)", weeksSpinner);
        addRow(form, 5, "Apply by", deadlineSpinner);
        buildDialog(form);
    }

    @Override
    protected String validateInput() {
        if (companyBox.getSelectedItem() == null) {
            return "Please choose a company.";
        }
        if (titleField.getText().trim().length() < 3) {
            titleField.requestFocusInWindow();
            return "Enter a title (at least 3 characters).";
        }
        try {
            // Make sure anything typed into the spinners is accepted before we read it
            stipendSpinner.commitEdit();
            weeksSpinner.commitEdit();
            deadlineSpinner.commitEdit();
        } catch (ParseException e) {
            return "Please enter a valid stipend, duration and deadline.";
        }
        return null;
    }

    @Override
    protected ServiceTask buildSaveTask() {
        Internship internship = new Internship();
        if (existing != null) {
            internship.setInternshipId(existing.getInternshipId());
            internship.setStatus(existing.getStatus());
        } else {
            internship.setStatus(InternshipStatus.OPEN);
        }
        internship.setCompanyId(((Company) companyBox.getSelectedItem()).getCompanyId());
        internship.setTitle(titleField.getText());
        internship.setDescription(descriptionArea.getText());
        internship.setStipend(BigDecimal.valueOf(((Number) stipendSpinner.getValue()).doubleValue())
                .setScale(2, RoundingMode.HALF_UP));
        internship.setDurationWeeks(((Number) weeksSpinner.getValue()).intValue());
        internship.setDeadline(toLocalDate((Date) deadlineSpinner.getValue()));

        return () -> {
            if (existing == null) {
                service.addInternship(internship);
            } else {
                service.updateInternship(internship);
            }
        };
    }

    private void selectCompany(int companyId) {
        for (int i = 0; i < companyBox.getItemCount(); i++) {
            if (companyBox.getItemAt(i).getCompanyId() == companyId) {
                companyBox.setSelectedIndex(i);
                return;
            }
        }
    }

    private static Date toDate(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}

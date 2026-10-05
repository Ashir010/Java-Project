package com.internportal.view.admin;

import com.internportal.model.ApplicationRecord;
import com.internportal.model.ApplicationStatus;
import com.internportal.model.Internship;
import com.internportal.service.ApplicationReviewService;
import com.internportal.service.CatalogService;
import com.internportal.service.ServiceException;
import com.internportal.view.common.BaseTablePanel;
import com.internportal.view.common.ResumeOpener;
import com.internportal.view.common.StatusCellRenderer;

import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.Dimension;
import java.awt.event.HierarchyEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutionException;

/** Admin screen: review applications, filter them, and move them through the workflow. */
public class ApplicationsPanel extends BaseTablePanel<ApplicationRecord> {

    private static final int STATUS_COLUMN = 6;
    private static final String ALL_STATUSES = "All statuses";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final ApplicationReviewService service = new ApplicationReviewService();
    private final CatalogService catalogService = new CatalogService();

    private final JComboBox<InternshipOption> internshipBox = new JComboBox<>();
    private final JComboBox<String> statusBox = new JComboBox<>();

    // Read by the background thread in loadItems(); written only on the UI thread
    private volatile int filterInternshipId = 0;
    private volatile ApplicationStatus filterStatus = null;

    // UI thread only: stops the drop-down from reloading the table while its items are being replaced
    private boolean updatingOptions = false;

    public ApplicationsPanel() {
        super(new String[]{"Student", "Email", "Course", "Internship", "Company", "Applied on", "Status"});
        getTable().getColumnModel().getColumn(STATUS_COLUMN).setCellRenderer(new StatusCellRenderer());

        internshipBox.addItem(new InternshipOption(0, "All internships"));
        statusBox.addItem(ALL_STATUSES);
        for (ApplicationStatus status : ApplicationStatus.values()) {
            statusBox.addItem(status.name());
        }
        internshipBox.setPreferredSize(new Dimension(300, 34));
        statusBox.setPreferredSize(new Dimension(160, 34));
        internshipBox.addActionListener(e -> onFilterChanged());
        statusBox.addActionListener(e -> onFilterChanged());
        addFilterComponent(internshipBox);
        addFilterComponent(statusBox);

        addToolbarButton("Shortlist", () -> changeStatus(ApplicationStatus.SHORTLISTED));
        addToolbarButton("Mark Selected", () -> changeStatus(ApplicationStatus.SELECTED));
        addToolbarButton("Reject", () -> changeStatus(ApplicationStatus.REJECTED));
        addToolbarButton("View Student", this::onViewStudent);
        addToolbarButton("View Resume", this::onViewResume);
        addToolbarButton("Refresh", this::reloadAll);

        // Reload every time the admin opens this page, so new applications always show
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                reloadAll();
            }
        });
        reloadAll();
    }

    @Override
    protected List<ApplicationRecord> loadItems() throws ServiceException {
        return service.listApplications(filterInternshipId, filterStatus);
    }

    @Override
    protected Object[] toRow(ApplicationRecord r) {
        return new Object[]{
                r.getStudentName(), r.getStudentEmail(), text(r.getCourse()), r.getInternshipTitle(),
                r.getCompanyName(), dateOf(r.getAppliedOn()), r.getStatus().name()
        };
    }

    // ---------------------------------------------------------------- Filters

    private void reloadAll() {
        reloadInternshipOptions();
        refresh();
    }

    private void onFilterChanged() {
        if (updatingOptions) {
            return;
        }
        InternshipOption option = (InternshipOption) internshipBox.getSelectedItem();
        filterInternshipId = option == null ? 0 : option.id;

        String status = (String) statusBox.getSelectedItem();
        filterStatus = (status == null || status.equals(ALL_STATUSES))
                ? null : ApplicationStatus.valueOf(status);
        refresh();
    }

    /** Reloads the internship drop-down (off the UI thread) and keeps the current choice. */
    private void reloadInternshipOptions() {
        new SwingWorker<List<Internship>, Void>() {
            @Override
            protected List<Internship> doInBackground() throws ServiceException {
                return catalogService.listInternships();
            }

            @Override
            protected void done() {
                try {
                    List<Internship> internships = get();
                    int keepId = filterInternshipId;
                    updatingOptions = true;
                    try {
                        internshipBox.removeAllItems();
                        internshipBox.addItem(new InternshipOption(0, "All internships"));
                        for (Internship i : internships) {
                            internshipBox.addItem(new InternshipOption(i.getInternshipId(),
                                    i.getTitle() + " (" + i.getCompanyName() + ")"));
                        }
                        selectOption(keepId);
                    } finally {
                        updatingOptions = false;
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    showError(messageOf(ex.getCause()));
                }
            }
        }.execute();
    }

    private void selectOption(int internshipId) {
        for (int i = 0; i < internshipBox.getItemCount(); i++) {
            if (internshipBox.getItemAt(i).id == internshipId) {
                internshipBox.setSelectedIndex(i);
                return;
            }
        }
        internshipBox.setSelectedIndex(0);
        filterInternshipId = 0;
    }

    // ---------------------------------------------------------------- Actions

    private void changeStatus(ApplicationStatus target) {
        ApplicationRecord selected = getSelectedItem();
        if (selected == null) {
            showInfo("Select an application first.");
            return;
        }
        ApplicationStatus current = selected.getStatus();
        if (!ApplicationReviewService.isAllowed(current, target)) {
            showInfo(ApplicationReviewService.transitionMessage(current, target));
            return;
        }
        String question = "Change " + selected.getStudentName() + "'s application for \""
                + selected.getInternshipTitle() + "\"\nfrom " + current + " to " + target + "?";
        if (!confirm(question)) {
            return;
        }

        int applicationId = selected.getApplicationId();
        runTask(() -> {
            try {
                service.changeStatus(applicationId, current, target);
            } catch (ServiceException e) {
                SwingUtilities.invokeLater(this::refresh);   // the list may be out of date, so reload it
                throw e;
            }
        }, this::refresh);
    }

    private void onViewStudent() {
        ApplicationRecord r = getSelectedItem();
        if (r == null) {
            showInfo("Select an application first.");
            return;
        }
        String skills = (r.getSkills() == null || r.getSkills().trim().isEmpty())
                ? "Not added yet" : r.getSkills();
        String details = "Name: " + r.getStudentName()
                + "\nEmail: " + r.getStudentEmail()
                + "\nPhone: " + text(r.getPhone())
                + "\nCourse: " + text(r.getCourse())
                + "\nSkills: " + skills
                + "\n\nApplied for: " + r.getInternshipTitle() + " at " + r.getCompanyName()
                + "\nApplied on: " + dateOf(r.getAppliedOn()).format(DATE_FORMAT)
                + "\nStatus: " + r.getStatus();

        JTextArea area = new JTextArea(details);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setCaretPosition(0);
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(420, 230));
        JOptionPane.showMessageDialog(this, scroll, "Applicant Details", JOptionPane.INFORMATION_MESSAGE);
    }

    // ---------------------------------------------------------------- Helpers

    private void onViewResume() {
        ApplicationRecord r = getSelectedItem();
        if (r == null) {
            showInfo("Select an application first.");
            return;
        }
        if (r.getResumePath() == null || r.getResumePath().trim().isEmpty()) {
            showInfo(r.getStudentName() + " has not uploaded a resume yet.");
            return;
        }
        ResumeOpener.open(this, r.getResumePath());
    }

    private static String text(String s) {
        return s == null ? "" : s;
    }

    private static LocalDate dateOf(LocalDateTime time) {
        return time == null ? LocalDate.of(1970, 1, 1) : time.toLocalDate();
    }

    /** One entry of the internship drop-down. id 0 means "All internships". */
    private static final class InternshipOption {
        final int id;
        final String label;

        InternshipOption(int id, String label) {
            this.id = id;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}

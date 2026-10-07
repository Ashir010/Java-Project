package com.internportal.view.student;

import com.internportal.model.Internship;
import com.internportal.model.InternshipListing;
import com.internportal.service.ApplicationService;
import com.internportal.service.ServiceException;
import com.internportal.util.SessionManager;
import com.internportal.view.common.BaseTablePanel;
import com.internportal.view.common.StatusCellRenderer;

import javax.swing.SwingUtilities;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/** Student screen: open internships to browse, view details, and apply. */
public class BrowsePanel extends BaseTablePanel<InternshipListing> {

    private static final int STATUS_COLUMN = 5;

    private final ApplicationService service = new ApplicationService();
    private final int studentId;

    public BrowsePanel() {
        super(new String[]{"Title", "Company", "Stipend (INR)", "Duration", "Deadline", "Status"});
        studentId = SessionManager.getInstance().getCurrentUser().getUserId();

        getTable().getColumnModel().getColumn(STATUS_COLUMN).setCellRenderer(new StatusCellRenderer());
        getTable().getColumnModel().getColumn(3).setPreferredWidth(240);
        getTable().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    onViewDetails();   // double-click opens the details
                }
            }
        });

        addToolbarButton("View Details", this::onViewDetails);
        addToolbarButton("Apply", this::onApply);
        addToolbarButton("Refresh", this::refresh);
        refresh();
    }

    @Override
    protected List<InternshipListing> loadItems() throws ServiceException {
        return service.listAvailable(studentId);
    }

    @Override
    protected Object[] toRow(InternshipListing listing) {
        Internship i = listing.getInternship();
        return new Object[]{
                i.getTitle(), i.getCompanyName(), i.getStipend(), i.getPeriod(),
                i.getDeadline(), listing.isApplied() ? "APPLIED" : ""
        };
    }

    private void onViewDetails() {
        InternshipListing selected = getSelectedItem();
        if (selected == null) {
            showInfo("Select an internship first.");
            return;
        }
        InternshipDetailDialog dialog =
                new InternshipDetailDialog(SwingUtilities.getWindowAncestor(this), selected);
        dialog.setVisible(true);
        if (dialog.isApplyRequested()) {
            submitApplication(selected);   // the Apply button inside the details is the confirmation
        }
    }

    private void onApply() {
        InternshipListing selected = getSelectedItem();
        if (selected == null) {
            showInfo("Select an internship first.");
            return;
        }
        if (selected.isApplied()) {
            showInfo("You have already applied to this internship.");
            return;
        }
        Internship i = selected.getInternship();
        if (!confirm("Apply for \"" + i.getTitle() + "\" at " + i.getCompanyName() + "?")) {
            return;
        }
        submitApplication(selected);
    }

    private void submitApplication(InternshipListing listing) {
        int internshipId = listing.getInternship().getInternshipId();
        String title = listing.getInternship().getTitle();

        runTask(() -> {
            try {
                service.apply(studentId, internshipId);
            } catch (ServiceException e) {
                SwingUtilities.invokeLater(this::refresh);   // the list may be out of date, so reload it
                throw e;
            }
        }, () -> {
            showInfo("Application submitted for \"" + title + "\".\nYou can track it under My Applications.");
            refresh();
        });
    }
}

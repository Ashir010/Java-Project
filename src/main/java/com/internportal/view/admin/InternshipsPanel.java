package com.internportal.view.admin;

import com.internportal.model.Company;
import com.internportal.model.Internship;
import com.internportal.model.InternshipStatus;
import com.internportal.service.CatalogService;
import com.internportal.service.ServiceException;
import com.internportal.view.common.BaseTablePanel;
import com.internportal.view.common.StatusCellRenderer;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class InternshipsPanel extends BaseTablePanel<Internship> {

    private static final int STATUS_COLUMN = 5;

    private final CatalogService service = new CatalogService();

    public InternshipsPanel() {
        super(new String[]{"Title", "Company", "Stipend (INR)", "Duration", "Deadline", "Status"});
        getTable().getColumnModel().getColumn(STATUS_COLUMN).setCellRenderer(new StatusCellRenderer());
        getTable().getColumnModel().getColumn(3).setPreferredWidth(240);

        addToolbarButton("Add", () -> openForm(null));
        addToolbarButton("Edit", this::onEdit);
        addToolbarButton("Close / Reopen", this::onToggleStatus);
        refresh();
    }

    @Override
    protected List<Internship> loadItems() throws ServiceException {
        return service.listInternships();
    }

    @Override
    protected Object[] toRow(Internship i) {
        return new Object[]{
                i.getTitle(), i.getCompanyName(), i.getStipend(), i.getPeriod(),
                i.getDeadline(), i.getStatus().name()
        };
    }

    private void onEdit() {
        Internship selected = getSelectedItem();
        if (selected == null) {
            showInfo("Select an internship first.");
            return;
        }
        openForm(selected);
    }

    private void onToggleStatus() {
        Internship selected = getSelectedItem();
        if (selected == null) {
            showInfo("Select an internship first.");
            return;
        }
        InternshipStatus next = selected.getStatus() == InternshipStatus.OPEN
                ? InternshipStatus.CLOSED : InternshipStatus.OPEN;
        String verb = next == InternshipStatus.CLOSED ? "close" : "reopen";
        if (!confirm("Do you want to " + verb + " \"" + selected.getTitle() + "\"?")) {
            return;
        }
        int id = selected.getInternshipId();
        runTask(() -> service.setInternshipStatus(id, next), this::refresh);
    }

    /** Loads the companies first (off the UI thread), then opens the add/edit dialog. */
    private void openForm(Internship existing) {
        new SwingWorker<List<Company>, Void>() {
            @Override
            protected List<Company> doInBackground() throws ServiceException {
                return service.listCompanies();
            }

            @Override
            protected void done() {
                try {
                    List<Company> companies = get();
                    if (companies.isEmpty()) {
                        showInfo("Add a company first (Companies tab), then create internships for it.");
                        return;
                    }
                    InternshipDialog dialog = new InternshipDialog(
                            SwingUtilities.getWindowAncestor(InternshipsPanel.this), companies, existing);
                    dialog.setVisible(true);
                    if (dialog.isSaved()) {
                        refresh();
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    showError(messageOf(ex.getCause()));
                }
            }
        }.execute();
    }
}

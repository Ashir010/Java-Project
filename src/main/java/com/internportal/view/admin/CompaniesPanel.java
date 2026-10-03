package com.internportal.view.admin;

import com.internportal.model.Company;
import com.internportal.service.CatalogService;
import com.internportal.service.ServiceException;
import com.internportal.view.common.BaseTablePanel;

import javax.swing.SwingUtilities;
import java.util.List;

public class CompaniesPanel extends BaseTablePanel<Company> {

    private final CatalogService service = new CatalogService();

    public CompaniesPanel() {
        super(new String[]{"Company", "Industry", "Website", "Contact email"});
        addToolbarButton("Add", this::onAdd);
        addToolbarButton("Edit", this::onEdit);
        addToolbarButton("Delete", this::onDelete);
        refresh();
    }

    @Override
    protected List<Company> loadItems() throws ServiceException {
        return service.listCompanies();
    }

    @Override
    protected Object[] toRow(Company c) {
        return new Object[]{
                c.getCompanyName(), text(c.getIndustry()), text(c.getWebsite()), text(c.getContactEmail())
        };
    }

    private void onAdd() {
        CompanyDialog dialog = new CompanyDialog(SwingUtilities.getWindowAncestor(this), null);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            refresh();
        }
    }

    private void onEdit() {
        Company selected = getSelectedItem();
        if (selected == null) {
            showInfo("Select a company first.");
            return;
        }
        CompanyDialog dialog = new CompanyDialog(SwingUtilities.getWindowAncestor(this), selected);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            refresh();
        }
    }

    private void onDelete() {
        Company selected = getSelectedItem();
        if (selected == null) {
            showInfo("Select a company first.");
            return;
        }
        if (!confirm("Delete company \"" + selected.getCompanyName() + "\"?\nThis cannot be undone.")) {
            return;
        }
        int id = selected.getCompanyId();
        runTask(() -> service.deleteCompany(id), this::refresh);
    }

    private static String text(String s) {
        return s == null ? "" : s;
    }
}

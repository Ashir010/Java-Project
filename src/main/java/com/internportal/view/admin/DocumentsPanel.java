package com.internportal.view.admin;

import com.internportal.model.IssuedDocument;
import com.internportal.service.DocumentService;
import com.internportal.service.ServiceException;
import com.internportal.view.common.BaseTablePanel;
import com.internportal.view.common.DocumentActions;

import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/** Admin screen: every offer letter and certificate that has been issued. */
public class DocumentsPanel extends BaseTablePanel<IssuedDocument> {

    private final DocumentService service = new DocumentService();

    public DocumentsPanel() {
        super(new String[]{"Document", "Student", "Internship", "Company", "Reference", "Issued on"});

        getTable().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    onOpen();   // double-click opens the PDF
                }
            }
        });

        addToolbarButton("Open", this::onOpen);
        addToolbarButton("Save a Copy", this::onSaveCopy);
        addToolbarButton("Delete", this::onDelete);
        addToolbarButton("Refresh", this::refresh);

        // Reload every time the admin opens this page, so documents issued from Applications always show
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                refresh();
            }
        });
        refresh();
    }

    @Override
    protected List<IssuedDocument> loadItems() throws ServiceException {
        return service.listAll();
    }

    @Override
    protected Object[] toRow(IssuedDocument d) {
        return new Object[]{
                d.getType().getLabel(), d.getStudentName(), d.getInternshipTitle(), d.getCompanyName(),
                d.getReferenceNo(), d.getIssuedOn().toLocalDate()
        };
    }

    private void onOpen() {
        IssuedDocument selected = getSelectedItem();
        if (selected == null) {
            showInfo("Select a document first. Issue documents from the Applications screen.");
            return;
        }
        DocumentActions.open(this, selected);
    }

    private void onSaveCopy() {
        IssuedDocument selected = getSelectedItem();
        if (selected == null) {
            showInfo("Select a document first.");
            return;
        }
        DocumentActions.saveCopy(this, selected);
    }

    private void onDelete() {
        IssuedDocument selected = getSelectedItem();
        if (selected == null) {
            showInfo("Select a document first.");
            return;
        }
        if (!confirm("Delete " + selected.getType().getLabel() + " " + selected.getReferenceNo() + " for "
                + selected.getStudentName() + "?\nThe PDF file is removed. You can issue it again from the "
                + "Applications screen.")) {
            return;
        }
        int id = selected.getDocumentId();
        runTask(() -> {
            try {
                service.deleteDocument(id);
            } catch (ServiceException e) {
                javax.swing.SwingUtilities.invokeLater(this::refresh);   // the list may be out of date
                throw e;
            }
        }, this::refresh);
    }
}

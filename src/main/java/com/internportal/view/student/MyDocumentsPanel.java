package com.internportal.view.student;

import com.internportal.model.IssuedDocument;
import com.internportal.service.DocumentService;
import com.internportal.service.ServiceException;
import com.internportal.util.SessionManager;
import com.internportal.view.common.BaseTablePanel;
import com.internportal.view.common.DocumentActions;

import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/** Student screen: the offer letters and certificates the admin has issued to me. */
public class MyDocumentsPanel extends BaseTablePanel<IssuedDocument> {

    private final DocumentService service = new DocumentService();
    private final int studentId;

    public MyDocumentsPanel() {
        super(new String[]{"Document", "Internship", "Company", "Reference", "Issued on"});
        studentId = SessionManager.getInstance().getCurrentUser().getUserId();

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
        addToolbarButton("Refresh", this::refresh);

        // Reload every time the student opens this page, so a newly issued document always shows
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                refresh();
            }
        });
        refresh();
    }

    @Override
    protected List<IssuedDocument> loadItems() throws ServiceException {
        return service.listForStudent(studentId);
    }

    @Override
    protected Object[] toRow(IssuedDocument d) {
        return new Object[]{
                d.getType().getLabel(), d.getInternshipTitle(), d.getCompanyName(),
                d.getReferenceNo(), d.getIssuedOn().toLocalDate()
        };
    }

    private void onOpen() {
        IssuedDocument selected = getSelectedItem();
        if (selected == null) {
            showInfo("Select a document first. Documents appear here after the admin issues them.");
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
}

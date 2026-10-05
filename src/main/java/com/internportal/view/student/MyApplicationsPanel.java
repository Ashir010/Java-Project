package com.internportal.view.student;

import com.internportal.model.MyApplication;
import com.internportal.model.StatusChange;
import com.internportal.service.MyApplicationService;
import com.internportal.service.ServiceException;
import com.internportal.util.SessionManager;
import com.internportal.view.common.BaseTablePanel;
import com.internportal.view.common.StatusCellRenderer;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;

/** Student screen: all my applications with their status, and a timeline for each. */
public class MyApplicationsPanel extends BaseTablePanel<MyApplication> {

    private static final int STATUS_COLUMN = 3;

    private final MyApplicationService service = new MyApplicationService();
    private final int studentId;

    public MyApplicationsPanel() {
        super(new String[]{"Internship", "Company", "Applied on", "Status", "Last update"});
        studentId = SessionManager.getInstance().getCurrentUser().getUserId();

        getTable().getColumnModel().getColumn(STATUS_COLUMN).setCellRenderer(new StatusCellRenderer());
        getTable().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    onTrack();   // double-click opens the timeline
                }
            }
        });

        addToolbarButton("Track Application", this::onTrack);
        addToolbarButton("Refresh", this::refresh);

        // Reload every time the student opens this page, so an application made in Browse Internships
        // (or a status change by the admin) always shows without restarting
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                refresh();
            }
        });
        refresh();
    }

    @Override
    protected List<MyApplication> loadItems() throws ServiceException {
        return service.listForStudent(studentId);
    }

    @Override
    protected Object[] toRow(MyApplication a) {
        return new Object[]{
                a.getInternshipTitle(), a.getCompanyName(), dateOf(a.getAppliedOn()),
                a.getStatus().name(), dateOf(a.getLastUpdate())
        };
    }

    /** Loads the timeline (off the UI thread), then opens the pop-up. */
    private void onTrack() {
        MyApplication selected = getSelectedItem();
        if (selected == null) {
            showInfo("Select an application first.");
            return;
        }
        new SwingWorker<List<StatusChange>, Void>() {
            @Override
            protected List<StatusChange> doInBackground() throws ServiceException {
                return service.timeline(studentId, selected);
            }

            @Override
            protected void done() {
                try {
                    List<StatusChange> steps = get();
                    new TrackDialog(SwingUtilities.getWindowAncestor(MyApplicationsPanel.this),
                            selected, steps).setVisible(true);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    showError(messageOf(ex.getCause()));
                }
            }
        }.execute();
    }

    private static LocalDate dateOf(LocalDateTime time) {
        return time == null ? LocalDate.of(1970, 1, 1) : time.toLocalDate();
    }
}

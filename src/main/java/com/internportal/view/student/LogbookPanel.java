package com.internportal.view.student;

import com.internportal.model.ApplicationStatus;
import com.internportal.model.InternshipPeriod;
import com.internportal.model.LogbookInternship;
import com.internportal.model.LogbookWeek;
import com.internportal.model.WeekStatus;
import com.internportal.service.LogbookService;
import com.internportal.service.ServiceException;
import com.internportal.util.SessionManager;
import com.internportal.view.common.BaseTablePanel;
import com.internportal.view.common.StatusCellRenderer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.HierarchyEvent;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/** Student screen: one row per week of a selected internship, and an editor for the chosen week. */
public class LogbookPanel extends BaseTablePanel<LogbookWeek> {

    private static final int DATES_COLUMN = 1;
    private static final int STATUS_COLUMN = 2;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final LogbookService service = new LogbookService();
    private final int studentId;

    private final JComboBox<LogbookInternship> internshipBox = new JComboBox<>();
    private final JLabel entryTitle = new JLabel("Select a week to write or view its entry");
    private final JTextArea entryArea = new JTextArea(5, 40);
    private final JLabel feedbackLabel = new JLabel(" ");
    private final JButton submitButton;

    // Read by the background thread in loadItems(); written only on the UI thread
    private volatile int selectedApplicationId = 0;

    // UI thread only
    private LogbookInternship currentInternship = null;
    private boolean updatingOptions = false;
    private int reselectWeek = 0;

    public LogbookPanel() {
        super(new String[]{"Week", "Dates", "Status", "Grade", "Remarks"});
        studentId = SessionManager.getInstance().getCurrentUser().getUserId();

        getTable().getColumnModel().getColumn(STATUS_COLUMN).setCellRenderer(new StatusCellRenderer());
        getTable().getColumnModel().getColumn(DATES_COLUMN).setPreferredWidth(260);

        internshipBox.setPreferredSize(new Dimension(420, 34));
        internshipBox.addActionListener(e -> onInternshipChanged());
        addFilterComponent(internshipBox);

        setDetailComponent(buildEntryPanel());
        submitButton = addToolbarButton("Submit Entry", this::onSubmit);
        addToolbarButton("Refresh", this::reloadAll);

        getTable().getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelectedWeek();
            }
        });

        // Reload every time the student opens this page, so a newly selected internship or a grade always shows
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                reloadAll();
            }
        });

        showSelectedWeek();
        reloadAll();
    }

    @Override
    protected List<LogbookWeek> loadItems() throws ServiceException {
        int applicationId = selectedApplicationId;
        if (applicationId == 0) {
            return new ArrayList<>();
        }
        return service.weeks(studentId, applicationId);
    }

    @Override
    protected Object[] toRow(LogbookWeek w) {
        return new Object[]{
                w.getWeekNo(),
                new InternshipPeriod(w.getWeekStart(), w.getWeekEnd()),
                w.getStatus().getLabel(),
                w.getGrade() == null ? "" : w.getGrade() + "/10",
                w.getRemarks() == null ? "" : w.getRemarks()
        };
    }

    /** After a reload: re-select the week just saved, or else the first week that is waiting for an entry. */
    @Override
    protected void onLoaded() {
        List<LogbookWeek> weeks = getItems();
        int target = -1;
        for (int i = 0; i < weeks.size(); i++) {
            if (reselectWeek > 0 && weeks.get(i).getWeekNo() == reselectWeek) {
                target = i;
                break;
            }
        }
        if (target < 0 && reselectWeek == 0) {
            for (int i = 0; i < weeks.size(); i++) {
                if (weeks.get(i).getStatus() == WeekStatus.PENDING) {
                    target = i;
                    break;
                }
            }
        }
        reselectWeek = 0;

        if (target >= 0) {
            int viewRow = getTable().convertRowIndexToView(target);
            if (viewRow >= 0) {
                getTable().setRowSelectionInterval(viewRow, viewRow);
            }
        }
    }

    // ---------------------------------------------------------------- UI

    private JPanel buildEntryPanel() {
        entryTitle.setFont(entryTitle.getFont().deriveFont(Font.BOLD));
        entryArea.setLineWrap(true);
        entryArea.setWrapStyleWord(true);
        entryArea.setEditable(false);
        JScrollPane scroll = new JScrollPane(entryArea);
        scroll.setPreferredSize(new Dimension(400, 110));

        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        panel.add(entryTitle, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(feedbackLabel, BorderLayout.SOUTH);
        return panel;
    }

    /** Shows the selected week in the editor, and decides whether it can be edited. */
    private void showSelectedWeek() {
        LogbookWeek week = getSelectedItem();
        LogbookInternship internship = currentInternship;

        if (internship == null) {
            entryTitle.setText("You have no selected internships yet.");
            entryArea.setText("");
            entryArea.setEditable(false);
            feedbackLabel.setText(html("The logbook opens after the admin marks your application as Selected."));
            submitButton.setEnabled(false);
            return;
        }
        if (week == null) {
            entryTitle.setText("Select a week to write or view its entry");
            entryArea.setText("");
            entryArea.setEditable(false);
            feedbackLabel.setText(" ");
            submitButton.setEnabled(false);
            return;
        }

        entryTitle.setText("Week " + week.getWeekNo() + ":  "
                + new InternshipPeriod(week.getWeekStart(), week.getWeekEnd()));
        entryArea.setText(week.getSummary() == null ? "" : week.getSummary());
        entryArea.setCaretPosition(0);

        boolean internshipOpen = internship.getStatus() == ApplicationStatus.SELECTED;
        boolean canEdit = internshipOpen
                && (week.getStatus() == WeekStatus.PENDING || week.getStatus() == WeekStatus.SUBMITTED);
        entryArea.setEditable(canEdit);
        submitButton.setEnabled(canEdit);

        if (!internshipOpen) {
            feedbackLabel.setText(html("This internship is completed, so the logbook is read-only."));
            return;
        }
        switch (week.getStatus()) {
            case NOT_STARTED:
                feedbackLabel.setText(html("This week starts on " + week.getWeekStart().format(DATE_FORMAT)
                        + ". You can write its entry from then."));
                break;
            case PENDING:
                feedbackLabel.setText(html("Describe what you worked on and learned this week "
                        + "(at least 20 characters), then click Submit Entry."));
                break;
            case SUBMITTED:
                feedbackLabel.setText(html("Submitted. You can still edit it until the admin grades it."));
                break;
            default:
                String remarks = week.getRemarks() == null ? "" : " Remarks: " + week.getRemarks();
                feedbackLabel.setText(html("Graded " + week.getGrade() + "/10." + remarks
                        + " Graded entries cannot be changed."));
                break;
        }
    }

    // ---------------------------------------------------------------- Loading

    /** Reloads the internship drop-down (keeping the current choice), then the weeks. */
    private void reloadAll() {
        new SwingWorker<List<LogbookInternship>, Void>() {
            @Override
            protected List<LogbookInternship> doInBackground() throws ServiceException {
                return service.listMyInternships(studentId);
            }

            @Override
            protected void done() {
                try {
                    List<LogbookInternship> internships = get();
                    int keepId = selectedApplicationId;
                    updatingOptions = true;
                    try {
                        internshipBox.removeAllItems();
                        for (LogbookInternship internship : internships) {
                            internshipBox.addItem(internship);
                        }
                        selectInternship(keepId);
                    } finally {
                        updatingOptions = false;
                    }
                    currentInternship = (LogbookInternship) internshipBox.getSelectedItem();
                    selectedApplicationId = currentInternship == null ? 0 : currentInternship.getApplicationId();
                    showSelectedWeek();
                    refresh();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    showError(messageOf(ex.getCause()));
                }
            }
        }.execute();
    }

    private void selectInternship(int applicationId) {
        for (int i = 0; i < internshipBox.getItemCount(); i++) {
            if (internshipBox.getItemAt(i).getApplicationId() == applicationId) {
                internshipBox.setSelectedIndex(i);
                return;
            }
        }
        if (internshipBox.getItemCount() > 0) {
            internshipBox.setSelectedIndex(0);
        }
    }

    private void onInternshipChanged() {
        if (updatingOptions) {
            return;
        }
        currentInternship = (LogbookInternship) internshipBox.getSelectedItem();
        selectedApplicationId = currentInternship == null ? 0 : currentInternship.getApplicationId();
        refresh();
    }

    // ---------------------------------------------------------------- Actions

    private void onSubmit() {
        LogbookWeek week = getSelectedItem();
        LogbookInternship internship = currentInternship;
        if (week == null || internship == null) {
            showInfo("Select a week first.");
            return;
        }
        String text = entryArea.getText();
        int weekNo = week.getWeekNo();
        int applicationId = internship.getApplicationId();

        runTask(() -> {
            try {
                service.submit(studentId, applicationId, weekNo, text);
            } catch (ServiceException e) {
                SwingUtilities.invokeLater(this::refresh);   // the weeks may be out of date, so reload them
                throw e;
            }
        }, () -> {
            reselectWeek = weekNo;
            showInfo("Week " + weekNo + " saved.");
            refresh();
        });
    }

    private static String html(String text) {
        String safe = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        return "<html><div style='width:640px'>" + safe + "</div></html>";
    }
}

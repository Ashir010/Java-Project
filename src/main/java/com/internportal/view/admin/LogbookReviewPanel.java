package com.internportal.view.admin;

import com.internportal.model.ApplicationStatus;
import com.internportal.model.Internship;
import com.internportal.model.LogbookRecord;
import com.internportal.service.CatalogService;
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
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.HierarchyEvent;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutionException;

/** Admin screen: read submitted logbook entries, grade them, and mark an internship completed. */
public class LogbookReviewPanel extends BaseTablePanel<LogbookRecord> {

    private static final int STATUS_COLUMN = 4;
    private static final String STATE_ALL = "All entries";
    private static final String STATE_AWAITING = "Awaiting grade";
    private static final String STATE_GRADED = "Graded";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final LogbookService service = new LogbookService();
    private final CatalogService catalogService = new CatalogService();

    private final JComboBox<InternshipOption> internshipBox = new JComboBox<>();
    private final JComboBox<String> stateBox = new JComboBox<>();

    private final JLabel infoLabel = new JLabel("Select an entry to read and grade it.");
    private final JTextArea summaryArea = new JTextArea(4, 40);
    private final JSpinner gradeSpinner = new JSpinner(new SpinnerNumberModel(8, 0, 10, 1));
    private final JTextField remarksField = new JTextField();
    private final JButton saveGradeButton;

    // Read by the background thread in loadItems(); written only on the UI thread
    private volatile int filterInternshipId = 0;
    private volatile Boolean filterGraded = null;

    // UI thread only
    private boolean updatingOptions = false;
    private int reselectEntryId = 0;

    public LogbookReviewPanel() {
        super(new String[]{"Student", "Internship", "Week", "Submitted", "Status", "Grade"});
        getTable().getColumnModel().getColumn(STATUS_COLUMN).setCellRenderer(new StatusCellRenderer());

        internshipBox.addItem(new InternshipOption(0, "All internships"));
        stateBox.addItem(STATE_ALL);
        stateBox.addItem(STATE_AWAITING);
        stateBox.addItem(STATE_GRADED);
        internshipBox.setPreferredSize(new Dimension(300, 34));
        stateBox.setPreferredSize(new Dimension(160, 34));
        internshipBox.addActionListener(e -> onFilterChanged());
        stateBox.addActionListener(e -> onFilterChanged());
        addFilterComponent(internshipBox);
        addFilterComponent(stateBox);

        setDetailComponent(buildReviewPanel());
        saveGradeButton = addToolbarButton("Save Grade", this::onSaveGrade);
        addToolbarButton("Mark Internship Completed", this::onMarkCompleted);
        addToolbarButton("Refresh", this::reloadAll);

        getTable().getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelectedEntry();
            }
        });

        // Reload every time the admin opens this page, so new entries always show
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                reloadAll();
            }
        });

        showSelectedEntry();
        reloadAll();
    }

    @Override
    protected List<LogbookRecord> loadItems() throws ServiceException {
        return service.listRecords(filterInternshipId, filterGraded);
    }

    @Override
    protected Object[] toRow(LogbookRecord r) {
        return new Object[]{
                r.getStudentName(),
                r.getInternshipTitle(),
                r.getWeekNo(),
                dateOf(r.getSubmittedOn()),
                r.getGrade() == null ? "SUBMITTED" : "GRADED",
                r.getGrade() == null ? "" : r.getGrade() + "/10"
        };
    }

    /** After a reload: re-select the entry that was just graded, if it is still in the list. */
    @Override
    protected void onLoaded() {
        if (reselectEntryId > 0) {
            List<LogbookRecord> records = getItems();
            for (int i = 0; i < records.size(); i++) {
                if (records.get(i).getEntryId() == reselectEntryId) {
                    int viewRow = getTable().convertRowIndexToView(i);
                    if (viewRow >= 0) {
                        getTable().setRowSelectionInterval(viewRow, viewRow);
                    }
                    break;
                }
            }
            reselectEntryId = 0;
        }
    }

    // ---------------------------------------------------------------- UI

    private JPanel buildReviewPanel() {
        infoLabel.setFont(infoLabel.getFont().deriveFont(Font.BOLD));
        summaryArea.setEditable(false);
        summaryArea.setLineWrap(true);
        summaryArea.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(summaryArea);
        scroll.setPreferredSize(new Dimension(400, 100));

        gradeSpinner.setPreferredSize(new Dimension(80, 32));
        remarksField.setPreferredSize(new Dimension(420, 32));
        remarksField.putClientProperty("JTextField.placeholderText", "Remarks for the student (optional)");

        JPanel gradeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        gradeRow.setOpaque(false);
        gradeRow.add(new JLabel("Grade (0-10)"));
        gradeRow.add(gradeSpinner);
        gradeRow.add(new JLabel("Remarks"));
        gradeRow.add(remarksField);

        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        panel.add(infoLabel, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(gradeRow, BorderLayout.SOUTH);
        return panel;
    }

    private void showSelectedEntry() {
        LogbookRecord r = getSelectedItem();
        if (r == null) {
            infoLabel.setText("Select an entry to read and grade it.");
            summaryArea.setText("");
            remarksField.setText("");
            setGradeControlsEnabled(false);
            return;
        }
        boolean open = r.getApplicationStatus() == ApplicationStatus.SELECTED;
        infoLabel.setText(r.getStudentName() + "   |   " + r.getInternshipTitle() + " (" + r.getCompanyName()
                + ")   |   Week " + r.getWeekNo()
                + "   |   submitted " + dateOf(r.getSubmittedOn()).format(DATE_FORMAT)
                + (open ? "" : "   |   internship completed, grades are locked"));
        summaryArea.setText(r.getSummary() == null ? "" : r.getSummary());
        summaryArea.setCaretPosition(0);
        gradeSpinner.setValue(r.getGrade() == null ? 8 : r.getGrade());
        remarksField.setText(r.getRemarks() == null ? "" : r.getRemarks());
        setGradeControlsEnabled(open);
    }

    private void setGradeControlsEnabled(boolean enabled) {
        gradeSpinner.setEnabled(enabled);
        remarksField.setEnabled(enabled);
        saveGradeButton.setEnabled(enabled);
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

        String state = (String) stateBox.getSelectedItem();
        if (STATE_AWAITING.equals(state)) {
            filterGraded = Boolean.FALSE;
        } else if (STATE_GRADED.equals(state)) {
            filterGraded = Boolean.TRUE;
        } else {
            filterGraded = null;
        }
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

    private void onSaveGrade() {
        LogbookRecord r = getSelectedItem();
        if (r == null) {
            showInfo("Select an entry first.");
            return;
        }
        if (r.getApplicationStatus() != ApplicationStatus.SELECTED) {
            showInfo("This internship is completed, so grades can no longer be changed.");
            return;
        }
        try {
            gradeSpinner.commitEdit();   // accept anything typed into the spinner
        } catch (ParseException e) {
            showError("Please enter a grade between 0 and 10.");
            return;
        }
        int grade = ((Number) gradeSpinner.getValue()).intValue();
        String remarks = remarksField.getText();
        int entryId = r.getEntryId();
        int adminId = SessionManager.getInstance().getCurrentUser().getUserId();

        runTask(() -> {
            try {
                service.grade(entryId, grade, remarks, adminId);
            } catch (ServiceException e) {
                SwingUtilities.invokeLater(this::refresh);   // the list may be out of date, so reload it
                throw e;
            }
        }, () -> {
            reselectEntryId = entryId;
            refresh();
        });
    }

    private void onMarkCompleted() {
        LogbookRecord r = getSelectedItem();
        if (r == null) {
            showInfo("Select one of the student's entries first.");
            return;
        }
        if (r.getApplicationStatus() == ApplicationStatus.COMPLETED) {
            showInfo("This internship is already completed.");
            return;
        }
        String question = "Mark \"" + r.getInternshipTitle() + "\" as COMPLETED for " + r.getStudentName()
                + "?\nEvery week must already be graded. This cannot be undone.";
        if (!confirm(question)) {
            return;
        }
        int applicationId = r.getApplicationId();

        runTask(() -> {
            try {
                service.complete(applicationId);
            } catch (ServiceException e) {
                SwingUtilities.invokeLater(this::refresh);
                throw e;
            }
        }, () -> {
            showInfo("The internship is now completed. The student can see it on their Track Application timeline.");
            refresh();
        });
    }

    // ---------------------------------------------------------------- Helpers

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

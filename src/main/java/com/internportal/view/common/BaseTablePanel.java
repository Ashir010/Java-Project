package com.internportal.view.common;

import com.internportal.service.ServiceException;
import com.internportal.service.ServiceTask;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

/**
 * Reusable screen: search box on top, a sortable table, and a row of buttons below.
 * Subclasses provide the data (loadItems) and how one item becomes a table row (toRow).
 * Subclasses must call refresh() at the end of their own constructor.
 */
public abstract class BaseTablePanel<T> extends JPanel {

    private final DefaultTableModel model;
    private final JTable table;
    private final TableRowSorter<DefaultTableModel> sorter;
    private final JTextField searchField = new JTextField();
    private final JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    private final JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    private final JLabel statusLabel = new JLabel(" ");
    private List<T> items = new ArrayList<>();

    protected BaseTablePanel(String[] columns) {
        super(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            // Lets numbers and dates sort properly instead of as text
            @Override
            public Class<?> getColumnClass(int column) {
                Object value = getRowCount() > 0 ? getValueAt(0, column) : null;
                return value != null ? value.getClass() : Object.class;
            }
        };

        table = new JTable(model);
        table.setRowHeight(30);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        searchField.putClientProperty("JTextField.placeholderText", "Search...");
        searchField.putClientProperty("JTextField.showClearButton", true);
        searchField.setPreferredSize(new Dimension(280, 34));
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { applyFilter(); }

            @Override
            public void removeUpdate(DocumentEvent e) { applyFilter(); }

            @Override
            public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });

        topBar.setOpaque(false);
        topBar.add(searchField);

        buttonBar.setOpaque(false);
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(buttonBar, BorderLayout.WEST);
        bottom.add(statusLabel, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    // ---------------------------------------------------------------- For subclasses to implement

    /** Runs on a background thread. */
    protected abstract List<T> loadItems() throws Exception;

    /** Converts one item to the cells of one table row. Avoid null cells. */
    protected abstract Object[] toRow(T item);

    // ---------------------------------------------------------------- For subclasses to use

    protected JTable getTable() {
        return table;
    }

    protected JButton addToolbarButton(String text, Runnable action) {
        JButton button = new JButton(text);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(e -> action.run());
        buttonBar.add(button);
        return button;
    }

    /** Adds a control (for example a filter drop-down) to the top bar, next to the search box. */
    protected void addFilterComponent(JComponent component) {
        topBar.add(Box.createHorizontalStrut(10));
        topBar.add(component);
    }

    /** The item for the selected row, or null if nothing is selected. */
    protected T getSelectedItem() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            return null;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        return modelRow < items.size() ? items.get(modelRow) : null;
    }

    /** Runs a task off the UI thread. On success runs onSuccess, on failure shows the error. */
    protected void runTask(ServiceTask task, Runnable onSuccess) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws ServiceException {
                task.run();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    onSuccess.run();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    showError(messageOf(ex.getCause()));
                }
            }
        }.execute();
    }

    protected void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    protected void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    protected boolean confirm(String message) {
        return JOptionPane.showConfirmDialog(this, message, "Confirm",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    protected static String messageOf(Throwable t) {
        return t instanceof ServiceException ? t.getMessage() : "Unexpected error: " + t;
    }

    // ---------------------------------------------------------------- Loading and filtering

    /** Reloads the table from the database without freezing the window. */
    public void refresh() {
        statusLabel.setText("Loading...");
        new SwingWorker<List<T>, Void>() {
            @Override
            protected List<T> doInBackground() throws Exception {
                return loadItems();
            }

            @Override
            protected void done() {
                try {
                    items = get();
                    model.setRowCount(0);
                    for (T item : items) {
                        model.addRow(toRow(item));
                    }
                    statusLabel.setText(items.size() + " record(s)");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    statusLabel.setText(" ");
                    showError(messageOf(ex.getCause()));
                }
            }
        }.execute();
    }

    private void applyFilter() {
        String text = searchField.getText().trim();
        if (text.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(text)));
        }
    }
}

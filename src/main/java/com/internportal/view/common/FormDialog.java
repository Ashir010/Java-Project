package com.internportal.view.common;

import com.internportal.service.ServiceException;
import com.internportal.service.ServiceTask;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Window;
import java.util.concurrent.ExecutionException;

/**
 * Base for add/edit pop-ups. Subclasses build the form, validate it, and describe the save.
 * On Save: validateInput() runs, then buildSaveTask() captures the values and the task runs
 * in the background. Errors stay inside the dialog so the user does not lose what they typed.
 */
public abstract class FormDialog extends JDialog {

    private static final Color ERROR_COLOR = new Color(0xC62828);
    private static final Color PRIMARY_COLOR = new Color(0x1E5AA8);

    private final JLabel messageLabel = new JLabel(" ");
    private final JButton saveButton = new JButton("Save");
    private final JButton cancelButton = new JButton("Cancel");
    private boolean saved = false;

    protected FormDialog(Window owner, String title) {
        super(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    /** Runs on the UI thread. Return an error message, or null if the input is valid. */
    protected abstract String validateInput();

    /** Runs on the UI thread. Read the fields here and return the work to run in the background. */
    protected abstract ServiceTask buildSaveTask();

    /** True if the user saved successfully (check after setVisible(true) returns). */
    public boolean isSaved() {
        return saved;
    }

    /** Call once at the end of the subclass constructor, after the form is built. */
    protected final void buildDialog(JPanel form) {
        JPanel root = new JPanel(new BorderLayout(0, 8));
        root.setBorder(BorderFactory.createEmptyBorder(18, 24, 14, 24));
        root.add(form, BorderLayout.CENTER);

        messageLabel.setForeground(ERROR_COLOR);
        messageLabel.setPreferredSize(new Dimension(320, 40));

        saveButton.setBackground(PRIMARY_COLOR);
        saveButton.setForeground(Color.WHITE);
        saveButton.addActionListener(e -> onSave());
        cancelButton.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(cancelButton);
        buttons.add(saveButton);

        JPanel south = new JPanel(new BorderLayout(0, 6));
        south.add(messageLabel, BorderLayout.NORTH);
        south.add(buttons, BorderLayout.SOUTH);
        root.add(south, BorderLayout.SOUTH);

        setContentPane(root);
        getRootPane().setDefaultButton(saveButton);
        pack();
        setResizable(false);
        setLocationRelativeTo(getOwner());
    }

    /** Adds "label | field" as one row of a GridBagLayout form. */
    protected static void addRow(JPanel form, int row, String labelText, JComponent field) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.NORTHWEST;
        labelConstraints.insets = new Insets(8, 0, 8, 14);
        form.add(new JLabel(labelText), labelConstraints);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = row;
        fieldConstraints.weightx = 1;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.insets = new Insets(4, 0, 4, 0);
        form.add(field, fieldConstraints);
    }

    // ---------------------------------------------------------------- Saving

    private void onSave() {
        String error = validateInput();
        if (error != null) {
            showError(error);
            return;
        }
        clearError();

        ServiceTask task = buildSaveTask();
        setBusy(true);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws ServiceException {
                task.run();
                return null;
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    get();
                    saved = true;
                    dispose();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    showError(cause instanceof ServiceException
                            ? cause.getMessage()
                            : "Unexpected error: " + cause);
                }
            }
        }.execute();
    }

    private void setBusy(boolean busy) {
        saveButton.setEnabled(!busy);
        cancelButton.setEnabled(!busy);
        saveButton.setText(busy ? "Saving..." : "Save");
    }

    private void showError(String text) {
        String safe = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        messageLabel.setText("<html><div style='width:300px'>" + safe + "</div></html>");
    }

    private void clearError() {
        messageLabel.setText(" ");
    }
}

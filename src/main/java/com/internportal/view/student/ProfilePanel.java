package com.internportal.view.student;

import com.internportal.model.ResumeData;
import com.internportal.model.StudentProfile;
import com.internportal.service.ProfileService;
import com.internportal.service.ServiceException;
import com.internportal.util.Courses;
import com.internportal.util.ResumeStorage;
import com.internportal.util.SessionManager;
import com.internportal.util.Validator;
import com.internportal.view.common.ResumeOpener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/** Student screen: edit details, upload a resume, and auto-fill the form from it. */
public class ProfilePanel extends JPanel {

    private static final Color PRIMARY_COLOR = new Color(0x1E5AA8);
    private static final Color ERROR_COLOR = new Color(0xC62828);
    private static final Color SUCCESS_COLOR = new Color(0x2E7D32);
    private static final int FIELD_WIDTH = 360;

    private final ProfileService service = new ProfileService();
    private final int studentId;

    private final JTextField nameField = new JTextField();
    private final JTextField emailField = new JTextField();
    private final JTextField phoneField = new JTextField();
    private final JComboBox<String> courseBox = new JComboBox<>(Courses.ALL);
    private final JTextArea skillsArea = new JTextArea(4, 24);
    private final JLabel resumeLabel = new JLabel("No resume uploaded yet");
    private final JButton uploadButton = new JButton("Upload Resume...");
    private final JButton viewButton = new JButton("View Resume");
    private final JButton autofillButton = new JButton("Auto-fill from Resume");
    private final JButton saveButton = new JButton("Save Profile");
    private final JLabel messageLabel = new JLabel(" ", SwingConstants.CENTER);

    private String resumePath;   // stored path of the current resume, or null

    public ProfilePanel() {
        super(new BorderLayout());
        studentId = SessionManager.getInstance().getCurrentUser().getUserId();

        emailField.setEditable(false);
        skillsArea.setLineWrap(true);
        skillsArea.setWrapStyleWord(true);
        phoneField.putClientProperty("JTextField.placeholderText", "10-digit mobile number");
        skillsArea.putClientProperty("JTextField.placeholderText", "e.g. Java, MySQL, Swing");

        // Scrolls if the window is small, and keeps the card centred when it is large
        JScrollPane scroll = new JScrollPane(buildContent());
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        uploadButton.addActionListener(e -> onUpload());
        viewButton.addActionListener(e -> ResumeOpener.open(this, resumePath));
        autofillButton.addActionListener(e -> onAutofill());
        saveButton.addActionListener(e -> onSave());

        loadProfile();
    }

    // ---------------------------------------------------------------- UI

    private JPanel buildContent() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(new Color(0xF2F5F9));
        outer.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));
        outer.add(buildCard());
        return outer;
    }

    private JPanel buildCard() {
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createEmptyBorder(24, 40, 24, 40));
        card.putClientProperty("FlatLaf.style", "arc: 16");

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        int row = 0;

        JLabel title = new JLabel("MY PROFILE", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setForeground(PRIMARY_COLOR);
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 14, 0);
        card.add(title, gbc);

        row = addField(card, gbc, row, "Full name", nameField, 36);
        row = addField(card, gbc, row, "Email (your login, cannot be changed)", emailField, 36);
        row = addField(card, gbc, row, "Phone", phoneField, 36);
        row = addField(card, gbc, row, "Course", courseBox, 36);
        row = addField(card, gbc, row, "Skills (comma separated)", new JScrollPane(skillsArea), 90);

        gbc.gridy = row++;
        gbc.insets = new Insets(6, 0, 2, 0);
        card.add(new JLabel("Resume (PDF or DOCX, max 5 MB)"), gbc);

        resumeLabel.setForeground(Color.GRAY);
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 6, 0);
        card.add(resumeLabel, gbc);

        styleButton(uploadButton);
        gbc.gridy = row++;
        card.add(uploadButton, gbc);

        styleButton(viewButton);
        styleButton(autofillButton);
        JPanel pair = new JPanel(new GridLayout(1, 2, 8, 0));
        pair.setOpaque(false);
        pair.add(viewButton);
        pair.add(autofillButton);
        gbc.gridy = row++;
        card.add(pair, gbc);

        messageLabel.setPreferredSize(new Dimension(FIELD_WIDTH, 46));
        gbc.gridy = row++;
        gbc.insets = new Insets(8, 0, 4, 0);
        card.add(messageLabel, gbc);

        styleButton(saveButton);
        saveButton.setBackground(PRIMARY_COLOR);
        saveButton.setForeground(Color.WHITE);
        saveButton.setFont(saveButton.getFont().deriveFont(Font.BOLD));
        gbc.gridy = row;
        gbc.insets = new Insets(0, 0, 0, 0);
        card.add(saveButton, gbc);

        return card;
    }

    /** Adds a label and a field below each other. Returns the next free grid row. */
    private int addField(JPanel card, GridBagConstraints gbc, int row,
                         String label, JComponent field, int height) {
        gbc.gridy = row++;
        gbc.insets = new Insets(2, 0, 2, 0);
        card.add(new JLabel(label), gbc);

        field.setPreferredSize(new Dimension(FIELD_WIDTH, height));
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 8, 0);
        card.add(field, gbc);
        return row;
    }

    private static void styleButton(JButton button) {
        button.setPreferredSize(new Dimension(FIELD_WIDTH, 38));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.putClientProperty("JButton.buttonType", "roundRect");
    }

    // ---------------------------------------------------------------- Loading

    private void loadProfile() {
        setBusy(true);
        new SwingWorker<StudentProfile, Void>() {
            @Override
            protected StudentProfile doInBackground() throws ServiceException {
                return service.load(studentId);
            }

            @Override
            protected void done() {
                try {
                    StudentProfile p = get();
                    nameField.setText(p.getFullName());
                    emailField.setText(p.getEmail());
                    phoneField.setText(p.getPhone() == null ? "" : p.getPhone());
                    selectCourse(p.getCourse());
                    skillsArea.setText(p.getSkills() == null ? "" : p.getSkills());
                    skillsArea.setCaretPosition(0);
                    resumePath = p.getResumePath();
                    updateResumeLabel();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    showError(errorText(ex.getCause()));
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    private void selectCourse(String course) {
        if (course == null) {
            return;
        }
        for (int i = 0; i < courseBox.getItemCount(); i++) {
            if (courseBox.getItemAt(i).equals(course)) {
                courseBox.setSelectedIndex(i);
                return;
            }
        }
        courseBox.addItem(course);   // a value that is no longer in the list is kept, not lost
        courseBox.setSelectedItem(course);
    }

    // ---------------------------------------------------------------- Actions

    private void onUpload() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose your resume (PDF or DOCX, max 5 MB)");
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileFilter(new FileNameExtensionFilter("Resume (PDF, DOCX)", "pdf", "docx"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();

        clearMessage();
        setBusy(true);
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws ServiceException {
                return service.uploadResume(studentId, file);
            }

            @Override
            protected void done() {
                try {
                    resumePath = get();
                    updateResumeLabel();
                    showSuccess("Resume uploaded. Click \"Auto-fill from Resume\" to read it.");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    showError(errorText(ex.getCause()));
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    private void onAutofill() {
        if (resumePath == null) {
            showError("Upload your resume first.");
            return;
        }
        clearMessage();
        setBusy(true);
        new SwingWorker<ResumeData, Void>() {
            @Override
            protected ResumeData doInBackground() throws ServiceException {
                return service.readResume(studentId);
            }

            @Override
            protected void done() {
                try {
                    applyResumeData(get());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    showError(errorText(ex.getCause()));
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    /** Fills the phone and skills fields. Nothing is saved until the student clicks Save Profile. */
    private void applyResumeData(ResumeData data) {
        if (data.isEmpty()) {
            showError("No phone number or known skills were found. The resume may be a scanned image. "
                    + "Please fill the fields yourself.");
            return;
        }
        List<String> filled = new ArrayList<>();
        if (data.getPhone() != null) {
            phoneField.setText(data.getPhone());
            filled.add("phone number");
        }
        if (!data.getSkills().isEmpty()) {
            skillsArea.setText(mergeSkills(skillsArea.getText(), data.getSkills()));
            skillsArea.setCaretPosition(0);
            int count = data.getSkills().size();
            filled.add(count + (count == 1 ? " skill" : " skills"));
        }
        showSuccess("Filled from your resume: " + String.join(" and ", filled)
                + ". Please check the details, then click Save Profile.");
    }

    private void onSave() {
        String name = nameField.getText().trim();
        String phone = phoneField.getText().trim();
        String course = (String) courseBox.getSelectedItem();
        String skills = skillsArea.getText();

        if (!Validator.isValidName(name)) {
            nameField.requestFocusInWindow();
            showError("Please enter your full name (letters only).");
            return;
        }
        if (!Validator.isValidPhone(phone)) {
            phoneField.requestFocusInWindow();
            showError("Enter a valid 10-digit mobile number.");
            return;
        }
        clearMessage();
        setBusy(true);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws ServiceException {
                service.save(studentId, name, phone, course, skills);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    SessionManager.getInstance().getCurrentUser().setFullName(name);
                    showSuccess("Profile saved.");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    showError(errorText(ex.getCause()));
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    // ---------------------------------------------------------------- Helpers

    private void setBusy(boolean busy) {
        uploadButton.setEnabled(!busy);
        saveButton.setEnabled(!busy);
        viewButton.setEnabled(!busy && resumePath != null);
        autofillButton.setEnabled(!busy && resumePath != null);
    }

    private void updateResumeLabel() {
        if (resumePath == null) {
            resumeLabel.setText("No resume uploaded yet");
            resumeLabel.setForeground(Color.GRAY);
        } else {
            resumeLabel.setText("Resume uploaded (" + ResumeStorage.typeLabel(resumePath) + ")");
            resumeLabel.setForeground(SUCCESS_COLOR);
        }
    }

    private static String mergeSkills(String existing, List<String> found) {
        Map<String, String> merged = new LinkedHashMap<>();
        for (String part : existing.split("[,\\n]")) {
            String skill = part.trim();
            if (!skill.isEmpty()) {
                merged.putIfAbsent(skill.toLowerCase(Locale.ROOT), skill);
            }
        }
        for (String skill : found) {
            merged.putIfAbsent(skill.toLowerCase(Locale.ROOT), skill);
        }
        return String.join(", ", merged.values());
    }

    private static String errorText(Throwable t) {
        return t instanceof ServiceException ? t.getMessage() : "Unexpected error: " + t;
    }

    private void showError(String text) {
        showMessage(text, ERROR_COLOR);
    }

    private void showSuccess(String text) {
        showMessage(text, SUCCESS_COLOR);
    }

    private void showMessage(String text, Color color) {
        String safe = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        messageLabel.setForeground(color);
        messageLabel.setText("<html><div style='text-align:center;width:" + (FIELD_WIDTH - 20) + "px'>"
                + safe + "</div></html>");
    }

    private void clearMessage() {
        messageLabel.setText(" ");
    }
}

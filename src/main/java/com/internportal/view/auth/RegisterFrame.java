package com.internportal.view.auth;

import com.internportal.service.AuthException;
import com.internportal.service.RegistrationService;
import com.internportal.util.Validator;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.concurrent.ExecutionException;

/** Screen 2: Student registration. */
public class RegisterFrame extends JFrame {

    private static final Color ERROR_COLOR = new Color(0xC62828);
    private static final Color PRIMARY_COLOR = new Color(0x1E5AA8);

    private static final String[] COURSES = {
            "B.Tech CSE", "B.Tech IT", "B.Tech ECE", "B.Tech (Other)",
            "BCA", "MCA", "B.Sc (CS/IT)", "M.Tech", "Other"
    };

    private final JTextField nameField = new JTextField();
    private final JTextField emailField = new JTextField();
    private final JTextField phoneField = new JTextField();
    private final JComboBox<String> courseBox = new JComboBox<>(COURSES);
    private final JPasswordField passwordField = new JPasswordField();
    private final JPasswordField confirmField = new JPasswordField();
    private final JButton registerButton = new JButton("Register");
    private final JButton backButton = new JButton("Back to Login");
    private final JLabel messageLabel = new JLabel(" ", SwingConstants.CENTER);

    private final RegistrationService registrationService = new RegistrationService();

    public RegisterFrame() {
        setTitle("Internship Management Portal - Student Registration");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setContentPane(buildContent());
        wireActions();

        pack();
        setLocationRelativeTo(null);
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

        JLabel title = new JLabel("STUDENT REGISTRATION", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setForeground(PRIMARY_COLOR);
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 2, 0);
        card.add(title, gbc);

        JLabel subtitle = new JLabel("Create your account", SwingConstants.CENTER);
        subtitle.setForeground(Color.GRAY);
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 14, 0);
        card.add(subtitle, gbc);

        passwordField.putClientProperty("JPasswordField.showRevealButton", true);
        confirmField.putClientProperty("JPasswordField.showRevealButton", true);

        row = addField(card, gbc, row, "Full name", nameField, "e.g. Riya Sharma");
        row = addField(card, gbc, row, "Email", emailField, "you@example.com");
        row = addField(card, gbc, row, "Phone", phoneField, "10-digit mobile number");
        row = addField(card, gbc, row, "Course", courseBox, null);
        row = addField(card, gbc, row, "Password", passwordField, "At least 8 characters, letters and numbers");
        row = addField(card, gbc, row, "Confirm password", confirmField, "Re-enter your password");

        messageLabel.setForeground(ERROR_COLOR);
        messageLabel.setPreferredSize(new Dimension(300, 38));
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 8, 0);
        card.add(messageLabel, gbc);

        registerButton.setPreferredSize(new Dimension(300, 40));
        registerButton.setBackground(PRIMARY_COLOR);
        registerButton.setForeground(Color.WHITE);
        registerButton.setFont(registerButton.getFont().deriveFont(Font.BOLD));
        registerButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        registerButton.putClientProperty("JButton.buttonType", "roundRect");
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 8, 0);
        card.add(registerButton, gbc);

        backButton.setPreferredSize(new Dimension(300, 40));
        backButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backButton.putClientProperty("JButton.buttonType", "roundRect");
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 0, 0);
        card.add(backButton, gbc);

        return card;
    }

    /** Adds a label and a field below each other. Returns the next free grid row. */
    private int addField(JPanel card, GridBagConstraints gbc, int row,
                         String label, JComponent field, String placeholder) {
        gbc.gridy = row++;
        gbc.insets = new Insets(2, 0, 2, 0);
        card.add(new JLabel(label), gbc);

        if (placeholder != null) {
            field.putClientProperty("JTextField.placeholderText", placeholder);
        }
        field.setPreferredSize(new Dimension(300, 36));
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 8, 0);
        card.add(field, gbc);
        return row;
    }

    // ---------------------------------------------------------------- Actions

    private void wireActions() {
        registerButton.addActionListener(e -> onRegisterClicked());
        backButton.addActionListener(e -> goBackToLogin());
        getRootPane().setDefaultButton(registerButton);   // Enter = Register
    }

    private void onRegisterClicked() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String course = (String) courseBox.getSelectedItem();
        String password = new String(passwordField.getPassword());
        String confirm = new String(confirmField.getPassword());

        String error = validate(name, email, phone, password, confirm);
        if (error != null) {
            showError(error);
            return;
        }
        clearMessage();
        setLoading(true);

        // Run the database work off the UI thread so the window never freezes
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws AuthException {
                registrationService.registerStudent(name, email, phone, course, password);
                return null;
            }

            @Override
            protected void done() {
                setLoading(false);
                try {
                    get();
                    onRegistered();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    showError(cause instanceof AuthException
                            ? cause.getMessage()
                            : "Unexpected error: " + cause);
                }
            }
        }.execute();
    }

    private void onRegistered() {
        JOptionPane.showMessageDialog(this,
                "Registration successful!\nYou can now log in with your email and password.",
                "Registered", JOptionPane.INFORMATION_MESSAGE);
        goBackToLogin();
    }

    private void goBackToLogin() {
        new LoginFrame().setVisible(true);
        dispose();
    }

    // ---------------------------------------------------------------- Helpers

    /** Returns an error message, or null if the input is valid. */
    private String validate(String name, String email, String phone, String password, String confirm) {
        if (!Validator.isValidName(name)) {
            nameField.requestFocusInWindow();
            return "Please enter your full name (letters only).";
        }
        if (!Validator.isValidEmail(email)) {
            emailField.requestFocusInWindow();
            return "Please enter a valid email address.";
        }
        if (!Validator.isValidPhone(phone)) {
            phoneField.requestFocusInWindow();
            return "Enter a valid 10-digit mobile number.";
        }
        String passwordError = Validator.passwordError(password);
        if (passwordError != null) {
            passwordField.requestFocusInWindow();
            return passwordError;
        }
        if (!password.equals(confirm)) {
            confirmField.requestFocusInWindow();
            return "Passwords do not match.";
        }
        return null;
    }

    private void setLoading(boolean loading) {
        registerButton.setEnabled(!loading);
        backButton.setEnabled(!loading);
        registerButton.setText(loading ? "Creating account..." : "Register");
    }

    private void showError(String text) {
        // HTML lets long messages wrap instead of stretching the window
        messageLabel.setText("<html><div style='text-align:center;width:260px'>" + text + "</div></html>");
    }

    private void clearMessage() {
        messageLabel.setText(" ");
    }
}

package com.internportal.view.auth;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import com.internportal.model.User;
import com.internportal.service.AuthException;
import com.internportal.service.AuthService;
import com.internportal.util.SessionManager;
import com.internportal.view.Navigator;
import javax.swing.SwingWorker;
import java.util.concurrent.ExecutionException;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.regex.Pattern;

/**
 * Screen 1: Login.
 * Step 1 builds the UI and input validation only.
 * Step 2 will connect onLoginClicked() to the database (AuthService / UserDAO).
 */
public class LoginFrame extends JFrame {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final Color ERROR_COLOR = new Color(0xC62828);
    private static final Color PRIMARY_COLOR = new Color(0x1E5AA8);

    private final JTextField emailField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JButton loginButton = new JButton("Login");
    private final JButton registerButton = new JButton("Register");
    private final JLabel messageLabel = new JLabel(" ", SwingConstants.CENTER);
    private final AuthService authService = new AuthService();

    public LoginFrame() {
        setTitle("Internship Management Portal - Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setContentPane(buildContent());
        wireActions();

        setMinimumSize(new Dimension(460, 520));
        pack();
        setLocationRelativeTo(null);   // center on screen
    }

    // ---------------------------------------------------------------- UI

    private JPanel buildContent() {
        // Outer panel centers the card
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(new Color(0xF2F5F9));
        outer.add(buildCard());
        return outer;
    }

    private JPanel buildCard() {
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createEmptyBorder(32, 40, 32, 40));
        card.putClientProperty("FlatLaf.style", "arc: 16");   // rounded corners (FlatLaf)

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 0, 4, 0);

        // Title
        JLabel title = new JLabel("INTERNSHIP MANAGEMENT PORTAL", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setForeground(PRIMARY_COLOR);
        gbc.gridy = 0;
        card.add(title, gbc);

        JLabel subtitle = new JLabel("Sign in to continue", SwingConstants.CENTER);
        subtitle.setForeground(Color.GRAY);
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 20, 0);
        card.add(subtitle, gbc);

        // Email
        gbc.insets = new Insets(4, 0, 2, 0);
        gbc.gridy = 2;
        card.add(new JLabel("Email"), gbc);

        emailField.putClientProperty("JTextField.placeholderText", "you@example.com");
        emailField.setPreferredSize(new Dimension(300, 36));
        gbc.insets = new Insets(0, 0, 10, 0);
        gbc.gridy = 3;
        card.add(emailField, gbc);

        // Password
        gbc.insets = new Insets(4, 0, 2, 0);
        gbc.gridy = 4;
        card.add(new JLabel("Password"), gbc);

        passwordField.putClientProperty("JTextField.placeholderText", "Enter your password");
        passwordField.putClientProperty("JPasswordField.showRevealButton", true);  // eye icon
        passwordField.setPreferredSize(new Dimension(300, 36));
        gbc.insets = new Insets(0, 0, 6, 0);
        gbc.gridy = 5;
        card.add(passwordField, gbc);

        // Message / error line
        messageLabel.setForeground(ERROR_COLOR);
        gbc.insets = new Insets(2, 0, 10, 0);
        gbc.gridy = 6;
        card.add(messageLabel, gbc);

        // Login button
        loginButton.setPreferredSize(new Dimension(300, 40));
        loginButton.setBackground(PRIMARY_COLOR);
        loginButton.setForeground(Color.WHITE);
        loginButton.setFont(loginButton.getFont().deriveFont(Font.BOLD));
        loginButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        loginButton.putClientProperty("JButton.buttonType", "roundRect");
        gbc.insets = new Insets(0, 0, 8, 0);
        gbc.gridy = 7;
        card.add(loginButton, gbc);

        // Register button
        registerButton.setPreferredSize(new Dimension(300, 40));
        registerButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        registerButton.putClientProperty("JButton.buttonType", "roundRect");
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.gridy = 8;
        card.add(registerButton, gbc);

        return card;
    }

    // ---------------------------------------------------------------- Actions

    private void wireActions() {
        loginButton.addActionListener(e -> onLoginClicked());
        registerButton.addActionListener(e -> onRegisterClicked());

        // Pressing Enter anywhere on the form triggers Login
        getRootPane().setDefaultButton(loginButton);
    }

    private void onLoginClicked() {
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());

        String error = validate(email, password);
        if (error != null) {
            showError(error);
            return;
        }
        clearMessage();
        setLoading(true);

        // Run the database check off the UI thread so the window never freezes
        new SwingWorker<User, Void>() {
            @Override
            protected User doInBackground() throws AuthException {
                return authService.login(email, password);
            }

            @Override
            protected void done() {
                setLoading(false);
                try {
                    onLoginSuccess(get());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    showError(cause instanceof AuthException
                            ? cause.getMessage()
                            : "Unexpected error: " + cause);
                    passwordField.setText("");
                }
            }
        }.execute();
    }

    private void onLoginSuccess(User user) {
        SessionManager.getInstance().login(user);
        passwordField.setText("");

        Navigator.openDashboardFor(user);   // open the dashboard first, then close the login window
        dispose();
    }

    private void setLoading(boolean loading) {
        loginButton.setEnabled(!loading);
        registerButton.setEnabled(!loading);
        loginButton.setText(loading ? "Signing in..." : "Login");
    }

    private void onRegisterClicked() {
        new RegisterFrame().setVisible(true);
        dispose();
    }

    // ---------------------------------------------------------------- Helpers

    /** Returns an error message, or null if the input is valid. */
    private String validate(String email, String password) {
        if (email.isEmpty()) {
            emailField.requestFocusInWindow();
            return "Please enter your email.";
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            emailField.requestFocusInWindow();
            return "Please enter a valid email address.";
        }
        if (password.isEmpty()) {
            passwordField.requestFocusInWindow();
            return "Please enter your password.";
        }
        return null;
    }

    private void showError(String text) {
        messageLabel.setText(text);
    }

    private void clearMessage() {
        messageLabel.setText(" ");
    }
}

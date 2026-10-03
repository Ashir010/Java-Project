package com.internportal;

import com.formdev.flatlaf.FlatLightLaf;
import com.internportal.service.AuthException;
import com.internportal.service.AuthService;
import com.internportal.view.auth.LoginFrame;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FlatLightLaf.setup();

            // Create the default admin on first run (does nothing if one exists)
            try {
                new AuthService().ensureDefaultAdmin();
            } catch (AuthException e) {
                JOptionPane.showMessageDialog(null, e.getMessage(),
                        "Database problem", JOptionPane.WARNING_MESSAGE);
            }

            new LoginFrame().setVisible(true);
        });
    }
}

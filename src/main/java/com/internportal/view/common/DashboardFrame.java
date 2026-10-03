package com.internportal.view.common;

import com.internportal.model.Role;
import com.internportal.model.User;
import com.internportal.util.SessionManager;
import com.internportal.view.Navigator;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shared dashboard layout: header (name + Logout), sidebar (menu buttons)
 * and a CardLayout content area. Subclasses only add their pages.
 */
public abstract class DashboardFrame extends JFrame {

    private static final Color HEADER_COLOR = new Color(0x1E5AA8);
    private static final Color SIDEBAR_COLOR = new Color(0x16427A);
    private static final Color SIDEBAR_ACTIVE_COLOR = new Color(0x2F6FBF);
    private static final Color CONTENT_COLOR = new Color(0xF2F5F9);

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(cardLayout);
    private final JPanel menuPanel = new JPanel(new GridLayout(0, 1, 0, 4));
    private final Map<String, JButton> menuButtons = new LinkedHashMap<>();

    protected DashboardFrame(String screenTitle, Role requiredRole) {
        // Role check: a dashboard refuses to open unless the matching user is logged in
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null || user.getRole() != requiredRole) {
            throw new IllegalStateException("Not logged in as " + requiredRole);
        }

        setTitle("Internship Management Portal - " + screenTitle);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout());
        root.add(buildHeader(user), BorderLayout.NORTH);
        root.add(buildSidebar(), BorderLayout.WEST);
        contentPanel.setBackground(CONTENT_COLOR);
        root.add(contentPanel, BorderLayout.CENTER);
        setContentPane(root);

        setMinimumSize(new Dimension(960, 600));
        setSize(1180, 720);
        setLocationRelativeTo(null);
    }

    // ---------------------------------------------------------------- Pages (used by subclasses)

    /** Adds a sidebar button and its content panel. */
    protected void addPage(String name, JComponent page) {
        contentPanel.add(page, name);

        JButton button = new JButton(name);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 14f));
        button.setForeground(Color.WHITE);
        button.setBackground(SIDEBAR_COLOR);
        button.setBorder(BorderFactory.createEmptyBorder(11, 16, 11, 16));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(e -> showPage(name));

        menuButtons.put(name, button);
        menuPanel.add(button);
    }

    /** Shows a page and highlights its sidebar button. */
    protected void showPage(String name) {
        cardLayout.show(contentPanel, name);
        for (Map.Entry<String, JButton> entry : menuButtons.entrySet()) {
            entry.getValue().setBackground(
                    entry.getKey().equals(name) ? SIDEBAR_ACTIVE_COLOR : SIDEBAR_COLOR);
        }
    }

    // ---------------------------------------------------------------- UI

    private JPanel buildHeader(User user) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(HEADER_COLOR);
        header.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JLabel title = new JLabel("Internship Management Portal");
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        header.add(title, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        right.setOpaque(false);

        JLabel userLabel = new JLabel(user.getFullName() + "  |  " + displayRole(user.getRole()));
        userLabel.setForeground(Color.WHITE);
        right.add(userLabel);

        JButton logoutButton = new JButton("Logout");
        logoutButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logoutButton.addActionListener(e -> onLogoutClicked());
        right.add(logoutButton);

        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(SIDEBAR_COLOR);
        sidebar.setBorder(BorderFactory.createEmptyBorder(16, 10, 16, 10));
        sidebar.setPreferredSize(new Dimension(230, 0));

        menuPanel.setOpaque(false);
        sidebar.add(menuPanel, BorderLayout.NORTH);
        return sidebar;
    }

    // ---------------------------------------------------------------- Actions

    private void onLogoutClicked() {
        int choice = JOptionPane.showConfirmDialog(this,
                "Do you want to log out?", "Logout",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            SessionManager.getInstance().logout();
            Navigator.openLogin();   // open the login window first, then close this one
            dispose();
        }
    }

    private static String displayRole(Role role) {
        String text = role.name();
        return text.charAt(0) + text.substring(1).toLowerCase();
    }
}

package com.internportal.view;

import com.internportal.model.Role;
import com.internportal.model.User;
import com.internportal.view.admin.AdminDashboard;
import com.internportal.view.auth.LoginFrame;
import com.internportal.view.student.StudentDashboard;

import javax.swing.JFrame;

/** One place that decides which window opens next. */
public final class Navigator {

    private Navigator() { }

    public static void openLogin() {
        new LoginFrame().setVisible(true);
    }

    public static void openDashboardFor(User user) {
        JFrame dashboard;
        if (user.getRole() == Role.ADMIN) {
            dashboard = new AdminDashboard();
        } else if (user.getRole() == Role.STUDENT) {
            dashboard = new StudentDashboard();
        } else {
            throw new IllegalArgumentException("Unknown role: " + user.getRole());
        }
        dashboard.setVisible(true);
    }
}

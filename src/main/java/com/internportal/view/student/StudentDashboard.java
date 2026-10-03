package com.internportal.view.student;

import com.internportal.model.Role;
import com.internportal.view.common.DashboardFrame;
import com.internportal.view.common.PlaceholderPanel;

public class StudentDashboard extends DashboardFrame {

    public StudentDashboard() {
        super("Student Dashboard", Role.STUDENT);

        addPage("My Profile", new PlaceholderPanel("My Profile"));
        addPage("Browse Internships", new PlaceholderPanel("Browse Internships"));
        addPage("My Applications", new PlaceholderPanel("My Applications"));
        addPage("Weekly Logbook", new PlaceholderPanel("Weekly Logbook"));
        addPage("My Documents", new PlaceholderPanel("My Documents"));

        showPage("My Profile");
    }
}

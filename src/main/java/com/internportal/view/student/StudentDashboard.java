package com.internportal.view.student;

import com.internportal.model.Role;
import com.internportal.view.common.DashboardFrame;
import com.internportal.view.common.PlaceholderPanel;

public class StudentDashboard extends DashboardFrame {

    public StudentDashboard() {
        super("Student Dashboard", Role.STUDENT);

        addPage("My Profile", new ProfilePanel());
        addPage("Browse Internships", new BrowsePanel());
        addPage("My Applications", new MyApplicationsPanel());
        addPage("Weekly Logbook", new LogbookPanel());
        addPage("My Documents", new MyDocumentsPanel());

        showPage("My Profile");
    }
}

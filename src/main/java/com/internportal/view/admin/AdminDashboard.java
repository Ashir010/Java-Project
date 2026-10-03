package com.internportal.view.admin;

import com.internportal.model.Role;
import com.internportal.view.common.DashboardFrame;
import com.internportal.view.common.PlaceholderPanel;

public class AdminDashboard extends DashboardFrame {

    public AdminDashboard() {
        super("Admin Dashboard", Role.ADMIN);

        addPage("Overview", new PlaceholderPanel("Overview & Reports"));
        addPage("Students", new PlaceholderPanel("Students"));
        addPage("Companies & Internships", new CompaniesInternshipsPanel());
        addPage("Applications", new PlaceholderPanel("Applications"));
        addPage("Logbooks", new PlaceholderPanel("Logbooks"));
        addPage("Documents", new PlaceholderPanel("Documents"));

        showPage("Overview");
    }
}

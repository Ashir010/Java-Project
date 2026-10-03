package com.internportal.view.admin;

import com.internportal.view.common.BaseTablePanel;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;

/** Admin page with two tabs: Companies and Internships. */
public class CompaniesInternshipsPanel extends JPanel {

    public CompaniesInternshipsPanel() {
        super(new BorderLayout());
        setBackground(new Color(0xF2F5F9));
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Companies", new CompaniesPanel());
        tabs.addTab("Internships", new InternshipsPanel());

        // Reload the tab being opened so it never shows stale data
        tabs.addChangeListener(e -> {
            Component selected = tabs.getSelectedComponent();
            if (selected instanceof BaseTablePanel) {
                ((BaseTablePanel<?>) selected).refresh();
            }
        });

        add(tabs, BorderLayout.CENTER);
    }
}

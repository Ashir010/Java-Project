package com.internportal.view.student;

import com.internportal.model.Internship;
import com.internportal.model.InternshipListing;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.time.format.DateTimeFormatter;

/** Read-only pop-up with the full internship details and an Apply button. */
public class InternshipDetailDialog extends JDialog {

    private static final Color PRIMARY_COLOR = new Color(0x1E5AA8);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private boolean applyRequested = false;

    public InternshipDetailDialog(Window owner, InternshipListing listing) {
        super(owner, "Internship Details", Dialog.ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        Internship internship = listing.getInternship();

        JLabel title = new JLabel(internship.getTitle());
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setForeground(PRIMARY_COLOR);

        JLabel company = new JLabel(internship.getCompanyName());
        company.setForeground(Color.GRAY);

        JPanel header = new JPanel(new GridLayout(0, 1, 0, 2));
        header.add(title);
        header.add(company);

        JPanel facts = new JPanel(new GridLayout(0, 2, 12, 6));
        addFact(facts, "Stipend", String.format("INR %,.0f per month", internship.getStipend()));
        addFact(facts, "Duration", internship.getDurationWeeks() + " weeks");
        addFact(facts, "Apply by", internship.getDeadline().format(DATE_FORMAT));

        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.add(header, BorderLayout.NORTH);
        top.add(facts, BorderLayout.CENTER);

        String text = internship.getDescription() == null
                ? "No description provided." : internship.getDescription();
        JTextArea description = new JTextArea(text);
        description.setEditable(false);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setCaretPosition(0);
        JScrollPane descriptionScroll = new JScrollPane(description);
        descriptionScroll.setBorder(BorderFactory.createTitledBorder("About this internship"));
        descriptionScroll.setPreferredSize(new Dimension(420, 160));

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());

        JButton applyButton = new JButton(listing.isApplied() ? "Already applied" : "Apply");
        if (listing.isApplied()) {
            applyButton.setEnabled(false);
        } else {
            applyButton.setBackground(PRIMARY_COLOR);
            applyButton.setForeground(Color.WHITE);
            applyButton.addActionListener(e -> {
                applyRequested = true;
                dispose();
            });
            getRootPane().setDefaultButton(applyButton);
        }

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(closeButton);
        buttons.add(applyButton);

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBorder(BorderFactory.createEmptyBorder(18, 24, 14, 24));
        root.add(top, BorderLayout.NORTH);
        root.add(descriptionScroll, BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);

        setContentPane(root);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    /** True if the student clicked Apply (check after setVisible(true) returns). */
    public boolean isApplyRequested() {
        return applyRequested;
    }

    private static void addFact(JPanel panel, String label, String value) {
        JLabel labelComponent = new JLabel(label);
        labelComponent.setForeground(Color.GRAY);
        panel.add(labelComponent);

        JLabel valueComponent = new JLabel(value);
        valueComponent.setFont(valueComponent.getFont().deriveFont(Font.BOLD));
        panel.add(valueComponent);
    }
}

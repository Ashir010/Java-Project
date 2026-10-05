package com.internportal.view.student;

import com.internportal.model.ApplicationStatus;
import com.internportal.model.MyApplication;
import com.internportal.model.StatusChange;
import com.internportal.service.MyApplicationService;
import com.internportal.view.common.StatusCellRenderer;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Window;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Pop-up timeline: every stage the application has been through, plus the stages still ahead. */
public class TrackDialog extends JDialog {

    private static final Color PRIMARY_COLOR = new Color(0x1E5AA8);
    private static final Color PENDING_COLOR = new Color(0xB0B0B0);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, h:mm a");

    public TrackDialog(Window owner, MyApplication application, List<StatusChange> steps) {
        super(owner, "Track Application", Dialog.ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        ApplicationStatus current = application.getStatus();

        JLabel title = new JLabel(application.getInternshipTitle());
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setForeground(PRIMARY_COLOR);

        JLabel company = new JLabel(application.getCompanyName());
        company.setForeground(Color.GRAY);

        JLabel currentLabel = new JLabel("Current status: " + displayName(current));
        currentLabel.setFont(currentLabel.getFont().deriveFont(Font.BOLD));
        currentLabel.setForeground(colorOf(current));

        JPanel header = new JPanel(new GridLayout(0, 1, 0, 3));
        header.add(title);
        header.add(company);
        header.add(currentLabel);

        // Timeline: finished steps first (filled dots), then upcoming steps (hollow dots)
        JPanel timeline = new JPanel(new GridBagLayout());
        timeline.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Progress"),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        int row = 0;
        for (int i = 0; i < steps.size(); i++) {
            StatusChange step = steps.get(i);
            String when = step.getChangedOn() == null ? "" : step.getChangedOn().format(DATE_TIME);
            addStep(timeline, row++, new DotIcon(colorOf(step.getStatus()), true),
                    displayName(step.getStatus()), when, i == steps.size() - 1, false);
        }
        for (ApplicationStatus upcoming : MyApplicationService.upcomingSteps(current)) {
            addStep(timeline, row++, new DotIcon(PENDING_COLOR, false),
                    displayName(upcoming), "Pending", false, true);
        }
        Dimension preferred = timeline.getPreferredSize();
        timeline.setPreferredSize(new Dimension(Math.max(preferred.width, 400), preferred.height));

        JLabel message = new JLabel("<html><div style='width:360px'>" + messageFor(current) + "</div></html>");
        message.setForeground(Color.DARK_GRAY);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttons.add(closeButton);

        JPanel south = new JPanel(new BorderLayout(0, 10));
        south.add(message, BorderLayout.NORTH);
        south.add(buttons, BorderLayout.SOUTH);

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBorder(BorderFactory.createEmptyBorder(18, 24, 14, 24));
        root.add(header, BorderLayout.NORTH);
        root.add(timeline, BorderLayout.CENTER);
        root.add(south, BorderLayout.SOUTH);

        setContentPane(root);
        getRootPane().setDefaultButton(closeButton);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private static void addStep(JPanel panel, int row, Icon icon, String name, String when,
                                boolean bold, boolean pending) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0;
        c.insets = new Insets(6, 0, 6, 12);
        panel.add(new JLabel(icon), c);

        JLabel nameLabel = new JLabel(name);
        if (bold) {
            nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));
        }
        if (pending) {
            nameLabel.setForeground(PENDING_COLOR);
        }
        c.gridx = 1;
        c.insets = new Insets(6, 0, 6, 24);
        panel.add(nameLabel, c);

        JLabel whenLabel = new JLabel(when);
        whenLabel.setForeground(pending ? PENDING_COLOR : Color.GRAY);
        c.gridx = 2;
        c.weightx = 1;
        c.insets = new Insets(6, 0, 6, 0);
        panel.add(whenLabel, c);
    }

    private static Color colorOf(ApplicationStatus status) {
        Color color = StatusCellRenderer.colorFor(status.name());
        return color == null ? Color.GRAY : color;
    }

    private static String displayName(ApplicationStatus status) {
        String text = status.name();
        return text.charAt(0) + text.substring(1).toLowerCase();
    }

    private static String messageFor(ApplicationStatus status) {
        switch (status) {
            case APPLIED:
                return "Your application has been received and is waiting for review.";
            case SHORTLISTED:
                return "You have been shortlisted. The final decision is still pending.";
            case SELECTED:
                return "Congratulations, you have been selected for this internship!";
            case REJECTED:
                return "This application was not successful. Keep applying, new internships are posted regularly.";
            case COMPLETED:
                return "You have completed this internship. Well done!";
            default:
                return "";
        }
    }

    /** A small filled or hollow circle, drawn directly so it looks the same on every system. */
    private static final class DotIcon implements Icon {
        private static final int SIZE = 16;

        private final Color color;
        private final boolean filled;

        DotIcon(Color color, boolean filled) {
            this.color = color;
            this.filled = filled;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            if (filled) {
                g2.fillOval(x + 1, y + 1, SIZE - 2, SIZE - 2);
            } else {
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(x + 2, y + 2, SIZE - 4, SIZE - 4);
            }
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return SIZE;
        }

        @Override
        public int getIconHeight() {
            return SIZE;
        }
    }
}

package com.internportal.view.common;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;

/** Temporary screen shown for a sidebar item until its real panel is built. */
public class PlaceholderPanel extends JPanel {

    public PlaceholderPanel(String title) {
        super(new GridBagLayout());
        setBackground(new Color(0xF2F5F9));

        JPanel card = new JPanel(new GridLayout(0, 1, 0, 6));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createEmptyBorder(28, 48, 28, 48));
        card.putClientProperty("FlatLaf.style", "arc: 16");

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 22f));
        titleLabel.setForeground(new Color(0x1E5AA8));

        JLabel infoLabel = new JLabel("This screen will be built in a later step.", SwingConstants.CENTER);
        infoLabel.setForeground(Color.GRAY);

        card.add(titleLabel);
        card.add(infoLabel);
        add(card);
    }
}

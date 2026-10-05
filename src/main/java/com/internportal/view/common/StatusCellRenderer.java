package com.internportal.view.common;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

/** Colours a status column: green for good states, orange for in-progress, red or gray for ended. */
public class StatusCellRenderer extends DefaultTableCellRenderer {

    private static final Color GREEN = new Color(0x2E7D32);
    private static final Color ORANGE = new Color(0xEF6C00);
    private static final Color RED = new Color(0xC62828);
    private static final Color GRAY = new Color(0x757575);
    private static final Color BLUE = new Color(0x1E5AA8);

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        setHorizontalAlignment(SwingConstants.CENTER);
        setFont(getFont().deriveFont(Font.BOLD));

        Color color = colorFor(String.valueOf(value));
        if (!isSelected && color != null) {
            setForeground(color);
        }
        return this;
    }

    public static Color colorFor(String status) {
        switch (status) {
            case "OPEN":
            case "SELECTED":
            case "COMPLETED":
                return GREEN;
            case "SHORTLISTED":
                return ORANGE;
            case "REJECTED":
                return RED;
            case "CLOSED":
                return GRAY;
            case "APPLIED":
                return BLUE;
            default:
                return null;
        }
    }
}

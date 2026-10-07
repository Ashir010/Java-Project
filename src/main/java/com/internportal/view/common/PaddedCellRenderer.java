package com.internportal.view.common;

import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Component;

/** Plain cell renderer with space on both sides, so neighbouring columns never touch. */
public class PaddedCellRenderer extends DefaultTableCellRenderer {

    private static final int PADDING = 12;

    public PaddedCellRenderer(int horizontalAlignment) {
        setHorizontalAlignment(horizontalAlignment);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        setBorder(BorderFactory.createEmptyBorder(0, PADDING, 0, PADDING));
        return this;
    }
}

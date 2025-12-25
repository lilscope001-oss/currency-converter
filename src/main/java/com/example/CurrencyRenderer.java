package com.example;

import javax.swing.*;
import java.awt.*;

public class CurrencyRenderer extends DefaultListCellRenderer {

	private static final long serialVersionUID = 1L;

	@Override
    public Component getListCellRendererComponent(
            JList<?> list, Object value, int index,
            boolean isSelected, boolean cellHasFocus) {

        JLabel label = (JLabel) super.getListCellRendererComponent(
                list, value, index, isSelected, cellHasFocus);

        if (value instanceof CurrencyItem) {
            CurrencyItem item = (CurrencyItem) value;
            label.setText(item.toString());
            label.setIcon(item.flag);
            // Ensure icon is shown to the left of the text with some gap
            label.setHorizontalTextPosition(SwingConstants.RIGHT);
            label.setIconTextGap(8);
        }
        return label;
    }
}

package com.example;

import javax.swing.*;
import java.awt.*;

public class CurrencyRenderer extends DefaultListCellRenderer {

	private static final long serialVersionUID = 1L;

	@Override
	public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
			boolean cellHasFocus) {

		JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

		if (value instanceof CurrencyItem) {
			CurrencyItem item = (CurrencyItem) value;

			// Format text with currency code highlighted
			String displayText = item.country;
			if (!displayText.equals(item.code)) {
				displayText += " (" + item.code + ")";
			} else {
				displayText = item.code;
			}

			label.setText(displayText);
			label.setIcon(item.flag);
			label.setHorizontalTextPosition(SwingConstants.RIGHT);
			label.setIconTextGap(10);
			label.setFont(new Font("Segoe UI", Font.PLAIN, 14));

			// Better styling for selected items
			if (isSelected) {
				label.setBackground(new Color(76, 175, 80, 100));
				label.setForeground(Color.WHITE);
			}
		}
		return label;
	}
}

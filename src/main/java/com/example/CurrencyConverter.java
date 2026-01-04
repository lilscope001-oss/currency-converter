package com.example;

import com.formdev.flatlaf.FlatDarkLaf;
import org.json.JSONObject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

/**
 * Currency Converter using ExchangeRate-API (open.er-api.com) Base currency:
 * USD (default) Requires Java 21+
 */

public class CurrencyConverter extends JFrame {

   private static final long serialVersionUID = 1L;

   // UI Constants
   private static final Color PRIMARY_COLOR = new Color(76, 175, 80);
   private static final Color DARK_BG = new Color(30, 30, 30);
   private static final Color PANEL_BG = new Color(40, 40, 40);
   private static final Color INPUT_BG = new Color(50, 50, 50);
   private static final Color SUCCESS_COLOR = new Color(76, 175, 80);
   private static final Color WARNING_COLOR = new Color(255, 150, 100);
   private static final Color TEXT_LIGHT = new Color(220, 220, 220);

   // Size Constants
   private static final int MIN_CONTENT_WIDTH = 400;
   private static final int MAX_CONTENT_WIDTH = 500;
   private static final int CONTENT_HEIGHT = 100;

   private JComboBox<CurrencyItem> fromBox;
   private JComboBox<CurrencyItem> toBox;
   private JTextField amountField;
   private JLabel resultLabel;
   private JButton convertButton;
   private JLabel loadingLabel;
   private final HttpClient httpClient = HttpClient.newHttpClient();

   // Currency → Rate (relative to USD)
   private final Map<String, Double> ratesMap = new TreeMap<>();

   public CurrencyConverter() {
      super("Currency Converter");
      setupLookAndFeel();
      initUI();
      fetchRates();
   }

   private void setupLookAndFeel() {
      try {
         UIManager.setLookAndFeel(new FlatDarkLaf());
      } catch (UnsupportedLookAndFeelException e) {
         System.err.println("Could not set FlatDarkLaf: " + e.getMessage());
      }
   }

   private void initUI() {
      setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      setSize(650, 750);
      setMinimumSize(new Dimension(550, 500));
      setLocationRelativeTo(null);
      setResizable(true);

      // Main container panel
      JPanel mainPanel = new JPanel();
      mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
      mainPanel.setBackground(DARK_BG);
      mainPanel.setBorder(new EmptyBorder(30, 30, 30, 30));
      mainPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

      // Title
      JLabel titleLabel = createTitleLabel("Currency Converter");
      titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
      mainPanel.add(titleLabel);
      mainPanel.add(Box.createVerticalStrut(30));

      // From currency section
      JPanel fromPanel = createCurrencyPanel("From", true);
      fromPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
      mainPanel.add(fromPanel);
      mainPanel.add(Box.createVerticalStrut(20));

      // Swap button
      JButton swapButton = createSwapButton();
      swapButton.setAlignmentX(Component.CENTER_ALIGNMENT);
      mainPanel.add(swapButton);
      mainPanel.add(Box.createVerticalStrut(20));

      // To currency section
      JPanel toPanel = createCurrencyPanel("To", false);
      toPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
      mainPanel.add(toPanel);
      mainPanel.add(Box.createVerticalStrut(30));

      // Amount and Convert section
      JPanel amountPanel = new JPanel();
      amountPanel.setLayout(new BoxLayout(amountPanel, BoxLayout.Y_AXIS));
      amountPanel.setBackground(PANEL_BG);
      amountPanel.setBorder(new RoundedBorder(12, new Color(60, 60, 60), 1, new Insets(20, 20, 20, 20)));
      amountPanel.setMaximumSize(new Dimension(MAX_CONTENT_WIDTH, 120));
      amountPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

      JLabel amountLabel = createSectionLabel("Amount");
      amountLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
      amountPanel.add(amountLabel);
      amountPanel.add(Box.createVerticalStrut(8));

      amountField = new JTextField("1");
      amountField.setFont(new Font("Segoe UI", Font.PLAIN, 18));
      amountField.setMaximumSize(new Dimension(MAX_CONTENT_WIDTH - 40, 45));
      amountField.setBackground(INPUT_BG);
      amountField.setForeground(Color.WHITE);
      amountField.setCaretColor(PRIMARY_COLOR);
      amountField.setBorder(new RoundedBorder(8, new Color(60, 60, 60), 1, new Insets(10, 15, 10, 15)));
      amountPanel.add(amountField);
      mainPanel.add(amountPanel);
      mainPanel.add(Box.createVerticalStrut(25));

      // Convert button
      convertButton = createStyledButton("Convert", PRIMARY_COLOR);
      convertButton.setAlignmentX(Component.CENTER_ALIGNMENT);
      convertButton.addActionListener(e -> onConvert());
      mainPanel.add(convertButton);
      mainPanel.add(Box.createVerticalStrut(25));

      // Result display
      JPanel resultPanel = new JPanel();
      resultPanel.setLayout(new BoxLayout(resultPanel, BoxLayout.Y_AXIS));
      resultPanel.setBackground(new Color(45, 45, 45));
      resultPanel.setBorder(new RoundedBorder(12, new Color(60, 60, 60), 1, new Insets(20, 20, 20, 20)));
      resultPanel.setMaximumSize(new Dimension(MAX_CONTENT_WIDTH, 100));
      resultPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

      resultLabel = new JLabel("Result: —");
      resultLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
      resultLabel.setForeground(TEXT_LIGHT);
      resultLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
      resultPanel.add(resultLabel);

      loadingLabel = new JLabel("Loading exchange rates...");
      loadingLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
      loadingLabel.setForeground(new Color(150, 150, 150));
      loadingLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
      resultPanel.add(Box.createVerticalStrut(5));
      resultPanel.add(loadingLabel);

      mainPanel.add(resultPanel);
      mainPanel.add(Box.createVerticalStrut(15));

      // Footer
      JLabel footerLabel = new JLabel("Live rates from open.er-api.com");
      footerLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
      footerLabel.setForeground(new Color(120, 120, 120));
      footerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
      mainPanel.add(footerLabel);

      // Add glue to push content to top
      mainPanel.add(Box.createVerticalGlue());

      // Scroll pane - wrap mainPanel directly
      JScrollPane scrollPane = new JScrollPane(mainPanel);
      scrollPane.setBackground(DARK_BG);
      scrollPane.setBorder(null);
      scrollPane.getVerticalScrollBar().setUnitIncrement(10);
      scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
      scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
      add(scrollPane);
   }

   private JLabel createTitleLabel(String text) {
      JLabel label = new JLabel(text);
      label.setFont(new Font("Segoe UI", Font.BOLD, 32));
      label.setForeground(Color.WHITE);
      return label;
   }

   private JLabel createSectionLabel(String text) {
      JLabel label = new JLabel(text);
      label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
      label.setForeground(new Color(180, 180, 180));
      return label;
   }

   private JPanel createCurrencyPanel(String labelText, boolean isFrom) {
      JPanel panel = new JPanel();
      panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
      panel.setBackground(PANEL_BG);
      panel.setBorder(new RoundedBorder(12, new Color(60, 60, 60), 1, new Insets(20, 20, 20, 20)));
      panel.setPreferredSize(new Dimension(MIN_CONTENT_WIDTH, CONTENT_HEIGHT));
      panel.setMinimumSize(new Dimension(MIN_CONTENT_WIDTH, CONTENT_HEIGHT));
      panel.setMaximumSize(new Dimension(MAX_CONTENT_WIDTH, CONTENT_HEIGHT));
      panel.setAlignmentX(Component.CENTER_ALIGNMENT);

      JLabel label = createSectionLabel(labelText);
      label.setAlignmentX(Component.LEFT_ALIGNMENT);
      panel.add(label);
      panel.add(Box.createVerticalStrut(8));

      JComboBox<CurrencyItem> comboBox = new JComboBox<>();
      comboBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
      comboBox.setPreferredSize(new Dimension(MIN_CONTENT_WIDTH - 40, 45));
      comboBox.setMinimumSize(new Dimension(MIN_CONTENT_WIDTH - 40, 45));
      comboBox.setMaximumSize(new Dimension(MAX_CONTENT_WIDTH - 40, 45));
      comboBox.setRenderer(new CurrencyRenderer());
      comboBox.setBackground(INPUT_BG);
      comboBox.setForeground(Color.WHITE);
      comboBox.setBorder(new RoundedBorder(8, new Color(60, 60, 60), 1, new Insets(8, 12, 8, 12)));
      comboBox.setAlignmentX(Component.LEFT_ALIGNMENT);
      panel.add(comboBox);

      if (isFrom) {
         fromBox = comboBox;
      } else {
         toBox = comboBox;
      }

      return panel;
   }

   private JButton createSwapButton() {
      JButton button = new JButton("SWAP");
      button.setFont(new Font("Segoe UI", Font.BOLD, 12));
      button.setPreferredSize(new Dimension(120, 38));
      button.setMinimumSize(new Dimension(120, 38));
      button.setMaximumSize(new Dimension(120, 38));
      button.setBackground(new Color(60, 60, 60));
      button.setForeground(Color.WHITE);
      button.setBorder(new RoundedBorder(8, new Color(80, 80, 80), 1, new Insets(8, 20, 8, 20)));
      button.setFocusPainted(false);
      button.setCursor(new Cursor(Cursor.HAND_CURSOR));

      button.addMouseListener(new java.awt.event.MouseAdapter() {
         public void mouseEntered(java.awt.event.MouseEvent e) {
            button.setBackground(new Color(70, 70, 70));
            button.setBorder(new RoundedBorder(8, PRIMARY_COLOR, 1, new Insets(8, 20, 8, 20)));
         }

         public void mouseExited(java.awt.event.MouseEvent e) {
            button.setBackground(new Color(60, 60, 60));
            button.setBorder(new RoundedBorder(8, new Color(80, 80, 80), 1, new Insets(8, 20, 8, 20)));
         }
      });

      button.addActionListener(e -> swapCurrencies());
      return button;
   }

   private JButton createStyledButton(String text, Color color) {
      JButton button = new JButton(text);
      button.setFont(new Font("Segoe UI", Font.BOLD, 15));
      button.setPreferredSize(new Dimension(200, 50));
      button.setMinimumSize(new Dimension(200, 50));
      button.setMaximumSize(new Dimension(200, 50));
      button.setBackground(color);
      button.setForeground(Color.WHITE);
      button.setBorder(new RoundedBorder(10, color, 0, new Insets(12, 30, 12, 30)));
      button.setFocusPainted(false);
      button.setCursor(new Cursor(Cursor.HAND_CURSOR));

      button.addMouseListener(new java.awt.event.MouseAdapter() {
         public void mouseEntered(java.awt.event.MouseEvent e) {
            button.setBackground(color.brighter());
         }

         public void mouseExited(java.awt.event.MouseEvent e) {
            button.setBackground(color);
         }

         public void mousePressed(java.awt.event.MouseEvent e) {
            button.setBackground(color.darker());
         }

         public void mouseReleased(java.awt.event.MouseEvent e) {
            button.setBackground(color);
         }
      });

      return button;
   }

   private void swapCurrencies() {
      if (fromBox.getSelectedItem() != null && toBox.getSelectedItem() != null) {
         CurrencyItem from = (CurrencyItem) fromBox.getSelectedItem();
         CurrencyItem to = (CurrencyItem) toBox.getSelectedItem();

         fromBox.setSelectedItem(to);
         toBox.setSelectedItem(from);

         // Auto-convert after swap if amount is valid
         if (!amountField.getText().trim().isEmpty()) {
            onConvert();
         }
      }
   }

   private void fetchRates() {
      new Thread(() -> {
         try {
            String url = "https://open.er-api.com/v6/latest/USD";

            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
               throw new RuntimeException("Failed to fetch exchange rates");
            }

            JSONObject json = new JSONObject(response.body());

            if (!"success".equalsIgnoreCase(json.getString("result"))) {
               throw new RuntimeException("API returned error");
            }

            JSONObject rates = json.getJSONObject("rates");

            for (String key : rates.keySet()) {
               ratesMap.put(key, rates.getDouble(key));
            }

            SwingUtilities.invokeLater(() -> {
               DefaultComboBoxModel<CurrencyItem> fromModel = new DefaultComboBoxModel<>();
               DefaultComboBoxModel<CurrencyItem> toModel = new DefaultComboBoxModel<>();

               for (String currency : ratesMap.keySet()) {
                  String code = currency;
                  String displayName = code;
                  try {
                     java.util.Currency cur = java.util.Currency.getInstance(code);
                     displayName = cur.getDisplayName(Locale.ENGLISH);
                  } catch (Exception ignored) {
                  }

                  ImageIcon flag = null;
                  try {
                     String path = "/flags/" + code.toLowerCase(Locale.ROOT) + ".png";
                     java.net.URL res = getClass().getResource(path);
                     if (res != null) {
                        ImageIcon raw = new ImageIcon(res);
                        Image scaled = raw.getImage().getScaledInstance(20, 14, Image.SCALE_SMOOTH);
                        flag = new ImageIcon(scaled);
                     }
                  } catch (Exception ignored) {
                  }

                  CurrencyItem item = new CurrencyItem(code, displayName, flag);
                  fromModel.addElement(item);
                  toModel.addElement(item);
               }

               fromBox.setModel(fromModel);
               toBox.setModel(toModel);

               // Default: select USD for From and NGN for To (if available)
               for (int i = 0; i < fromModel.getSize(); i++) {
                  if ("USD".equals(fromModel.getElementAt(i).code)) {
                     fromBox.setSelectedIndex(i);
                     break;
                  }
               }

               boolean ngnFound = false;
               for (int i = 0; i < toModel.getSize(); i++) {
                  if ("NGN".equals(toModel.getElementAt(i).code)) {
                     toBox.setSelectedIndex(i);
                     ngnFound = true;
                     break;
                  }
               }

               if (!ngnFound) {
                  for (int i = 0; i < toModel.getSize(); i++) {
                     if ("USD".equals(toModel.getElementAt(i).code)) {
                        toBox.setSelectedIndex(i);
                        break;
                     }
                  }
               }

               // Hide loading label
               loadingLabel.setText("");
               loadingLabel.setVisible(false);
               resultLabel.setText("Result: —");
               resultLabel.setForeground(TEXT_LIGHT);
            });
         } catch (Exception e) {
            e.printStackTrace();
            SwingUtilities.invokeLater(() -> {
               loadingLabel.setText("Failed to load exchange rates");
               loadingLabel.setForeground(new Color(255, 100, 100));
               JOptionPane.showMessageDialog(this, "Failed to load exchange rates.\n" + e.getMessage(), "Error",
                     JOptionPane.ERROR_MESSAGE);
            });
         }
      }).start();
   }

   private void onConvert() {
      try {
         if (ratesMap.isEmpty()) {
            resultLabel.setText("Loading rates, please wait...");
            resultLabel.setForeground(WARNING_COLOR);
            return;
         }

         CurrencyItem fromItem = (CurrencyItem) fromBox.getSelectedItem();
         if (fromItem == null) {
            resultLabel.setText("Please select a currency");
            resultLabel.setForeground(WARNING_COLOR);
            return;
         }
         String from = fromItem.code;

         CurrencyItem toItem = (CurrencyItem) toBox.getSelectedItem();
         if (toItem == null) {
            resultLabel.setText("Please select a currency");
            resultLabel.setForeground(WARNING_COLOR);
            return;
         }
         String to = toItem.code;

         String amountText = amountField.getText().trim();
         if (amountText.isEmpty()) {
            resultLabel.setText("Please enter an amount");
            resultLabel.setForeground(WARNING_COLOR);
            return;
         }

         double amount = Double.parseDouble(amountText);

         if (amount < 0) {
            resultLabel.setText("Amount cannot be negative");
            resultLabel.setForeground(WARNING_COLOR);
            return;
         }

         double fromRate = ratesMap.get(from);
         double toRate = ratesMap.get(to);

         double converted = amount * (toRate / fromRate);

         // Format large numbers with commas
         String formattedAmount = formatNumber(amount);
         String formattedConverted = formatNumber(converted);

         resultLabel.setText(String.format(Locale.US, "%s %s = %s %s", formattedAmount, from, formattedConverted, to));
         resultLabel.setForeground(SUCCESS_COLOR);
      } catch (NumberFormatException e) {
         resultLabel.setText("Invalid amount format");
         resultLabel.setForeground(new Color(255, 100, 100));
      } catch (NullPointerException e) {
         resultLabel.setText("Please wait for rates to load");
         resultLabel.setForeground(WARNING_COLOR);
      } catch (Exception e) {
         resultLabel.setText("Conversion error occurred");
         resultLabel.setForeground(new Color(255, 100, 100));
      }
   }

   private String formatNumber(double number) {
      if (number >= 1000) {
         return String.format(Locale.US, "%,.2f", number);
      } else {
         return String.format(Locale.US, "%.2f", number);
      }
   }

   public static void main(String[] args) {
      SwingUtilities.invokeLater(() -> new CurrencyConverter().setVisible(true));
   }

   // Custom rounded border class with padding support
   static class RoundedBorder extends javax.swing.border.AbstractBorder {
      private int radius;
      private Color color;
      private int thickness;
      private Insets padding;

      public RoundedBorder(int radius, Color color, int thickness, Insets padding) {
         this.radius = radius;
         this.color = color;
         this.thickness = thickness;
         this.padding = padding != null ? padding : new Insets(0, 0, 0, 0);
      }

      public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
         Graphics2D g2d = (Graphics2D) g.create();
         g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

         if (thickness > 0) {
            g2d.setColor(color);
            g2d.setStroke(new BasicStroke(thickness));
            g2d.drawRoundRect(x + thickness / 2, y + thickness / 2, width - thickness - 1, height - thickness - 1,
                  radius, radius);
         }
         g2d.dispose();
      }

      public Insets getBorderInsets(Component c) {
         int top = thickness + padding.top;
         int left = thickness + padding.left;
         int bottom = thickness + padding.bottom;
         int right = thickness + padding.right;
         return new Insets(top, left, bottom, right);
      }
   }
}

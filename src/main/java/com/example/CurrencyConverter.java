package com.example;



import org.json.JSONObject;



import javax.swing.*;

import java.awt.*;

import java.net.URI;

import java.net.http.HttpClient;

import java.net.http.HttpRequest;

import java.net.http.HttpResponse;

import java.util.*;


 
 /**
 
 * Currency Converter using ExchangeRate-API (open.er-api.com)

 * Base currency: USD (default)

 * Requires Java 11+

 */

public class CurrencyConverter extends JFrame {



    private JComboBox<CurrencyItem> fromBox;

    private JComboBox<CurrencyItem> toBox;

    private JTextField amountField;

    private JLabel resultLabel;

    private JButton convertButton;



    private final HttpClient httpClient = HttpClient.newHttpClient();



    // Currency → Rate (relative to USD)

    private final Map<String, Double> ratesMap = new TreeMap<>();



    public CurrencyConverter() {

        super("Live Currency Converter (USD Base)");

        initUI();

        fetchRates();

    }



    private void initUI() {

        setLayout(new BorderLayout(8, 8));



        JPanel panel = new JPanel(new GridLayout(4, 2, 6, 6));



        panel.add(new JLabel("From (currency):"));
        fromBox = new JComboBox<>();
        panel.add(fromBox);

        panel.add(new JLabel("To (currency):"));
        toBox = new JComboBox<>();
        panel.add(toBox);

        // Renderer for flags + names
        fromBox.setRenderer(new CurrencyRenderer());
        toBox.setRenderer(new CurrencyRenderer());


        panel.add(new JLabel("Amount:"));

        amountField = new JTextField("1");

        panel.add(amountField);



        convertButton = new JButton("Convert");

        resultLabel = new JLabel("Result: —");



        panel.add(convertButton);

        panel.add(resultLabel);



        add(panel, BorderLayout.CENTER);



        JTextArea hint = new JTextArea(

                "Live exchange rates provided by open.er-api.com\nDefault base currency: USD"

        );

        hint.setEditable(false);

        hint.setBackground(getBackground());

        add(hint, BorderLayout.SOUTH);



        convertButton.addActionListener(e -> onConvert());



        setSize(500, 230);

        setLocationRelativeTo(null);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    }



    private void fetchRates() {

        new Thread(() -> {

            try {

                String url = "https://open.er-api.com/v6/latest/USD";



                HttpRequest request = HttpRequest.newBuilder()

                        .uri(URI.create(url))

                        .GET()

                        .build();



                HttpResponse<String> response =

                        httpClient.send(request, HttpResponse.BodyHandlers.ofString());



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

                });



            } catch (Exception e) {

                e.printStackTrace();

                SwingUtilities.invokeLater(() ->

                        JOptionPane.showMessageDialog(

                                this,

                                "Failed to load exchange rates.\n" + e.getMessage(),

                                "Error",

                                JOptionPane.ERROR_MESSAGE

                        )

                );

            }

        }).start();

    }



    private void onConvert() {

        try {

            CurrencyItem fromItem = (CurrencyItem) fromBox.getSelectedItem();
            String from = fromItem.code;

            CurrencyItem toItem = (CurrencyItem) toBox.getSelectedItem();
            String to = toItem.code;

            double amount = Double.parseDouble(amountField.getText());



            double fromRate = ratesMap.get(from);

            double toRate = ratesMap.get(to);



            double converted = amount * (toRate / fromRate);



            resultLabel.setText(String.format(

                    Locale.US,

                    "Result: %.2f %s = %.2f %s",

                    amount, from, converted, to

            ));



        } catch (Exception e) {

            JOptionPane.showMessageDialog(

                    this,

                    "Invalid input or rates not loaded.",

                    "Error",

                    JOptionPane.ERROR_MESSAGE

            );

        }

    }



    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            new CurrencyConverter().setVisible(true);

        });

    }

}

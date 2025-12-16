package com.example;

import javax.swing.*;

public class CurrencyItem {
    public String code;
    public String country;
    public ImageIcon flag;

    public CurrencyItem(String code, String country, ImageIcon flag) {
        this.code = code;
        this.country = country;
        this.flag = flag;
    }

    @Override
    public String toString() {
        return country + " (" + code + ")";
    }
}
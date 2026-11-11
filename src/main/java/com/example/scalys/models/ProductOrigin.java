package com.example.scalys.models;

public enum ProductOrigin {
    CAPTIVE_BRED("Nuôi sinh sản"), IMPORTED("Nhập khẩu");

    private final String label;

    ProductOrigin(String label) { this.label = label; }
    public String getLabel() { return label; }
}

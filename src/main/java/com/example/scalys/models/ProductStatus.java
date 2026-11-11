package com.example.scalys.models;

public enum ProductStatus {
    AVAILABLE("Còn hàng"),
    HELD("Đang giữ"),
    SOLD("Đã bán");

    private final String label;

    ProductStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

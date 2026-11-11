package com.example.scalys.models;

public enum ProductGender {
    MALE("Đực"), FEMALE("Cái"), UNKNOWN("Chưa xác định");

    private final String label;

    ProductGender(String label) { this.label = label; }
    public String getLabel() { return label; }
}

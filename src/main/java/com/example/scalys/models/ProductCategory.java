package com.example.scalys.models;

public enum ProductCategory {
    LIGHTING("Đèn & sưởi"),
    ENCLOSURE("Tank & chuồng nuôi"),
    FOOD("Thức ăn"),
    SUPPLEMENT("Vitamin & khoáng"),
    DECOR("Trang trí tank"),
    CARE("Dụng cụ chăm sóc"),
    PET_CARE("Chăm sóc pet"),
    HABITAT_SETUP("Thiết lập môi trường");

    private final String label;

    ProductCategory(String label) { this.label = label; }

    public String getLabel() { return label; }

    public boolean supports(ProductKind kind) {
        return switch (kind) {
            case PET -> false;
            case SUPPLY -> this == LIGHTING || this == ENCLOSURE || this == FOOD
                    || this == SUPPLEMENT || this == DECOR || this == CARE;
            case SERVICE -> this == PET_CARE || this == HABITAT_SETUP;
        };
    }
}

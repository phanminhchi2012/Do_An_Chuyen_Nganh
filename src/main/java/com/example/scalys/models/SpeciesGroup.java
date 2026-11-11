package com.example.scalys.models;

public enum SpeciesGroup {
    SNAKE("Rắn"), PYTHON("Trăn"), TURTLE("Rùa"), DRAGON("Rồng"),
    FROG("Ếch"), SPIDER("Nhện"), MONITOR_LIZARD("Kỳ đà"),
    LIZARD("Thằn lằn"), GECKO("Tắc kè"), OTHER("Khác");

    private final String label;

    SpeciesGroup(String label) { this.label = label; }
    public String getLabel() { return label; }
}

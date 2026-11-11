package com.example.scalys.models;

public enum CareDifficulty {
    EASY("Dễ"), MEDIUM("Trung bình"), HARD("Khó");

    private final String label;

    CareDifficulty(String label) { this.label = label; }
    public String getLabel() { return label; }
}

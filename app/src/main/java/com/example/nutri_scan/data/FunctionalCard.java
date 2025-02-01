package com.example.nutri_scan.data;

public class FunctionalCard {
    private int backgroundColor;
    private String title;
    private String description;
    private int icon;
    private CardType type;

    public enum CardType {
        BMI_CALCULATOR,
        CALORIE_CALCULATOR,
        WATER_INTAKE,
        MACRO_CALCULATOR
    }

    public FunctionalCard(int backgroundColor, String title, String description, int icon, CardType type) {
        this.backgroundColor = backgroundColor;
        this.title = title;
        this.description = description;
        this.icon = icon;
        this.type = type;
    }

    // Getters
    public int getBackgroundColor() { return backgroundColor; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getIcon() { return icon; }
    public CardType getType() { return type; }
}

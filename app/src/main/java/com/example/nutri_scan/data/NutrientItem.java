package com.example.nutri_scan.data;

public class NutrientItem {
    private boolean isAdditives;
    private int icon;         // Represents the nutrient icon resource ID
    private String name;      // Name of the nutrient
    private String description; // Description of the nutrient
    private String value;     // Value of the nutrient
    private int color;        // Represents the color code

    public NutrientItem(int icon, String name, String description, String value, int color) {
        this(icon, name, description, value, color, false);
    }

    // Constructor
    public NutrientItem(int icon, String name, String description, String value, int color,  boolean isAdditives) {
        this.icon = icon;
        this.name = name;
        this.description = description;
        this.value = value;
        this.color = color;
        this.isAdditives = isAdditives;

    }

    // Getters
    public int getIcon() {
        return icon;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getValue() {
        return value;
    }

    public int getColor() {
        return color;
    }

    public boolean isAdditives() {
        return isAdditives;
    }



    // Setters
    public void setIcon(int icon) {
        this.icon = icon;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public void setAdditives(boolean additives) {
        isAdditives = additives;
    }


}

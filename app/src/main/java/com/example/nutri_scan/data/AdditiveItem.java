package com.example.nutri_scan.data;

public class AdditiveItem {
    private final String code;        // Enumber
    private final String name;
    private final String description;
    private final String usage;

    // Constructor
    public AdditiveItem(String code, String name, String description, String usage) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.usage = usage;
    }

    // Getters
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getUsage() { return usage; }
}

package com.example.nutri_scan.data;

public class DangerItem {
    private String enumber;
    private String name;
    private String hyperactivity;
    private String asthma;
    private String cancer;

    // Constructor to initialize all fields
    public DangerItem(String enumber, String name, String hyperactivity, String asthma, String cancer) {
        this.enumber = enumber;
        this.name = name;
        this.hyperactivity = hyperactivity;
        this.asthma = asthma;
        this.cancer = cancer;
    }

    // Getter for Enumber
    public String getEnumber() {
        return enumber;
    }

    // Getter for Name
    public String getName() {
        return name;
    }

    // Getter for Hyperactivity
    public String getHyperactivity() {
        return hyperactivity;
    }

    // Getter for Asthma
    public String getAsthma() {
        return asthma;
    }

    // Getter for Cancer
    public String getCancer() {
        return cancer;
    }

    // Override toString() to display all the information in a readable format
    @Override
    public String toString() {
        return "DangerItem{" +
                "Enumber='" + enumber + '\'' +
                ", Name='" + name + '\'' +
                ", Hyperactivity='" + hyperactivity + '\'' +
                ", Asthma='" + asthma + '\'' +
                ", Cancer='" + cancer + '\'' +
                '}';
    }
}

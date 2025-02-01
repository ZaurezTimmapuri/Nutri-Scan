package com.example.nutri_scan.data;

public class SliderItem {
    private int image;
    private String description;

    public SliderItem(int image, String description) {
        this.image = image;
        this.description = description;
    }

    public int getImage() {
        return image;
    }

    public String getDescription() {
        return description;
    }
}

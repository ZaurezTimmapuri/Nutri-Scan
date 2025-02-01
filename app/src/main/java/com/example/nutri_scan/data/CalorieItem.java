package com.example.nutri_scan.data;

public class CalorieItem {
    private String name;
    private int calories;
    private float fats;
    private float saturatedFats;
    private float carbohydrates;
    private float fiber;
    private float proteins;
    private float sodium;
    private float sugar;
    private int score;
    private String description;

    public CalorieItem(String name, float sugar, int i , int score, int calories, float saturatedFats, float carbohydrates, float fiber, float proteins , float sodium , String description) {
        this.name = name;
        this.calories = calories;
        this.fats = fats;
        this.saturatedFats = saturatedFats;
        this.carbohydrates = carbohydrates;
        this.fiber = fiber;
        this.proteins = proteins;
        this.sodium = sodium;
        this.sugar = sugar;
        this.score = score;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public int getCalories() {
        return calories;
    }

    public float getSaturatedFats() {
        return saturatedFats;
    }

    public float getCarbohydrates() {
        return carbohydrates;
    }

    public float getFiber() {
        return fiber;
    }

    public float getProteins() {
        return proteins;
    }

    public float getSodium() {return sodium;}

    public float getSugar() {return sugar;}

    public int getScore(){return score;}

    public String getDescription(){return description;}
}



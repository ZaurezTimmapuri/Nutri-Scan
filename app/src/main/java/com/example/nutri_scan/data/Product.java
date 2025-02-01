package com.example.nutri_scan.data;

import java.util.List;
import java.util.Map;

public class Product {
    private String barcode;
    private String imageUrl;
    private String Product_Name;
    private String Product_Brand;
    private String Product_Category;
    private double Score;
    private String Recommendation;
    private List<String> Ingredients;
    private List<String> Additives;
    private Map<String, String> Nutrition;
    private List<String> Keywords;

    public enum ProductDisplayType {
        NORMAL,
        SCANNED,
        ALTERNATIVE
    }

    // Required empty constructor for Firebase
    public Product() {
    }

    // Constructor
    public Product(String barcode, String imageUrl, String productName, String productBrand,
                   String productCategory, double score, String recommendation,
                   List<String> ingredients, List<String> additives,
                   Map<String, String> nutrition ,List<String> keywords) {
        this.barcode = barcode;
        this.imageUrl = imageUrl;
        this.Product_Name = productName;
        this.Product_Brand = productBrand;
        this.Product_Category = productCategory;
        this.Score = score;
        this.Recommendation = recommendation;
        this.Ingredients = ingredients;
        this.Additives = additives;
        this.Nutrition = nutrition;
        this.Keywords = keywords;
    }

    private ProductDisplayType displayType = ProductDisplayType.NORMAL;

    public ProductDisplayType getDisplayType() {
        return displayType;
    }

    public void setDisplayType(ProductDisplayType displayType) {
        this.displayType = displayType;
    }

    // Getters and Setters
    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getProduct_Name() {
        return Product_Name;
    }

    public void setProduct_Name(String product_Name) {
        this.Product_Name = product_Name;
    }

    public String getProduct_Brand() {
        return Product_Brand;
    }

    public void setProduct_Brand(String product_Brand) {this.Product_Brand = product_Brand;}

    public String getProduct_Category() {
        return Product_Category;
    }

    public void setProduct_Category(String product_Category) {this.Product_Category = product_Category;}

    public double getScore() {
        return Score;
    }

    public void setScore(double score) {
        this.Score = score;
    }

    public String getRecommendation() {
        return Recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.Recommendation = recommendation;
    }

    public List<String> getIngredients() {
        return Ingredients;
    }

    public void setIngredients(List<String> ingredients) {
        this.Ingredients = ingredients;
    }

    public List<String> getAdditives() {
        return Additives;
    }

    public void setAdditives(List<String> additives) {
        this.Additives = additives;
    }

    public Map<String, String> getNutrition() {
        return Nutrition;
    }

    public void setNutrition(Map<String, String> nutrition) {
        this.Nutrition = nutrition;
    }

    public List<String> getKeywords() {return Keywords;}

    public void setKeywords(List<String> keywords) {this.Keywords = keywords;}

}
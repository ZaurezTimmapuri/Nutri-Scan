package com.example.nutri_scan.data;

import java.util.HashMap;

public class TempDataHolder {
    private static final HashMap<String, Object> tempData = new HashMap<>();

    // Constants for keys
    public static final String KEY_BARCODE = "barcode_data";
    public static final String KEY_PRODUCT_INFO = "product_info";
    public static final String KEY_IMAGE_DATA = "image_data";
    public static final String KEY_NUTRITION = "nutrition_data";
    public static final String KEY_INGREDIENTS = "ingredients_data";

    public static void saveData(String key, Object value) {
        tempData.put(key, value);
    }

    public static Object getData(String key) {
        return tempData.get(key);
    }

    public static void removeData(String key) {
        tempData.remove(key);
    }

    public static void clearAll() {
        tempData.clear();
    }

    // Helper method for product info
    public static void saveProductInfo(String name, String brand, String Category) {
        HashMap<String, String> productInfo = new HashMap<>();
        productInfo.put("name", name);
        productInfo.put("brand", brand);
        productInfo.put("category", Category);
        saveData(KEY_PRODUCT_INFO, productInfo);
    }

    // Helper method for image data
    public static void saveImageData(String uri, String path) {
        HashMap<String, String> imageData = new HashMap<>();
        imageData.put("uri", uri);
        imageData.put("path", path);
        saveData(KEY_IMAGE_DATA, imageData);
    }
}

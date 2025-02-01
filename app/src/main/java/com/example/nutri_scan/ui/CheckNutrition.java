package com.example.nutri_scan.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.nutri_scan.R;
import com.example.nutri_scan.data.TempDataHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class CheckNutrition extends AppCompatActivity {
    private Map<String, EditText> nutritionEditTexts;
    private Map<String, String> nutritionData;
    private static final String DEFAULT_VALUE = "0.0";

    // Define constant keys for nutrition values
    private static final String KEY_TOTAL_FAT = "total-fat";
    private static final String KEY_SATURATED_FAT = "saturated-fat";
    private static final String KEY_TRANS_FAT = "trans-fat";
    private static final String KEY_SALT = "salt";
    private static final String KEY_CARBOHYDRATES = "carbohydrates";
    private static final String KEY_FIBER = "fiber";
    private static final String KEY_SUGARS = "sugars";
    private static final String KEY_ADDED_SUGARS = "added-sugars";
    private static final String KEY_PROTEINS = "proteins";
    private static final String KEY_ENERGY = "energy_value";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_check_nutrition);

//        TextView nutritionTextView = findViewById(R.id.nutrition_text_view);
//        Bundle nutritionBundle = getIntent().getBundleExtra(FetchNutrition.EXTRA_NUTRITION_DATA);

        // Set status bar color
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.status_bar_color_green));

//        if (nutritionBundle != null) {
//            // Convert bundle to sorted map for organized display
//            Map<String, String> sortedNutrition = new TreeMap<>();
//            for (String key : nutritionBundle.keySet()) {
//                sortedNutrition.put(key, nutritionBundle.getString(key));
//            }
//
//            // Build the display text
//            StringBuilder displayText = new StringBuilder("Nutrition Facts\n\n");
//            for (Map.Entry<String, String> entry : sortedNutrition.entrySet()) {
//                displayText.append(String.format("%s: %s\n",
//                        capitalizeFirstLetter(entry.getKey()),
//                        entry.getValue()));
//            }
//
//            nutritionTextView.setText(displayText.toString());
//        } else {
//            nutritionTextView.setText("No nutrition information available");
//        }

        initializeNutritionEditTexts();
        processNutritionData();

        // Set up next button
        Button nextButton = findViewById(R.id.next_ingredients);
        nextButton.setOnClickListener(v -> handleNextButton());
    }

    private void initializeNutritionEditTexts() {
        nutritionEditTexts = new HashMap<>();

        // Map TextView IDs to corresponding EditText fields
        Map<String, Integer> nutritionFieldMap = new HashMap<>();
        nutritionFieldMap.put(KEY_ENERGY, R.id.nutrition_energy_value);
        nutritionFieldMap.put(KEY_TOTAL_FAT, R.id.nutrition_fats_value);
        nutritionFieldMap.put(KEY_SATURATED_FAT, R.id.nutrition_saturates_value);
        nutritionFieldMap.put(KEY_TRANS_FAT, R.id.nutrition_trans_fat_value);
        nutritionFieldMap.put("cholesterol", R.id.nutrition_cholesterol_value);
        nutritionFieldMap.put("sodium", R.id.nutrition_sodium_value);
        nutritionFieldMap.put(KEY_SALT, R.id.nutrition_salt_value);
        nutritionFieldMap.put(KEY_CARBOHYDRATES, R.id.nutrition_carbs_value);
        nutritionFieldMap.put(KEY_FIBER, R.id.nutrition_fibre_value);
        nutritionFieldMap.put(KEY_SUGARS, R.id.nutrition_total_sugars_value);
        nutritionFieldMap.put(KEY_ADDED_SUGARS, R.id.nutrition_added_sugars_value);
        nutritionFieldMap.put(KEY_PROTEINS, R.id.nutrition_protein_value);

        // Initialize EditTexts with default value
        for (Map.Entry<String, Integer> entry : nutritionFieldMap.entrySet()) {
            EditText editText = findViewById(entry.getValue());
            editText.setText(DEFAULT_VALUE);
            nutritionEditTexts.put(entry.getKey(), editText);
        }
    }

    private void processNutritionData() {
        Bundle nutritionBundle = getIntent().getBundleExtra(FetchNutrition.EXTRA_NUTRITION_DATA);
        nutritionData = new TreeMap<>();

        if (nutritionBundle != null) {
            for (String key : nutritionBundle.keySet()) {
                String cleanedKey = standardizeNutrientKey(removeUnits(key.toLowerCase()));
                String originalValue = nutritionBundle.getString(key, DEFAULT_VALUE);
                String cleanedValue = removeUnits(originalValue);

                // If cleaned value is empty or null, use default value
                if (cleanedValue.isEmpty()) {
                    cleanedValue = DEFAULT_VALUE;
                }

                nutritionData.put(cleanedKey, cleanedValue);

                // Update EditText if it exists
                if (nutritionEditTexts.containsKey(cleanedKey)) {
                    nutritionEditTexts.get(cleanedKey).setText(cleanedValue);
                }
            }
            displayNutritionData();
        }
    }

    private String standardizeNutrientKey(String key) {
        // Standardize keys for different variations of the same nutrient
        switch (key.toLowerCase()) {
            case"energy":
                return  KEY_ENERGY;
            case "total fat":
            case "total fats":
                return KEY_TOTAL_FAT;
            case "saturated fat":
            case "saturated fats":
                return KEY_SATURATED_FAT;
            case "trans fat":
            case "trans fats":
                return KEY_TRANS_FAT;
            case "salt":
            case "salts":
                return KEY_SALT;
            case "carbohydrate":
            case "carbohydrates":
                return KEY_CARBOHYDRATES;
            case "dietary fiber":
            case "dietary fibre":
            case "dietary fibers":
                return KEY_FIBER;
            case "total sugar":
            case "total sugars":
                return KEY_SUGARS;
            case "added sugar":
            case "added sugars":
                return KEY_ADDED_SUGARS;
            case "protein":
            case "proteins":
                return KEY_PROTEINS;
            default:
                return key;
        }
    }

    private void displayNutritionData() {
        for (Map.Entry<String, String> entry : nutritionData.entrySet()) {
            String nutrientKey = entry.getKey().toLowerCase();
            EditText editText = nutritionEditTexts.get(nutrientKey);

            if (editText != null) {
                String value = entry.getValue();
                editText.setText(value.isEmpty() ? DEFAULT_VALUE : value);
            }
        }
    }

    private void handleNextButton() {
        Bundle updatedNutrition = new Bundle();

        // Iterate through all EditTexts and get their values
        for (Map.Entry<String, EditText> entry : nutritionEditTexts.entrySet()) {
            String nutrientName = entry.getKey();
            String value = entry.getValue().getText().toString().trim();

            // If value is empty or not a valid number, use default value
            if (value.isEmpty() || !isValidNumber(value)) {
                value = DEFAULT_VALUE;
            }

            updatedNutrition.putString(nutrientName, value);
        }

        ValidatingDialog validatingDialog = new ValidatingDialog(CheckNutrition.this);
        validatingDialog.start_validation_Dialog();



        new Handler().postDelayed(() -> {
            Intent intent = new Intent(this, Ingredients.class);
            saveNutritionData(updatedNutrition);
            startActivity(intent);
            validatingDialog.validation_dismiss_Dialog();
        }, 3000);
    }

    private boolean isValidNumber(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String capitalizeFirstLetter(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    private String removeUnits(String value) {
        if (value == null) return DEFAULT_VALUE;

        // Remove units like kcal, g, mg at the end of the string
        value = value.replaceAll("\\s*(kcal|mg|g)\\s*$", "").trim();

        // Remove units like kcal, g, mg within parentheses
        value = value.replaceAll("\\(.*?(kcal|mg|g).*?\\)", "").trim();

        return value.isEmpty() ? DEFAULT_VALUE : value;
    }

    private void saveNutritionData(Bundle nutritionData) {
        if (nutritionData != null) {
            TempDataHolder.saveData(TempDataHolder.KEY_NUTRITION, nutritionData);
            Intent intent = new Intent(this, OcrEnd.class);
            startActivity(intent);
        } else {
            Toast.makeText(this, "No nutrition data to save", Toast.LENGTH_SHORT).show();
        }
    }
}
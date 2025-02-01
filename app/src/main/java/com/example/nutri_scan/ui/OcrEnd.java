package com.example.nutri_scan.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.nutri_scan.R;
import com.example.nutri_scan.data.AdditiveDatabase;
import com.example.nutri_scan.data.TempDataHolder;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class OcrEnd extends AppCompatActivity {
    private static final String SCANNED_BARCODE = "scanned_barcode";
    private static final String EXTRA_IMAGE_URI = "image_uri";
    private static final String EXTRA_IMAGE_PATH = "image_path";
    private static final String EXTRA_INGREDIENTS_DATA = "ingredients_data";

    private static final String TAG = "OcrEnd";
    private TextView dataTextView;
    private ImageView imageView;
    private StringBuilder displayText;
    private Button dashboard_button;
    int Item_Score = 0;

        @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr_end);


            // Initialize views
            initializeViews();

            // Set up window appearance
            setupWindowAppearance();

            // Process data with validation
            if (!processDataWithValidation()) {
                // If data processing fails, show error and return to previous screen
                Toast.makeText(this, "Error: Required data not found", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            // Set up dashboard button
            setupDashboardButton();

    }



    private void initializeViews() {
//        dataTextView = findViewById(R.id.data_text_view);
//        imageView = findViewById(R.id.image_view);
        dashboard_button = findViewById(R.id.back_dashboard);
        displayText = new StringBuilder();
    }

    private void setupWindowAppearance() {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.green));
    }

    private boolean processDataWithValidation() {
        // Check if required data exists in TempDataHolder
        if (!validateRequiredData()) {
            return false;
        }

        try {
            // Process barcode
            processBarcode();

            // Process product info
            processProductInfo();

            // Process nutrition data and calculate score
            processNutritionDataAndScore();

            // Process image data
//            processImageData();

            // Send data to Firebase
            sendDataToFirebase();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean validateRequiredData() {
        // Check for minimum required data
        Object barcodeData = TempDataHolder.getData(TempDataHolder.KEY_BARCODE);
        Object productInfo = TempDataHolder.getData(TempDataHolder.KEY_PRODUCT_INFO);
        Object nutritionData = TempDataHolder.getData(TempDataHolder.KEY_NUTRITION);

        return barcodeData != null && productInfo != null && nutritionData != null;
    }

    private void processBarcode() {
        String barcode = (String) TempDataHolder.getData(TempDataHolder.KEY_BARCODE);
        if (barcode != null) {
            displayText.append("Barcode: ").append(barcode).append("\n\n");
        }
    }

    private void processProductInfo() {
        @SuppressWarnings("unchecked")
        HashMap<String, String> productInfo =
                (HashMap<String, String>) TempDataHolder.getData(TempDataHolder.KEY_PRODUCT_INFO);

        if (productInfo != null) {
            displayText.append("Product Information:\n")
                    .append("Name: ").append(productInfo.get("name")).append("\n")
                    .append("Brand: ").append(productInfo.get("brand")).append("\n")
                    .append("Category: ").append(productInfo.get("category")).append("\n\n");
        }
    }

//    private void processImageData() {
//        @SuppressWarnings("unchecked")
//        HashMap<String, String> imageData =
//                (HashMap<String, String>) TempDataHolder.getData(TempDataHolder.KEY_IMAGE_DATA);
//
//        if (imageData != null && imageData.get("uri") != null) {
//            loadImage(Uri.parse(imageData.get("uri")));
//        }
//    }

    private void setupDashboardButton() {
        dashboard_button.setOnClickListener(view -> {
            Intent intent = new Intent(OcrEnd.this, Dashboard.class);
            startActivity(intent);
            finish();
        });
    }

//    private void processAllData() {
//        displayText = new StringBuilder();
//
//        // Get barcode data
//        String barcode = (String) TempDataHolder.getData(TempDataHolder.KEY_BARCODE);
//        if (barcode != null) {
//            displayText.append("Barcode: ").append(barcode).append("\n\n");
//        }
//
//        // Get product info
//        @SuppressWarnings("unchecked")
//        HashMap<String, String> productInfo =
//                (HashMap<String, String>) TempDataHolder.getData(TempDataHolder.KEY_PRODUCT_INFO);
//        if (productInfo != null) {
//            displayText.append("Product Information:\n")
//                    .append("Name: ").append(productInfo.get("name")).append("\n")
//                    .append("Brand: ").append(productInfo.get("brand")).append("\n")
//                    .append("Category: ").append(productInfo.get("category")).append("\n\n");
//        }
//
//        // Process nutrition data and calculate score
//        processNutritionDataAndScore();
//
//        // Get image data
//        @SuppressWarnings("unchecked")
//        HashMap<String, String> imageData =
//                (HashMap<String, String>) TempDataHolder.getData(TempDataHolder.KEY_IMAGE_DATA);
//        if (imageData != null) {
//            String uri = imageData.get("uri");
//            if (uri != null) {
//                loadImage(Uri.parse(uri));
//            }
//        }
//
//        // Display the text
////        dataTextView = findViewById(R.id.data_text_view);
////        dataTextView.setText(displayText.toString());
//
//       sendDataToFirebase();
//        // Optional: Clear the data after using it
//        TempDataHolder.clearAll();
//    }

    private void processNutritionDataAndScore() {
        Bundle nutritionData = (Bundle) TempDataHolder.getData(TempDataHolder.KEY_NUTRITION);
        Bundle ingredientsData = (Bundle) TempDataHolder.getData(TempDataHolder.KEY_INGREDIENTS);

        // First calculate nutrition score
        int nutritionScore = calculateNutritionScore(nutritionData);
        int additiveScore = 0;

        if (ingredientsData != null) {
            displayText.append("\nIngredients Information:\n");

            // Get regular ingredients
            ArrayList<String> regularIngredients = ingredientsData.getStringArrayList("regular_ingredients");
            if (regularIngredients != null && !regularIngredients.isEmpty()) {
                displayText.append("Regular Ingredients:\n");
                for (String ingredient : regularIngredients) {
                    displayText.append("- ").append(ingredient).append("\n");
                }
            }

            // Get additives
            ArrayList<String> additives = ingredientsData.getStringArrayList("additives");
            if (additives != null) {
                displayText.append("\nAdditives:\n");
                if (!additives.isEmpty()) {
                    for (String additive : additives) {
                        displayText.append("- ").append(additive).append("\n");
                    }
                } else {
                    displayText.append("No additives found (healthier choice)\n");
                }

                // Calculate additives score
                additiveScore = evaluateAdditiveScore(additives);
                displayText.append("\nAdditive Score Impact: ").append(additiveScore).append("\n");
            }
        }

        // Calculate final score by combining nutrition and additive scores
        int finalScore = nutritionScore + additiveScore;

        // Ensure final score is not negative
        finalScore = Math.max(0, finalScore);

        Item_Score = finalScore;

        // Display final score and interpretation
        displayText.append("\nProduct Score Analysis:\n");
        displayText.append("Base Nutrition Score: ").append(nutritionScore).append("\n");
        displayText.append("Additive Score Impact: ").append(additiveScore).append("\n");
        displayText.append("Overall Score: ").append(finalScore).append("\n");
        displayText.append("Score Interpretation: ").append(interpretScore(finalScore)).append("\n\n");
    }

    private int calculateNutritionScore(Bundle nutritionData) {
        int score = 0;
        if (nutritionData != null) {
            JsonObject nutriments = new JsonObject();
            for (String key : nutritionData.keySet()) {
                Object value = nutritionData.get(key);
                if (value instanceof Double) {
                    nutriments.addProperty(key, (Double) value);
                } else if (value instanceof Integer) {
                    nutriments.addProperty(key, (Integer) value);
                } else if (value instanceof String) {
                    try {
                        double numValue = Double.parseDouble((String) value);
                        nutriments.addProperty(key, numValue);
                    } catch (NumberFormatException e) {
                        nutriments.addProperty(key, (String) value);
                    }
                }
                displayText.append(key).append(": ").append(value).append("\n");
            }
            score = calculateOverallScore(nutriments);
        }
        return score;
    }

    private int calculateOverallScore(JsonObject nutriments) {
        int overallScore = 0;

        // Calculate nutrient score based on each nutrient
        for (Map.Entry<String, JsonElement> entry : nutriments.entrySet()) {
            String key = entry.getKey();
            overallScore += calculateNutrientScore(nutriments, key);
        }

        return overallScore;
    }

    private int evaluateAdditiveScore(List<String> additives) {
        AdditiveDatabase db = new AdditiveDatabase(this);

        // If no additives present, return +30
        if (additives == null || additives.isEmpty()) {
            return 30;
        }

        // Start with assuming additives are safe (+15)
        int score = 15;

        // Check if any additive is in danger list
        for (String additive : additives) {
            if (db.getDangerInfo(additive) != null) {
                // If dangerous additive found, score becomes -15 and break
                return -15;
            }
        }

        // If we get here, no dangerous additives were found, return the +15
        return score;
    }


    private String interpretScore(int score) {
        if (score >= 75) {
            return "Excellent - Very healthy choice";
        } else if (score >= 50) {
            return "Good - Healthy choice";
        } else if (score >= 25) {
            return "Fair - Moderately healthy";
        } else if (score >= 0) {
            return "Poor - Less healthy choice";
        } else {
            return "Concerning - Contains potentially harmful additives";
        }
    }


//    private void loadImage(Uri uri) {
////        imageView = findViewById(R.id.image_view);
//        imageView.setVisibility(View.VISIBLE);
//        Glide.with(this)
//                .load(uri)
//                .into(imageView);
//    }

    private int calculateNutrientScore(JsonObject nutriments, String key) {
        int score = 0; // Default score

        if (nutriments.has(key)) {
            int value = nutriments.get(key).getAsInt();

            switch (key) {
                case "sodium_value":
                    if (value == 0) {
                        score += 0;
                    } else if (value > 0 && value < 126) {
                        score += 9;
                    } else if (value > 126 && value < 252) {
                        score += 6;
                    } else if (value > 252 && value < 441) {
                        score += 3;
                    } else {
                        score += 0;
                    }
                    break;

                case "energy_value":
                    if (value >= 0 && value < 112) {
                        score += 9;
                    } else if (value > 112 && value < 252) {
                        score += 6;
                    } else if (value > 252 && value < 392) {
                        score += 3;
                    } else if (value >= 1000) {
                        score -= 9;
                    } else {
                        score += 0;
                    }
                    break;

                case "fiber":
                    if (value == 0) {
                        score += 0;
                    } else if (value > 0 && value < 2.45) {
                        score += 6;
                    } else {
                        score += 9;
                    }
                    break;

                case "proteins":
                    if (value == 0) {
                        score += 0;
                    } else if (value > 0 && value < 5.2) {
                        score += 6;
                    } else {
                        score += 9;
                    }
                    break;

                case "sugars":
                    if (value == 0) {
                        score += 9;
                    } else if (value > 0 && value < 6.3) {
                        score += 9;
                    } else if (value > 6.3 && value < 12.6) {
                        score += 6;
                    } else if (value > 12.6 && value < 21.7) {
                        score += 3;
                    } else {
                        score += 0;
                    }
                    break;

                case "salt":
                     if (value >= 0 && value < 0.46) {
                        score += 9;
                    } else if (value > 0.46 && value < 0.92) {
                        score += 6;
                    } else if (value > 0.92 && value < 1.62) {
                        score += 3;
                    } else {
                        score += 0;
                    }
                    break;

                case "saturated-fat":
                    if (value == 0) {
                        score += 8;
                    } else if (value > 0 && value < 1.4) {
                        score += 9;
                    } else if (value > 1.4 && value < 2.8) {
                        score += 6;
                    } else if (value > 2.8 && value < 4.9) {
                        score += 3;
                    } else {
                        score += 0;
                    }
                    break;

                default:
                    // Handle any other nutrients (default case)
                    score += 0;
                    break;
            }
        }

        return score; // Return the calculated score
    }

    private void sendDataToFirebase() {
        // Get Firebase database reference
        DatabaseReference database = FirebaseDatabase.getInstance().getReference();

        // Get barcode
        String barcode = (String) TempDataHolder.getData(TempDataHolder.KEY_BARCODE);
        if (barcode == null) {
            Toast.makeText(this, "Barcode not found. Cannot upload data.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a map to hold the data
        Map<String, Object> productData = new HashMap<>();

        // Add barcode to data
        productData.put("barcode", barcode);

        // Add product information
        @SuppressWarnings("unchecked")
        HashMap<String, String> productInfo =
                (HashMap<String, String>) TempDataHolder.getData(TempDataHolder.KEY_PRODUCT_INFO);
        if (productInfo != null) {
            productData.put("Product_Name", productInfo.get("name"));
            productData.put("Product_Brand", productInfo.get("brand"));
            productData.put("Product_Category", productInfo.get("category"));
        }

        // Add nutrition information
        Bundle nutritionData = (Bundle) TempDataHolder.getData(TempDataHolder.KEY_NUTRITION);
        if (nutritionData != null) {
            Map<String, Object> nutritionMap = new HashMap<>();
            for (String key : nutritionData.keySet()) {
                Object value = nutritionData.get(key);
                if (value != null) {
                    nutritionMap.put(key, value.toString());
                }
            }
            productData.put("Nutrition", nutritionMap);
        }

        // Add ingredients and additives
        Bundle ingredientsData = (Bundle) TempDataHolder.getData(TempDataHolder.KEY_INGREDIENTS);
        if (ingredientsData != null) {
            ArrayList<String> regularIngredients = ingredientsData.getStringArrayList("regular_ingredients");
            if (regularIngredients != null) {
                productData.put("Ingredients", regularIngredients);
            }

            ArrayList<String> additives = ingredientsData.getStringArrayList("additives");
            if (additives != null) {
                productData.put("Additives", additives);
            }
        }

        // Add overall score and interpretation
        productData.put("Score", Item_Score);
        productData.put("Recommendation", interpretScore(Item_Score));

        // Get image data
        @SuppressWarnings("unchecked")
        HashMap<String, String> imageData =
                (HashMap<String, String>) TempDataHolder.getData(TempDataHolder.KEY_IMAGE_DATA);

        if (imageData != null && imageData.get("uri") != null) {
            // Compress and upload image, then update database
            compressAndUploadImage(Uri.parse(imageData.get("uri")), barcode, productData, database);
        } else {
            // No image to upload, just save the data
            saveToDatabase(database, barcode, productData);
        }
    }
    private void uploadImageToStorage(Uri imageUri, String barcode, Map<String, Object> productData,
                                      DatabaseReference database) {
        // Get storage reference
        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child("product_images")
                .child(barcode + ".jpg");

        // Upload image
        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Get the download URL
                    storageRef.getDownloadUrl()
                            .addOnSuccessListener(downloadUri -> {
                                // Add image URL to product data
                                productData.put("imageUrl", downloadUri.toString());
                                // Save all data to database
                                saveToDatabase(database, barcode, productData);
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(OcrEnd.this,
                                            "Failed to get image URL: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(OcrEnd.this,
                                "Failed to upload image: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    private void compressAndUploadImage(Uri imageUri, String barcode,
                                        Map<String, Object> productData,
                                        DatabaseReference database) {
        try {
            // Convert URI to Bitmap
            Bitmap originalBitmap = MediaStore.Images.Media.getBitmap(
                    getContentResolver(), imageUri);

            // Calculate dimensions maintaining aspect ratio
            int maxDimension = 1024; // Max width or height
            int originalWidth = originalBitmap.getWidth();
            int originalHeight = originalBitmap.getHeight();
            float scale = Math.min(
                    (float) maxDimension / originalWidth,
                    (float) maxDimension / originalHeight
            );

            // Skip resizing if image is already smaller
            if (scale >= 1) {
                uploadImageToStorage(imageUri, barcode, productData, database);
                return;
            }

            // Calculate new dimensions
            int newWidth = Math.round(originalWidth * scale);
            int newHeight = Math.round(originalHeight * scale);

            // Create scaled bitmap
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(
                    originalBitmap, newWidth, newHeight, true);

            // Compress to JPEG
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
            byte[] imageData = baos.toByteArray();

            // Clean up bitmaps
            if (originalBitmap != resizedBitmap) {
                originalBitmap.recycle();
            }
            resizedBitmap.recycle();

            // Get storage reference
            StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                    .child("product_images")
                    .child(barcode + ".jpg");

            // Upload compressed image
            storageRef.putBytes(imageData)
                    .addOnSuccessListener(taskSnapshot -> {
                        // Get the download URL
                        storageRef.getDownloadUrl()
                                .addOnSuccessListener(downloadUri -> {
                                    // Add image URL to product data
                                    productData.put("imageUrl", downloadUri.toString());
                                    // Save all data to database
                                    saveToDatabase(database, barcode, productData);
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(OcrEnd.this,
                                                "Failed to get image URL: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show());
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(OcrEnd.this,
                                    "Failed to upload image: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show());

        } catch (IOException e) {
            Toast.makeText(this, "Error processing image: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            // If image processing fails, save data without image
            saveToDatabase(database, barcode, productData);
        }
    }


    private void saveToDatabase(DatabaseReference database, String barcode,
                                Map<String, Object> productData) {
        database.child("products").child(barcode).setValue(productData)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(OcrEnd.this, "Data uploaded successfully!",
                            Toast.LENGTH_SHORT).show();
                    TempDataHolder.clearAll();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(OcrEnd.this,
                                "Failed to upload data: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }


}

// interesting function


//    private int evaluateAdditiveScore(List<String> additives) {
//        AdditiveDatabase db = new AdditiveDatabase(this);
//
//        if (additives.isEmpty()) {
//            return 30; // Bonus for no additives
//        }
//
//        int dangerousCount = 0;
//        int moderateCount = 0;
//        int safeCount = 0;
//
//        for (String additive : additives) {
//            String danger = db.getDangerLevel(additive);
//            switch (danger.toLowerCase()) {
//                case "high":
//                    dangerousCount++;
//                    break;
//                case "moderate":
//                    moderateCount++;
//                    break;
//                case "low":
//                    safeCount++;
//                    break;
//            }
//        }
//
//        // Calculate score based on additive composition
//        int score = 0;
//        score -= (dangerousCount * 15); // Severe penalty for dangerous additives
//        score -= (moderateCount * 5);  // Moderate penalty for concerning additives
//        score -= (safeCount * 2);      // Small penalty for safe additives
//
//        // Cap the minimum score at -30
//        return Math.max(score, -30);
//    }




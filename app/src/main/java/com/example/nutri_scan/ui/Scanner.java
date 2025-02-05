package com.example.nutri_scan.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.budiyev.android.codescanner.DecodeCallback;
import com.bumptech.glide.Glide;
import com.example.nutri_scan.data.AdditiveDatabase;
import com.example.nutri_scan.data.AdditiveItem;
import com.example.nutri_scan.adapter.NutrientAdapter;
import com.example.nutri_scan.data.NutrientItem;
import com.example.nutri_scan.R;
import com.example.nutri_scan.data.TempDataHolder;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.zxing.Result;

import org.jetbrains.annotations.Contract;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Scanner extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE = 100;
    private DatabaseReference databaseRef;

    // different layout rendering
    private View normalLayout;
    private View unknownProductLayout;

    private CodeScanner mCodeScanner;
    private ImageView productImage;
    private TextView scoreTextView;
    private TextView nameTextView;
    private TextView brandTextView;
    private TextView scoreDescriptionTextView;
    private RecyclerView negativesRecyclerView;
    private RecyclerView positivesRecyclerView;
    private View bottomSheet;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private List<AdditiveItem> additivesList;
    private Button add_product;
    private ImageView score_icon;

    private String Product_name;
    private String Product_brand;

    private String description;
    private int sodiumScore;
    private int energyScore; //Calories
    private int fiberScore;
    private int proteinScore;
    private int sugarscore;
    private int saltscore;
    private int fatscore;
    String decoded_result;

    private ShimmerFrameLayout shimmerFrameLayout;
    private LinearLayout noshimmer;

    private static final String Food_Facts_endpoint = "https://world.openfoodfacts.org/api/v2/product/";
    private static final String TAG = "Scanner";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        databaseRef = FirebaseDatabase.getInstance().getReference().child("products");

        bottomSheet = findViewById(R.id.sheets);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        normalLayout = findViewById(R.id.normal_layout);
        unknownProductLayout = findViewById(R.id.unknown_product_layout);
        add_product = findViewById(R.id.add_product);
        score_icon = findViewById(R.id.score_icon);

        shimmerFrameLayout=findViewById(R.id.shimmer);
        noshimmer =findViewById(R.id.no_shimmer);

        productImage = findViewById(R.id.product_image);
        scoreTextView = findViewById(R.id.score);
        nameTextView = findViewById(R.id.name);
        brandTextView = findViewById(R.id.brand);
        scoreDescriptionTextView = findViewById(R.id.score_description);
        negativesRecyclerView = findViewById(R.id.negatives_recycler_view);
        positivesRecyclerView = findViewById(R.id.positives_recycler_view);

        negativesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        positivesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        CodeScannerView scannerView = findViewById(R.id.scanner_views);
        mCodeScanner = new CodeScanner(this, scannerView);

        EditText editTextProductInfo = findViewById(R.id.editTextProductInfo);
        EditText AskDieto = findViewById(R.id.Askdieto);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_CODE);}


        mCodeScanner.setDecodeCallback(new DecodeCallback() {
            @Override
            public void onDecoded(@NonNull Result result) {
                runOnUiThread(() -> {
                    showShimmer();
                    Toast.makeText(Scanner.this, result.getText(), Toast.LENGTH_SHORT).show();
                    decoded_result = result.getText();
                    resetScores();
                    checkFirebaseForProduct(result.getText());
//                    fetchProductDetails(result.getText());    core functionality
                });

            }

        });

        scannerView.setOnClickListener(v -> mCodeScanner.startPreview());

        editTextProductInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Scanner.this, AddProduct.class);
                onBarcodeScanned(decoded_result);
                startActivity(intent);
            }
        });

        AskDieto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Scanner.this, Dieto.class);
                intent.putExtra("EXTRA_PRODUCT_NAME", Product_name);
                intent.putExtra("EXTRA_PRODUCT_BRAND", Product_brand);
                startActivity(intent);
            }
        });

    }


    ////Initialising Scores with Zero ///////////////
    ///Required for core Functionality///////////////
    private void resetScores() {
        sodiumScore = 0;
        energyScore = 0;
        fiberScore = 0;
        proteinScore = 0;
        sugarscore = 0;
        saltscore = 0;
        fatscore = 0;
    }


    // For Firebase//////////////////////
    //check/////
    private void checkFirebaseForProduct(String barcode) {
        showShimmer();
        databaseRef.child(barcode).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Product found in Firebase
                    parseAndDisplayFirebaseData(dataSnapshot);
                } else {
                    // Product not found in Firebase, try API
                    fetchProductDetails(barcode);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Error reading from Firebase, fallback to API
                fetchProductDetails(barcode);
            }
        });
    }



    //Used for Core functionality/////////////////////////////
    //Display data From Firebase/////////////////////////////
    private void parseAndDisplayFirebaseData(DataSnapshot dataSnapshot) {
        try {
            // Create lists for nutrients
            List<NutrientItem> negatives = new ArrayList<>();
            List<NutrientItem> positives = new ArrayList<>();
            List<AdditiveItem> additivesList = new ArrayList<>();

            // Get basic product info
            String productName = dataSnapshot.child("Product_Name").getValue(String.class);
            String brand = dataSnapshot.child("Product_Brand").getValue(String.class);
            String imageUrl = dataSnapshot.child("imageUrl").getValue(String.class);
            Integer score = dataSnapshot.child("Score").getValue(Integer.class);

            Product_name = productName;
            Product_brand = brand;

            // Get nutrition data
            DataSnapshot nutritionSnapshot = dataSnapshot.child("Nutrition");
            if (nutritionSnapshot.exists()) {
                Map<String, String> nutritionMap = new HashMap<>();
                for (DataSnapshot nutrient : nutritionSnapshot.getChildren()) {
                    nutritionMap.put(nutrient.getKey(), nutrient.getValue(String.class));
                }

                // Parse nutrients
                parseNutrient(nutritionMap, "energy_value", "Energy", negatives, positives);
                parseNutrient(nutritionMap, "sodium_value", "Sodium", negatives, positives);
                parseNutrient(nutritionMap, "saturated-fat", "Saturates", negatives, positives);
                parseNutrient(nutritionMap, "sugars", "Sugar", negatives, positives);
                parseNutrient(nutritionMap, "salt", "Salt", negatives, positives);
                parseNutrient(nutritionMap, "proteins", "Protein", negatives, positives);
                parseNutrient(nutritionMap, "fiber", "Fiber", negatives, positives);
                parseNutrient(nutritionMap, "carbohydrates", "Carbs", negatives, positives);
            }

            // Get additives
            DataSnapshot additivesSnapshot = dataSnapshot.child("Additives");
            if (additivesSnapshot.exists()) {
                AdditiveDatabase additiveDatabase = new AdditiveDatabase(this);
                for (DataSnapshot additive : additivesSnapshot.getChildren()) {
                    String additiveCode = additive.getValue(String.class).toUpperCase();
                    AdditiveItem additiveItem = additiveDatabase.getAdditiveInfo(additiveCode);
                    if (additiveItem != null) {
                        additivesList.add(additiveItem);
                        negatives.add(new NutrientItem(R.drawable.icon_additive, additiveCode,
                                "Additive", "", R.color.neutral, true));
                    }
                }
            }

            this.additivesList = additivesList;

            // Update UI
            runOnUiThread(() -> {
                if ((negatives.isEmpty() && positives.isEmpty()) ||
                        productName == null || imageUrl == null) {
                    showUnknownProductLayout();
                } else {
                    showNormalLayout();
                    updateUI(productName, brand, score, imageUrl, negatives, positives);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error parsing Firebase data", e);
            runOnUiThread(this::showUnknownProductLayout);
        }
    }

    ///Helper to obtain Data From Firebase//////////////////////////
    private void updateUI(String productName, String brand, int score,
                          String imageUrl, List<NutrientItem> negatives,
                          List<NutrientItem> positives) {
        shimmerFrameLayout.stopShimmer();
        shimmerFrameLayout.setVisibility(View.GONE);
        noshimmer.setVisibility(View.VISIBLE);

        nameTextView.setText(productName);
        brandTextView.setText(brand);
        scoreTextView.setText(score + "/100");
        scoreDescriptionTextView.setText(getScoreDescription(score));

        description = getScoreDescription(score);
        updateScoreIcon(description);

        if (imageUrl != null) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.logo)
                    .into(productImage);
        } else {
            productImage.setImageResource(R.drawable.logo);
        }

        negativesRecyclerView.setAdapter(new NutrientAdapter(this, negatives, additivesList));
        positivesRecyclerView.setAdapter(new NutrientAdapter(this, positives, additivesList));

        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    private void updateScoreIcon(String description) {
        if (description.equals("Bad")) {
            score_icon.setImageResource(R.drawable.icon_red_circle);
        } else if (description.equals("Poor")) {
            score_icon.setImageResource(R.drawable.icon_orange_circle);
        } else {
            score_icon.setImageResource(R.drawable.icon_green_circle);
        }
    }

    /// Helper to obtain firebase data //////////////////////
    private void parseNutrient(Map<String, String> nutritionMap, String key,
                               String displayName,
                               List<NutrientItem> negatives,
                               List<NutrientItem> positives) {
        String value = nutritionMap.get(key);
        if (value != null) {
            try {
                double numValue = Double.parseDouble(value);
                String formattedValue = String.format("%.2f", numValue);
                int icon = getNutrientIcon(key);
                String description;
                int color;

                switch (key) {
                    case "sodium_value":
                        if (numValue == 0) {
                            description = "No Sodium";
                            color = R.color.neutral;
                            positives.add(new NutrientItem(icon, displayName, description, formattedValue + " mg", color));
                            sodiumScore = 0;
                        } else if (numValue > 0 && numValue < 126) {
                            description = "Healthy amounts";
                            color = R.color.positive;
                            positives.add(new NutrientItem(icon, displayName, description, formattedValue + " mg", color));
                            sodiumScore = 9;
                        } else if (numValue > 126 && numValue < 252) {
                            description = "Fair amounts";
                            color = R.color.positive;
                            positives.add(new NutrientItem(icon, displayName, description, formattedValue + " mg", color));
                            sodiumScore = 6;
                        } else if (numValue > 252 && numValue < 441) {
                            description = "High amounts";
                            color = R.color.moderate;
                            negatives.add(new NutrientItem(icon, displayName, description, formattedValue + " mg", color));
                            sodiumScore = 3;
                        } else {
                            description = "Extreme amounts";
                            color = R.color.negative;
                            negatives.add(new NutrientItem(icon, displayName, description, formattedValue + " mg", color));
                            sodiumScore = 0;
                        }
                        break;

                    case "energy_value":
                        if (numValue >= 0 && numValue < 112) {
                            description = "Low caloric";
                            color = R.color.positive;
                            positives.add(new NutrientItem(icon, displayName, description, formattedValue + " kcal", color));
                            energyScore = 9;
                        } else if (numValue > 112 && numValue < 252) {
                            description = "A bit caloric";
                            color = R.color.positive;
                            positives.add(new NutrientItem(icon, displayName, description, formattedValue + " kcal", color));
                            energyScore = 6;
                        } else if (numValue > 252 && numValue < 392) {
                            description = "Moderate caloric";
                            color = R.color.moderate;
                            negatives.add(new NutrientItem(icon, displayName, description, formattedValue + " kcal", color));
                            energyScore = 3;
                        } else if (numValue >= 1000) {
                            description = "Extremely caloric";
                            color = R.color.negative;
                            negatives.add(new NutrientItem(icon, displayName, description, formattedValue + " kcal", color));
                            energyScore = -9;
                        } else {
                            description = "Highly Caloric";
                            color = R.color.negative;
                            negatives.add(new NutrientItem(icon, displayName, description, formattedValue + " kcal", color));
                            energyScore = 0;
                        }
                        break;

                    case "fiber":
                        if (numValue == 0) {
                            description = "No fiber";
                            color = R.color.neutral;
                            negatives.add(new NutrientItem(icon, displayName, description, formattedValue + " g", color));
                            fiberScore = 0;
                        } else if (numValue > 0 && numValue < 2.45) {
                            description = "Good source";
                            color = R.color.positive;
                            positives.add(new NutrientItem(icon, displayName, description, formattedValue + " g", color));
                            fiberScore = 6;
                        } else {
                            description = "Excellent source";
                            color = R.color.positive;
                            positives.add(new NutrientItem(icon, displayName, description, formattedValue + " g", color));
                            fiberScore = 9;
                        }
                        break;

                    case "proteins":
                        if (numValue == 0) {
                            description = "No protein";
                            color = R.color.neutral;
                            negatives.add(new NutrientItem(icon, displayName, description, formattedValue + " g", color));
                            proteinScore = 0;
                        } else if (numValue > 0 && numValue < 5.2) {
                            description = "Good source";
                            color = R.color.positive;
                            positives.add(new NutrientItem(icon, displayName, description, formattedValue + " g", color));
                            proteinScore = 6;
                        } else {
                            description = "Excellent source";
                            color = R.color.positive;
                            positives.add(new NutrientItem(icon, displayName, description, formattedValue + " g", color));
                            proteinScore = 9;
                        }
                        break;

                    case "sugars":
                        if (numValue == 0) {
                            description = "No sugar";
                            color = R.color.neutral;
                            positives.add(new NutrientItem(icon, displayName, description, formattedValue + " g", color));
                            sugarscore = 9;
                        } else if (numValue > 0 && numValue < 6.3) {
                            description = "Low sugar";
                            color = R.color.positive;
                            positives.add(new NutrientItem(icon, displayName, description, formattedValue + " g", color));
                            sugarscore = 9;
                        } else if (numValue > 6.3 && numValue < 12.6) {
                            description = "A bit sugary";
                            color = R.color.positive;
                            positives.add(new NutrientItem(icon, displayName, description, formattedValue + " g", color));
                            sugarscore = 6;
                        } else if (numValue > 12.6 && numValue < 21.7) {
                            description = "Too sugary";
                            color = R.color.moderate;
                            negatives.add(new NutrientItem(icon, displayName, description, formattedValue + " g", color));
                            sugarscore = 3;
                        } else {
                            description = "Extremely sugary";
                            color = R.color.negative;
                            negatives.add(new NutrientItem(icon, displayName, description, formattedValue + " g", color));
                            sugarscore = 0;
                        }
                        break;

                    case "salt":
                        if (numValue == 0) {
                            description = "No salt";
                            color = R.color.neutral;
                            positives.add(new NutrientItem(icon, displayName, description, formattedValue + " g", color));
                            saltscore = 9;
                        } else if (numValue > 0 && numValue < 0.46) {
                            description = "Low salt";
                            color = R.color.positive;
                            positives.add(new NutrientItem(icon, displayName, description, formattedValue + " g", color));
                            saltscore = 9;
                        } else if (numValue > 0.46 && numValue < 0.92) {
                            description = "A bit salty";
                            color = R.color.positive;
                            positives.add(new NutrientItem(icon, displayName, description, formattedValue + " g", color));
                            saltscore = 6;
                        } else if (numValue > 0.92 && numValue < 1.62) {
                            description = "Moderately salty";
                            color = R.color.moderate;
                            negatives.add(new NutrientItem(icon, displayName, description, formattedValue + " g", color));
                            saltscore = 3;
                        } else {
                            description = "Too salty";
                            color = R.color.negative;
                            negatives.add(new NutrientItem(icon, displayName, description, formattedValue + " g", color));
                            saltscore = 0;
                        }
                        break;

                    case "saturated-fat":
                        if (numValue == 0) {
                            description = "No fat";
                            color = R.color.neutral;
                            positives.add(new NutrientItem(icon, displayName, description, formattedValue + " g", color));
                            fatscore = 8;
                        } else if (numValue > 0 && numValue < 1.4) {
                            description = "Low fat";
                            color = R.color.positive;
                            positives.add(new NutrientItem(icon, displayName, description, formattedValue + " g", color));
                            fatscore = 9;
                        } else if (numValue > 1.4 && numValue < 2.8) {
                            description = "A bit fatty";
                            color = R.color.positive;
                            positives.add(new NutrientItem(icon, displayName, description, formattedValue + " g", color));
                            fatscore = 6;
                        } else if (numValue > 2.8 && numValue < 4.9) {
                            description = "Moderately fatty";
                            color = R.color.moderate;
                            negatives.add(new NutrientItem(icon, displayName, description, formattedValue + " g", color));
                            fatscore = 3;
                        } else {
                            description = "Too fatty";
                            color = R.color.negative;
                            negatives.add(new NutrientItem(icon, displayName, description, formattedValue + " g", color));
                            fatscore = 0;
                        }
                        break;
                    case "carbohydrates":
                        if (numValue == 0) {
                            description = "No Carbs-keto";
                            color = R.color.neutral;
                            positives.add(new NutrientItem(icon, displayName, description, formattedValue + "g", color));
                        } else if (numValue > 0 && numValue < 10) {
                            description = "Healthy amounts";
                            color = R.color.positive;
                            positives.add(new NutrientItem(icon, displayName, description, formattedValue + "g", color));
                        } else if (numValue > 10 && numValue < 30) {
                            description = "Fair amounts";
                            color = R.color.positive;
                            positives.add(new NutrientItem(icon, displayName, description, formattedValue + "g", color));
                        } else if (numValue > 30 && numValue < 50) {
                            description = "High amounts";
                            color = R.color.moderate;
                            negatives.add(new NutrientItem(icon, displayName, description, formattedValue + "g", color));
                        } else {
                            description = "Extreme amounts";
                            color = R.color.negative;
                            negatives.add(new NutrientItem(icon, displayName, description, formattedValue + "g", color));
                        }
                        break;

                    default:
                        description = "";
                        color = R.color.neutral;
                        positives.add(new NutrientItem(icon, displayName, description, formattedValue + " g", color, true));
                        break;
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "Error parsing nutrient value: " + e.getMessage());
            }
        }
    }

    @Contract(pure = true)
    private int getNutrientIcon(String nutrient) {
        switch (nutrient) {
            case "energy_value":
                return R.drawable.icon_fire;
            case "sodium_value":
                return R.drawable.icon_sodium;
            case "fat_value":                   //not in use
                return R.drawable.icon_fat;
            case "carbohydrates":
                return R.drawable.icon_carbs;
            case "saturated-fat":
                return R.drawable.icon_saturates;
            case "sugars":
                return R.drawable.icon_sugar;
            case "salt":
                return R.drawable.icon_salt;
            case "proteins":
                return R.drawable.icon_protein;
            case "fiber":
                return R.drawable.icon_fiber;
            default:
                return R.drawable.icon_scanner;
        }
    }

    private void saveProductToFirebase(String barcode, String productName, String brand,
                                       String imageUrl, List<NutrientItem> negatives,
                                       List<NutrientItem> positives, int score,
                                       List<String> keywords, String categories,
                                       List<String> ingredientsList, JsonObject nutriments) {
        DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        Map<String, Object> productData = new HashMap<>();

        // Basic product information
        productData.put("barcode", barcode);
        productData.put("Product_Name", productName);
        productData.put("Product_Brand", brand);
        productData.put("Score", score);
        productData.put("Recommendation", interpretScore(score));

        if (categories != null && !categories.isEmpty()) {
            productData.put("Product_Category", categories);
        }

        // Store keywords in the same way as additives
        if (keywords != null && !keywords.isEmpty()) {
            productData.put("Keywords", keywords);
        }

        // Store ingredients as ingredients_text
        if (ingredientsList != null && !ingredientsList.isEmpty()) {
            productData.put("Ingredients", ingredientsList);
        }


        // Nutrition information
        Map<String, Object> nutritionMap = new HashMap<>();

        if (nutriments.has("fat_100g")) {
            nutritionMap.put("total-fat", nutriments.get("fat_100g").getAsString());
        }
        if (nutriments.has("carbohydrates_100g")) {
            nutritionMap.put("carbohydrates", nutriments.get("carbohydrates_100g").getAsString());
        }


        // Process both negative and positive nutrients
        List<NutrientItem> allNutrients = new ArrayList<>();
        allNutrients.addAll(negatives);
        allNutrients.addAll(positives);

        for (NutrientItem nutrient : allNutrients) {
            // Skip if the nutrient name is an enumber (e.g., E100, E200, etc.)
            if (isEnumber(nutrient.getName())) {
                continue;
            }

            // Remove unit and convert to string value only
            String value = nutrient.getValue().replaceAll("[^0-9.]", "");
            nutritionMap.put(getNutrientKey(nutrient.getName()), value);
        }
        productData.put("Nutrition", nutritionMap);

        // Process additives
        if (additivesList != null && !additivesList.isEmpty()) {
            List<String> additiveCodes = new ArrayList<>();
            for (AdditiveItem additive : additivesList) {
                additiveCodes.add(additive.getCode());
            }
            productData.put("Additives", additiveCodes);
        }

        // Handle image upload if URL exists
        if (imageUrl != null && !imageUrl.isEmpty()) {
            downloadAndUploadImage(imageUrl, barcode, productData, database);
        } else {
            saveToDatabase(database, barcode, productData);
        }
    }

    // Helper method to check if a nutrient name is an enumber
    private boolean isEnumber(String nutrientName) {
        // Check if the nutrient name starts with "E" followed by numbers
        return nutrientName.matches("^E\\d{3}[a-zA-Z]?$");
    }


    private String getNutrientKey(String nutrientName) {
        switch (nutrientName.toLowerCase()) {
            case "energy":
                return "energy_value";
            case "sodium":
                return "sodium_value";
            case "saturates":
                return "saturated-fat";
            case "sugar":
                return "sugars";
            case "salt":
                return "salt";
            case "protein":
                return "proteins";
            case "fiber":
                return "fiber";
            default:
                return nutrientName.toLowerCase();
        }
    }

    private void downloadAndUploadImage(String imageUrl, String barcode,
                                        Map<String, Object> productData,
                                        DatabaseReference database) {
        // Create a temporary file to store the downloaded image
        File outputDir = getCacheDir();
        try {
            File outputFile = File.createTempFile("temp_image", ".jpg", outputDir);

            // Download image using OkHttp
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(imageUrl).build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        Toast.makeText(Scanner.this,
                                "Failed to download image: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        saveToDatabase(database, barcode, productData);
                    });
                }
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        runOnUiThread(() -> {
                            Toast.makeText(Scanner.this,
                                    "Failed to download image",
                                    Toast.LENGTH_SHORT).show();
                            saveToDatabase(database, barcode, productData);
                        });
                        return;
                    }

                    // Save the image to temporary file
                    try (InputStream inputStream = response.body().byteStream();
                         FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                    }

                    // Compress and upload the image
                    compressAndUploadImage(Uri.fromFile(outputFile), barcode,
                            productData, database);
                }
            });
        } catch (IOException e) {
            Toast.makeText(this,
                    "Error processing image: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            saveToDatabase(database, barcode, productData);
        }
    }

    private void compressAndUploadImage(Uri imageUri, String barcode,
                                        Map<String, Object> productData,
                                        DatabaseReference database) {
        try {
            // Convert URI to Bitmap
            Bitmap originalBitmap = MediaStore.Images.Media.getBitmap(
                    getContentResolver(), imageUri);

            // Calculate dimensions maintaining aspect ratio
            int maxDimension = 1024;
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
                                        runOnUiThread(() -> Toast.makeText(Scanner.this,
                                                "Failed to get image URL: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show()));
                    })
                    .addOnFailureListener(e ->
                            runOnUiThread(() -> Toast.makeText(Scanner.this,
                                    "Failed to upload image: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show()));

        } catch (IOException e) {
            Toast.makeText(this, "Error processing image: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            // If image processing fails, save data without image
            saveToDatabase(database, barcode, productData);
        }
    }


    private void uploadImageToStorage(Uri imageUri, String barcode,
                                      Map<String, Object> productData,
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
                                    runOnUiThread(() -> Toast.makeText(Scanner.this,
                                            "Failed to get image URL: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show()));
                })
                .addOnFailureListener(e ->
                        runOnUiThread(() -> Toast.makeText(Scanner.this,
                                "Failed to upload image: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()));
    }


    private void saveToDatabase(DatabaseReference database, String barcode,
                                Map<String, Object> productData) {
        database.child("products").child(barcode).setValue(productData)
                .addOnSuccessListener(unused ->
                        runOnUiThread(() -> Toast.makeText(Scanner.this,
                                "Data uploaded successfully!",
                                Toast.LENGTH_SHORT).show()))
                .addOnFailureListener(e ->
                        runOnUiThread(() -> Toast.makeText(Scanner.this,
                                "Failed to upload data: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()));
    }

    private String interpretScore(int score) {
        if (score <= 30) return "Avoid this product";
        if (score <= 50) return "Consume in moderation";
        if (score <= 72) return "Good choice";
        return "Excellent choice";
    }




    /////////////////// For API Fetch //////////////////////////////////


    /// Obtain data from Openfoodfacts if Firebase fails///////
    private void fetchProductDetails(String barcode) {
        showShimmer();
        OkHttpClient client = new OkHttpClient();
        String url = Food_Facts_endpoint + barcode + "?fields=product_name,_keywords,categories,ingredients_text_en,brands,nutriscore_data,nutriments,image_url,additives_tags,additives_tags";

        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                hideShimmer();
                runOnUiThread(() -> showUnknownProductLayout());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String jsonData = response.body().string();
                    Log.d(TAG, "Response data: " + jsonData);
                    parseAndDisplayData(jsonData);
                } else {
                    hideShimmer();
                    runOnUiThread(() -> showUnknownProductLayout());
                }
            }
        });
    }


    //do not change Code with basic functionality /////////////////
    private void showUnknownProductLayout() {
        hideShimmer();
        normalLayout.setVisibility(View.GONE);
        unknownProductLayout.setVisibility(View.VISIBLE);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);


        add_product.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Scanner.this, AddProduct.class);
                onBarcodeScanned(decoded_result);
                startActivity(intent);
            }
        });
    }

    public void onBarcodeScanned(String result) {
        TempDataHolder.saveData(TempDataHolder.KEY_BARCODE, result);
        // Now you can start any activity, the data will be available
        Intent intent = new Intent(this, OcrEnd.class);
        startActivity(intent);
    }

    private void showNormalLayout() {
        hideShimmer();
        normalLayout.setVisibility(View.VISIBLE);
        unknownProductLayout.setVisibility(View.GONE);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }


    @SuppressLint("SetTextI18n")
    private void parseAndDisplayData(String jsonData) {
        JsonObject data = JsonParser.parseString(jsonData).getAsJsonObject();
        JsonObject product = data.getAsJsonObject("product") != null ? data.getAsJsonObject("product") : new JsonObject();
        JsonObject nutriments = product.getAsJsonObject("nutriments") != null ? product.getAsJsonObject("nutriments") : new JsonObject();

        String productName = product.has("product_name") ? product.get("product_name").getAsString() : "N/A";
        String brand = product.has("brands") ? product.get("brands").getAsString() : "N/A";
        String imageUrl = product.has("image_url") ? product.get("image_url").getAsString() : null;

        Product_name = productName;
        Product_brand = brand;
                // Fetch additional fields like _keywords, categories, and ingredients_text_en
        List<String> keywords = new ArrayList<>();
        if (product.has("_keywords")) {
            JsonArray keywordsArray = product.getAsJsonArray("_keywords");
            for (JsonElement keywordElement : keywordsArray) {
                keywords.add(keywordElement.getAsString());
            }
        }

        String categories = product.has("categories") ? product.get("categories").getAsString() : "N/A";

        List<String> ingredientsList = new ArrayList<>();
        if (product.has("ingredients_text_en")) {
            String[] ingredientsArray = product.get("ingredients_text_en").getAsString().split(",");
            for (String ingredient : ingredientsArray) {
                ingredientsList.add(ingredient.trim()); // Trim to remove extra spaces
            }
        }

        List<NutrientItem> negatives = new ArrayList<>();
        List<NutrientItem> positives = new ArrayList<>();

        // Adjust the parseNutrient calls
        parseNutrient(nutriments, "energy-kcal_100g", "Energy", "", negatives, positives);
        parseNutrient(nutriments, "sodium_100g", "Sodium", "", negatives, positives);
        parseNutrient(nutriments, "saturated-fat_100g", "Saturates", "", negatives, positives);
        parseNutrient(nutriments, "sugars_100g", "Sugar", "", negatives, positives);
        parseNutrient(nutriments, "salt_100g", "Salt", "", negatives, positives);
        parseNutrient(nutriments, "proteins_100g", "Protein", "", negatives, positives);
        parseNutrient(nutriments, "fiber_100g", "Fiber", "", negatives, positives);

        // Fetch additives_tags and add them as positives after removing "en:" prefix

        List<AdditiveItem> additivesList = new ArrayList<>();
        if (product.has("additives_tags")) {
            AdditiveDatabase additiveDatabase = new AdditiveDatabase(this);  // Initialize your AdditiveDatabase

            for (int i = 0; i < product.getAsJsonArray("additives_tags").size(); i++) {
                String additiveCode = product.getAsJsonArray("additives_tags").get(i).getAsString().replace("en:", "").toUpperCase();
                String additive = product.getAsJsonArray("additives_tags").get(i).getAsString().replace("en:", "").toUpperCase();
                AdditiveItem additiveItem = additiveDatabase.getAdditiveInfo(additiveCode);  // Get the AdditiveItem using its code

                if (additiveItem != null) {
                    additivesList.add(additiveItem);  // Add the full AdditiveItem to the list
                    negatives.add(new NutrientItem(R.drawable.icon_additive, additive, "Additive", "", R.color.neutral, true));
                }
            }
        }

        this.additivesList = additivesList;

        int score = evaluateScore();

        runOnUiThread(() -> {
            if ( (negatives.isEmpty() && positives.isEmpty()) || productName.equals("N/A") || imageUrl == null ) {
                // Display the unknown product layout
                showUnknownProductLayout();
            } else {
                // Display the normal product details layout
                showNormalLayout();
                shimmerFrameLayout.stopShimmer();
                shimmerFrameLayout.setVisibility(View.GONE);
                noshimmer.setVisibility(View.VISIBLE);


                nameTextView.setText(productName);
                brandTextView.setText(brand);
                scoreTextView.setText(score + "/100");
                scoreDescriptionTextView.setText(getScoreDescription(score));

                description = getScoreDescription(score);

                if(description.equals("Bad")) {
                    score_icon.setImageResource(R.drawable.icon_red_circle);
                } else if (description.equals("Poor")) {
                    score_icon.setImageResource(R.drawable.icon_orange_circle);
                }
                else {
                    score_icon.setImageResource(R.drawable.icon_green_circle);
                }


                if (imageUrl != null) {
                    Glide.with(this)
                            .load(imageUrl)
                            .placeholder(R.drawable.logo)
                            .into(productImage);
                } else {
                    productImage.setImageResource(R.drawable.logo);
                }

                negativesRecyclerView.setAdapter(new NutrientAdapter(this, negatives, additivesList));
                positivesRecyclerView.setAdapter(new NutrientAdapter(this, positives, additivesList));

                // For Firebase ////////////////////////////////
                saveProductToFirebase(decoded_result, productName, brand, imageUrl, negatives, positives, score, keywords ,categories ,
                        ingredientsList, nutriments);
                //////////////////////////////////////////////

                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });
    }

    private void parseNutrient(JsonObject nutriments, String key, String name, String description, List<NutrientItem> negativesList, List<NutrientItem> positivesList) {
        if (nutriments.has(key)) {
            double value = nutriments.get(key).getAsDouble();
            String formattedValue = String.format("%.2f", value);  // Format value to 2 decimal places
            int icon = getNutrientIconFacts(key);
//            int color = getNutrientColor(key, (float)value);  // Pass the value as a float

            switch (key) {
                case "sodium_100g":
                    if (value == 0){
                        description = "No Sodium";
                        positivesList.add(new NutrientItem(icon, name, description, formattedValue + " mg", R.color.neutral));
                        sodiumScore = 0;
                    }
                    else if (value > 0 && value < 126 ) {
                        description = "Healthy amounts";
                        positivesList.add(new NutrientItem(icon, name, description, formattedValue + " mg", R.color.positive));
                        sodiumScore = 9;
                    }else if (value > 126 && value < 252 ) {
                        description = "Fair amounts";
                        positivesList.add(new NutrientItem(icon, name, description, formattedValue + " mg", R.color.positive));
                        sodiumScore = 6;
                    }else if (value > 252 && value < 441 ) {
                        description = "High amounts";
                        negativesList.add(new NutrientItem(icon, name, description, formattedValue + " mg", R.color.moderate));
                        sodiumScore = 3;
                    }else {
                        description = "Extreme amounts";
                        negativesList.add(new NutrientItem(icon, name, description, formattedValue + " mg", R.color.negative));
                        sodiumScore = 0;
                    }
                    break;

                case "energy-kcal_100g":
                    if (value >= 0 && value < 112 ) {
                        description = "Low caloric";
                        positivesList.add(new NutrientItem(icon, name, description, formattedValue + " kcal", R.color.positive));
                        energyScore = 9;
                    } else if (value > 112 && value < 252){
                        description = "A bit caloric";
                        positivesList.add(new NutrientItem(icon, name, description, formattedValue + " kcal", R.color.positive));
                        energyScore = 6;
                    } else if (value > 252 && value < 392){
                        description = "moderate caloric";
                        negativesList.add(new NutrientItem(icon, name, description, formattedValue + " kcal", R.color.moderate));
                        energyScore = 3;
                    }else if (value >= 1000){
                        description = "Extremely caloric";
                        negativesList.add(new NutrientItem(icon, name, description, formattedValue + " kcal", R.color.negative));
                        energyScore = -9;
                    }else {
                        description = "Highly Caloric";
                        negativesList.add(new NutrientItem(icon, name, description, formattedValue + " kcal", R.color.negative));
                        energyScore = 0;
                    }
                    break;

                case "fiber_100g":
                    if (value == 0 ){
                        description = "No fiber";
                        negativesList.add(new NutrientItem(icon, name, description, formattedValue + " g", R.color.neutral));
                        fiberScore = 0;
                    }else if (value > 0 && value < 2.45) {
                        description = "Good source";
                        positivesList.add(new NutrientItem(icon, name, description, formattedValue + " g", R.color.positive));
                        fiberScore = 6;
                    }
                    else {
                        description = "Excellent source";
                        positivesList.add(new NutrientItem(icon, name, description, formattedValue + " g", R.color.positive));
                        fiberScore = 9;
                    }
                    break;

                case "proteins_100g":
                    if (value == 0){
                        description = "No protein";
                        negativesList.add(new NutrientItem(icon, name, description, formattedValue + " g", R.color.neutral));
                        proteinScore = 0;
                    }
                    else if (value > 0 && value < 5.2) {
                        description = "Good source";
                        positivesList.add(new NutrientItem(icon, name, description, formattedValue + " g", R.color.positive));
                        proteinScore = 6;
                    } else {
                        description = "Excellent source";
                        positivesList.add(new NutrientItem(icon, name, description, formattedValue + " g", R.color.positive));
                        proteinScore = 9;
                    }
                    break;

                case "sugars_100g":
                    if (value == 0){
                        description = "No sugar";
                        positivesList.add(new NutrientItem(icon, name, description, formattedValue + " g", R.color.neutral));
                        sugarscore = 9;
                    }else if (value > 0 && value < 6.3) {
                        description = "Low sugar";
                        positivesList.add(new NutrientItem(icon, name, description, formattedValue + " g", R.color.positive));
                        sugarscore = 9;
                    }else if (value > 6.3 && value < 12.6) {
                        description = "A bit sugary";
                        positivesList.add(new NutrientItem(icon, name, description, formattedValue + " g", R.color.positive));
                        sugarscore = 6;
                    } else if (value > 12.6 && value < 21.7) {
                        description = "Too sugary";
                        negativesList.add(new NutrientItem(icon, name, description, formattedValue + " g", R.color.moderate));
                        sugarscore = 3;
                    }else {
                        description = "Extremely sugary";
                        negativesList.add(new NutrientItem(icon, name, description, formattedValue + " g", R.color.negative));
                        sugarscore = 0;
                    }
                    break;

                case "salt_100g":
                    if (value == 0 ){
                        description = "No salt";
                        negativesList.add(new NutrientItem(icon, name, description, formattedValue + " g", R.color.neutral));
                        saltscore = 9;
                    }
                    else if (value > 0 && value < 0.46) {
                        description = "Low salt";
                        positivesList.add(new NutrientItem(icon, name, description, formattedValue + " g", R.color.positive));
                        saltscore = 9;
                    } else if (value > 0.46 && value < 0.92 ) {
                        description = "A bit salty";
                        positivesList.add(new NutrientItem(icon, name, description, formattedValue + " g", R.color.positive));
                        saltscore = 6;
                    } else if (value > 0.92 && value < 1.62) {
                        description = "Moderately salty";
                        negativesList.add(new NutrientItem(icon, name, description, formattedValue + " g", R.color.moderate));
                        saltscore = 3;
                    }else {
                        description = "Too salty";
                        negativesList.add(new NutrientItem(icon, name, description, formattedValue + " g", R.color.negative));
                        saltscore = 0;
                    }
                    break;

                case "saturated-fat_100g":
                    if (value == 0){
                        description = "No fat";
                        positivesList.add(new NutrientItem(icon, name, description, formattedValue + " g", R.color.neutral));
                        fatscore = 8;
                    }
                    else if (value > 0 && value < 1.4) {
                        description = "Low fat";
                        positivesList.add(new NutrientItem(icon, name, description, formattedValue + " g", R.color.positive));
                        fatscore = 9;
                    } else if (value > 1.4 && value < 2.8) {
                        description = "A bit fatty";
                        positivesList.add(new NutrientItem(icon, name, description, formattedValue + " g", R.color.positive));
                        fatscore = 6;
                    } else if (value > 2.8 && value < 4.9) {
                        description = "moderately fatty";
                        negativesList.add(new NutrientItem(icon, name, description, formattedValue + " g", R.color.moderate));
                        fatscore = 3;
                    }else {
                        description = "Too fatty";
                        negativesList.add(new NutrientItem(icon, name, description, formattedValue + " g", R.color.negative));
                        fatscore = 0;
                    }
                    break;

                default:
                    // In case the nutrient is not handled explicitly, assume it's neutral and add it to positives
                    positivesList.add(new NutrientItem(icon, name, description, formattedValue + " g", R.color.neutral));
                    break;
            }
        }
    }

    @Contract(pure = true)
    private int getNutrientIconFacts(String nutrient) {
        switch (nutrient) {
            case "energy-kcal_100g":
                return R.drawable.icon_fire;
            case "sodium_100g":
                return R.drawable.icon_sodium;
//            case "fat_value":
//                return R.drawable.icon_fat;
            case "carbohydrates":
                return R.drawable.icon_carbs;
            case "saturated-fat_100g":
                return R.drawable.icon_saturates;
            case "sugars_100g":
                return R.drawable.icon_sugar;
            case "salt_100g":
                return R.drawable.icon_salt;
            case "proteins_100g":
                return R.drawable.icon_protein;
            case "fiber_100g":
                return R.drawable.icon_fiber;
            default:
                return R.drawable.icon_scanner;
        }
    }

//    private int getNutrientColor(String nutrient, float value) {
//        boolean isPositive = value > 0;
//        switch (nutrient) {
//            case "energy_value":
//            case "sodium_value":
//            case "fat_value":
//            case "saturated-fat":
//            case "sugars":
//            case "salt":
//            case "carbohydrates":
//            case "proteins":
//            case "fiber":
//                return isPositive ? R.color.positive : R.color.negative;
//            default:
//                return R.color.neutral;
//        }
//    }

    private String getScoreDescription(int score) {
        if (score <= 30) return "Bad";
        if (score <= 50) return "Poor";
        if (score <= 72) return "Good";
        return "Excellent";
    }

    private int evaluateScore() {
        int score = 0;
        if (additivesList.isEmpty()) {
            score += 30;
        } else {
            for (AdditiveItem additive : additivesList) {
                if (new AdditiveDatabase(this).getDangerList().containsKey(additive.getCode())) {
                    score = -15;
                    break;
                }
                else{
                    score = 15;
                }
            }
        }

        score += sodiumScore;
        score += energyScore;
        score += fiberScore;
        score += proteinScore;
        score += sugarscore;
        score += saltscore;
        score += fatscore;

        if(score <= 0){
            score = 0;
        }
        return score;
    }

    private void showShimmer() {
        runOnUiThread(() -> {
            shimmerFrameLayout.setVisibility(View.VISIBLE);
            shimmerFrameLayout.startShimmer();
            noshimmer.setVisibility(View.GONE);
            bottomSheetBehavior.setDraggable(false);
        });
    }

    private void hideShimmer() {
        runOnUiThread(() -> {
            shimmerFrameLayout.stopShimmer();
            shimmerFrameLayout.setVisibility(View.GONE);
            noshimmer.setVisibility(View.VISIBLE);
            bottomSheetBehavior.setDraggable(true);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCodeScanner.releaseResources();
        mCodeScanner.startPreview();
    }

    @Override
    protected void onPause() {
        mCodeScanner.releaseResources();
        super.onPause();
    }


    /// Everything down here is used to save data on for Firebase


}

package com.example.nutri_scan.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nutri_scan.R;
import com.example.nutri_scan.adapter.RecommendationAdapter;
import com.example.nutri_scan.data.Product;
import com.example.nutri_scan.databinding.ActivityRecommendationBinding;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Recommendation extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ShimmerFrameLayout shimmerLayout;
    private RecommendationAdapter adapter;
    private DatabaseReference database;
    private EditText searchEditText;
    private static final int BARCODE_SCANNER_REQUEST_CODE = 1001;
    private String lastScannedBarcode = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recommendation);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.status_bar_color_green));

        ImageButton scannerButton = findViewById(R.id.icon_scanner);
        scannerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Recommendation.this, BarcodeScannerRecommendation.class);
                startActivityForResult(intent, BARCODE_SCANNER_REQUEST_CODE);
            }
        });

        // Initialize views
        recyclerView = findViewById(R.id.recommendations_recycler_view);
        shimmerLayout = findViewById(R.id.shimmer_layout);
        searchEditText = findViewById(R.id.search_edit_text);

        // Initialize Firebase
        database = FirebaseDatabase.getInstance().getReference();

        // Initialize RecyclerView and adapter
        adapter = new RecommendationAdapter(this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        setupSearch();

        // Start shimmer effect
        shimmerLayout.startShimmer();

        // Fetch initial data
        fetchRecommendations();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BARCODE_SCANNER_REQUEST_CODE && resultCode == RESULT_OK) {
            String barcodeResult = data.getStringExtra("BARCODE_RESULT");
            if (barcodeResult != null && !barcodeResult.isEmpty()) {
                lastScannedBarcode = barcodeResult;
                fetchProductAndSimilarItems(barcodeResult);
            }
        }
    }

    private void handleNoProducts() {
        // Stop shimmer effect
        shimmerLayout.stopShimmer();
        shimmerLayout.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);

        // Show message to user
        Toast.makeText(this,
                "No product found with this barcode",
                Toast.LENGTH_SHORT).show();

        // Clear the adapter
        adapter.updateProducts(new ArrayList<>());
    }

    private void handleDatabaseError(DatabaseError error) {
        // Stop shimmer effect
        shimmerLayout.stopShimmer();
        shimmerLayout.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);

        // Show error message
        Toast.makeText(this,
                "Error loading products: " + error.getMessage(),
                Toast.LENGTH_SHORT).show();

        // Log the error for debugging
        Log.e("RecommendationActivity",
                "Database error: " + error.getMessage(),
                error.toException());
    }

    private void fetchProductAndSimilarItems(String barcode) {
        shimmerLayout.setVisibility(View.VISIBLE);
        shimmerLayout.startShimmer();
        recyclerView.setVisibility(View.GONE);

        database.child("products").child(barcode).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    try {
                        Product scannedProduct = snapshot.getValue(Product.class);
                        if (scannedProduct != null) {
                            scannedProduct.setBarcode(barcode);
                            scannedProduct.setDisplayType(Product.ProductDisplayType.SCANNED);
                            fetchSimilarProducts(scannedProduct);
                        } else {
                            handleNoProducts();
                        }
                    } catch (Exception e) {
                        Log.e("RecommendationActivity", "Error parsing product: " + e.getMessage());
                        handleNoProducts();
                    }
                } else {
                    handleNoProducts();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                handleDatabaseError(error);
            }
        });
    }

    private void fetchSimilarProducts(Product scannedProduct) {
        database.child("products").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Product> allProducts = new ArrayList<>();
                allProducts.add(scannedProduct); // Add scanned product first

                Map<String, Double> similarProducts = new HashMap<>();

                // Extract categories and keywords from scanned product
                Set<String> scannedCategories = new HashSet<>(
                        Arrays.asList(scannedProduct.getProduct_Category().toLowerCase().split("\\s*,\\s*"))
                );

                List<String> scannedKeywords = new ArrayList<>();
                // Add product name words
                scannedKeywords.addAll(Arrays.asList(
                        scannedProduct.getProduct_Name().toLowerCase().replaceAll("[^a-zA-Z0-9\\s]", "").split("\\s+")
                ));

                // Add numbered keywords
                for (DataSnapshot keywordSnapshot : snapshot.child(scannedProduct.getBarcode()).getChildren()) {
                    String key = keywordSnapshot.getKey();
                    if (key != null && key.matches("\\d+")) {
                        String keyword = keywordSnapshot.getValue(String.class);
                        if (keyword != null) {
                            scannedKeywords.add(keyword.toLowerCase());
                        }
                    }
                }

                // Find similar products
                for (DataSnapshot productSnapshot : snapshot.getChildren()) {
                    String currentBarcode = productSnapshot.getKey();
                    if (!currentBarcode.equals(scannedProduct.getBarcode())) {
                        try {
                            Product product = productSnapshot.getValue(Product.class);
                            if (product != null) {
                                product.setBarcode(currentBarcode);

                                // Calculate similarity
                                int matchingCategories = 0;
                                int matchingKeywords = 0;

                                // Check categories
                                Set<String> productCategories = new HashSet<>(
                                        Arrays.asList(product.getProduct_Category().toLowerCase().split("\\s*,\\s*"))
                                );
                                for (String category : scannedCategories) {
                                    if (productCategories.contains(category)) {
                                        matchingCategories++;
                                    }
                                }

                                // Check keywords (including numbered keywords)
                                List<String> productKeywords = new ArrayList<>();
                                productKeywords.addAll(Arrays.asList(
                                        product.getProduct_Name().toLowerCase().replaceAll("[^a-zA-Z0-9\\s]", "").split("\\s+")
                                ));

                                // Add numbered keywords for the current product
                                for (DataSnapshot keywordSnapshot : productSnapshot.getChildren()) {
                                    String key = keywordSnapshot.getKey();
                                    if (key != null && key.matches("\\d+")) {
                                        String keyword = keywordSnapshot.getValue(String.class);
                                        if (keyword != null) {
                                            productKeywords.add(keyword.toLowerCase());
                                        }
                                    }
                                }

                                for (String keyword : scannedKeywords) {
                                    if (productKeywords.contains(keyword)) {
                                        matchingKeywords++;
                                    }
                                }

                                // Calculate similarity score (50% categories, 50% keywords)
                                double categoryScore = scannedCategories.isEmpty() ? 0 :
                                        (double) matchingCategories / scannedCategories.size();
                                double keywordScore = scannedKeywords.isEmpty() ? 0 :
                                        (double) matchingKeywords / scannedKeywords.size();
                                double similarityScore = (categoryScore * 0.5 + keywordScore * 0.5);

                                if (similarityScore > 0.3) { // Only include products with >30% similarity
                                    similarProducts.put(currentBarcode, similarityScore);
                                }
                            }
                        } catch (Exception e) {
                            Log.e("RecommendationActivity", "Error parsing similar product: " + e.getMessage());
                        }
                    }
                }

                // Sort similar products by similarity score
                List<Map.Entry<String, Double>> sortedProducts = new ArrayList<>(similarProducts.entrySet());
                Collections.sort(sortedProducts, (e1, e2) -> e2.getValue().compareTo(e1.getValue()));

                // Add similar products to the list
                for (Map.Entry<String, Double> entry : sortedProducts) {
                    Product product = snapshot.child(entry.getKey()).getValue(Product.class);
                    if (product != null) {
                        product.setBarcode(entry.getKey());
                        product.setDisplayType(Product.ProductDisplayType.ALTERNATIVE);
                        allProducts.add(product);
                    }
                }

                // Stop shimmer and show RecyclerView
                shimmerLayout.stopShimmer();
                shimmerLayout.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);

                adapter.updateProducts(allProducts);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                handleDatabaseError(error);
            }
        });
    }


    private void fetchRecommendations() {
        database.child("products").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Product> productsList = new ArrayList<>();

                for (DataSnapshot productSnapshot : snapshot.getChildren()) {
                    String barcode = productSnapshot.getKey();
                    try {
                        Product product = productSnapshot.getValue(Product.class);
                        if (product != null) {
                            product.setBarcode(barcode);
                            productsList.add(product);
                        }
                    } catch (Exception e) {
                        Log.e("RecommendationActivity", "Error parsing product: " + e.getMessage());
                    }
                }

                // Stop shimmer and show RecyclerView
                shimmerLayout.stopShimmer();
                shimmerLayout.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);

                adapter.updateProducts(productsList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Recommendation.this,
                        "Error loading recommendations: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resetView() {
        // Reset the last scanned barcode
        lastScannedBarcode = null;

        // Clear any existing search
        if (searchEditText != null) {
            searchEditText.setText("");
        }

        // Reset the adapter
        if (adapter != null) {
            adapter.updateProducts(new ArrayList<>());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Reset the last scanned barcode when activity is destroyed
        lastScannedBarcode = null;
    }

    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filterProducts(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }
}
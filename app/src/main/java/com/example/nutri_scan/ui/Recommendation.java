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

    private static final int TOP_N_RECOMMENDATIONS = 10;
    private static final double INITIAL_FILTER_THRESHOLD = 0.08; // Combined threshold for first pass
    private static final double FINAL_SIMILARITY_THRESHOLD = 0.20;


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

//    private void fetchSimilarProducts(Product scannedProduct) {
//        database.child("products").addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                List<Product> allProducts = new ArrayList<>();
//                allProducts.add(scannedProduct); // Add scanned product first
//
//                Map<String, Double> similarProducts = new HashMap<>();
//
//                // Extract categories and keywords from scanned product
//                Set<String> scannedCategories = new HashSet<>(
//                        Arrays.asList(scannedProduct.getProduct_Category().toLowerCase().split("\\s*,\\s*"))
//                );
//
//                List<String> scannedKeywords = new ArrayList<>();
//                // Add product name words
//                scannedKeywords.addAll(Arrays.asList(
//                        scannedProduct.getProduct_Name().toLowerCase().replaceAll("[^a-zA-Z0-9\\s]", "").split("\\s+")
//                ));
//
//                // Add numbered keywords
//                for (DataSnapshot keywordSnapshot : snapshot.child(scannedProduct.getBarcode()).getChildren()) {
//                    String key = keywordSnapshot.getKey();
//                    if (key != null && key.matches("\\d+")) {
//                        String keyword = keywordSnapshot.getValue(String.class);
//                        if (keyword != null) {
//                            scannedKeywords.add(keyword.toLowerCase());
//                        }
//                    }
//                }
//
//                // Find similar products
//                for (DataSnapshot productSnapshot : snapshot.getChildren()) {
//                    String currentBarcode = productSnapshot.getKey();
//                    if (!currentBarcode.equals(scannedProduct.getBarcode())) {
//                        try {
//                            Product product = productSnapshot.getValue(Product.class);
//                            if (product != null) {
//                                product.setBarcode(currentBarcode);
//
//                                // Calculate similarity
//                                int matchingCategories = 0;
//                                int matchingKeywords = 0;
//
//                                // Check categories
//                                Set<String> productCategories = new HashSet<>(
//                                        Arrays.asList(product.getProduct_Category().toLowerCase().split("\\s*,\\s*"))
//                                );
//                                for (String category : scannedCategories) {
//                                    if (productCategories.contains(category)) {
//                                        matchingCategories++;
//                                    }
//                                }
//
//                                // Check keywords (including numbered keywords)
//                                List<String> productKeywords = new ArrayList<>();
//                                productKeywords.addAll(Arrays.asList(
//                                        product.getProduct_Name().toLowerCase().replaceAll("[^a-zA-Z0-9\\s]", "").split("\\s+")
//                                ));
//
//                                // Add numbered keywords for the current product
//                                for (DataSnapshot keywordSnapshot : productSnapshot.getChildren()) {
//                                    String key = keywordSnapshot.getKey();
//                                    if (key != null && key.matches("\\d+")) {
//                                        String keyword = keywordSnapshot.getValue(String.class);
//                                        if (keyword != null) {
//                                            productKeywords.add(keyword.toLowerCase());
//                                        }
//                                    }
//                                }
//
//                                for (String keyword : scannedKeywords) {
//                                    if (productKeywords.contains(keyword)) {
//                                        matchingKeywords++;
//                                    }
//                                }
//
//                                // Calculate similarity score (50% categories, 50% keywords)
//                                double categoryScore = scannedCategories.isEmpty() ? 0 :
//                                        (double) matchingCategories / scannedCategories.size();
//                                double keywordScore = scannedKeywords.isEmpty() ? 0 :
//                                        (double) matchingKeywords / scannedKeywords.size();
//                                double similarityScore = (categoryScore * 0.5 + keywordScore * 0.5);
//
//                                if (similarityScore > 0.3) { // Only include products with >30% similarity
//                                    similarProducts.put(currentBarcode, similarityScore);
//                                }
//                            }
//                        } catch (Exception e) {
//                            Log.e("RecommendationActivity", "Error parsing similar product: " + e.getMessage());
//                        }
//                    }
//                }
//
//                // Sort similar products by similarity score
//                List<Map.Entry<String, Double>> sortedProducts = new ArrayList<>(similarProducts.entrySet());
//                Collections.sort(sortedProducts, (e1, e2) -> e2.getValue().compareTo(e1.getValue()));
//
//                // Add similar products to the list
//                for (Map.Entry<String, Double> entry : sortedProducts) {
//                    Product product = snapshot.child(entry.getKey()).getValue(Product.class);
//                    if (product != null) {
//                        product.setBarcode(entry.getKey());
//                        product.setDisplayType(Product.ProductDisplayType.ALTERNATIVE);
//                        allProducts.add(product);
//                    }
//                }
//
//                // Stop shimmer and show RecyclerView
//                shimmerLayout.stopShimmer();
//                shimmerLayout.setVisibility(View.GONE);
//                recyclerView.setVisibility(View.VISIBLE);
//
//                adapter.updateProducts(allProducts);
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                handleDatabaseError(error);
//            }
//        });
//    }


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


    private void fetchSimilarProducts(Product scannedProduct) {
        database.child("products").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Product> allProducts = new ArrayList<>();
                allProducts.add(scannedProduct);

                // Step 1: Extract scanned product features
                ProductFeatures scannedFeatures = extractProductFeatures(scannedProduct, snapshot.child(scannedProduct.getBarcode()));

                // First pass: Category and Keyword filtering
                Map<String, Product> filteredProducts = new HashMap<>();
                Map<String, Double> initialScores = new HashMap<>();

                for (DataSnapshot productSnapshot : snapshot.getChildren()) {
                    String currentBarcode = productSnapshot.getKey();
                    if (!currentBarcode.equals(scannedProduct.getBarcode())) {
                        try {
                            Product product = productSnapshot.getValue(Product.class);
                            if (product != null) {
                                product.setBarcode(currentBarcode);

                                // Extract features for comparison product
                                ProductFeatures compareFeatures = extractProductFeatures(product, productSnapshot);

                                // Calculate initial similarity score
                                double initialSimilarity = calculateInitialSimilarity(scannedFeatures, compareFeatures);

                                // Keep products above threshold
                                if (initialSimilarity >= INITIAL_FILTER_THRESHOLD) {
                                    filteredProducts.put(currentBarcode, product);
                                    initialScores.put(currentBarcode, initialSimilarity);
                                }
                            }
                        } catch (Exception e) {
                            Log.e("RecommendationActivity", "Error parsing product: " + e.getMessage());
                        }
                    }
                }

                // Step 2: Apply TF-IDF on filtered products
                if (!filteredProducts.isEmpty()) {
                    // Prepare documents for TF-IDF
                    List<String> documents = new ArrayList<>();
                    Map<String, String> productFeatures = new HashMap<>();

                    // Add scanned product
                    String scannedDoc = createProductDocument(scannedProduct, scannedFeatures);
                    productFeatures.put(scannedProduct.getBarcode(), scannedDoc);
                    documents.add(scannedDoc);

                    // Add filtered products
                    for (Map.Entry<String, Product> entry : filteredProducts.entrySet()) {
                        ProductFeatures features = extractProductFeatures(entry.getValue(),
                                snapshot.child(entry.getKey()));
                        String productDoc = createProductDocument(entry.getValue(), features);
                        productFeatures.put(entry.getKey(), productDoc);
                        documents.add(productDoc);
                    }

                    // Calculate TF-IDF vectors
                    Map<String, Map<String, Double>> tfidfVectors = calculateTFIDF(documents);

                    // Calculate final similarities
                    Map<String, Double> finalSimilarities = new HashMap<>();
                    Map<String, Double> scannedVector = tfidfVectors.get(productFeatures.get(scannedProduct.getBarcode()));

                    for (Map.Entry<String, Product> entry : filteredProducts.entrySet()) {
                        String barcode = entry.getKey();
                        Map<String, Double> compareVector = tfidfVectors.get(productFeatures.get(barcode));

                        // Calculate TF-IDF similarity
                        double tfidfSimilarity = calculateCosineSimilarity(scannedVector, compareVector);

                        // Combine with initial similarity score
                        double initialSimilarity = initialScores.get(barcode);
                        double finalSimilarity = (0.4 * initialSimilarity) + (0.6 * tfidfSimilarity);

                        if (finalSimilarity >= FINAL_SIMILARITY_THRESHOLD) {
                            finalSimilarities.put(barcode, finalSimilarity);
                        }
                    }

                    // Sort and get top recommendations
                    List<Map.Entry<String, Double>> sortedProducts = new ArrayList<>(finalSimilarities.entrySet());
                    Collections.sort(sortedProducts, (e1, e2) -> e2.getValue().compareTo(e1.getValue()));

                    List<Product> recommendedProducts = new ArrayList<>();
                    recommendedProducts.add(scannedProduct);

                    for (int i = 0; i < Math.min(TOP_N_RECOMMENDATIONS, sortedProducts.size()); i++) {
                        String barcode = sortedProducts.get(i).getKey();
                        Product product = filteredProducts.get(barcode);
                        if (product != null) {
                            product.setDisplayType(Product.ProductDisplayType.ALTERNATIVE);
                            recommendedProducts.add(product);
                        }
                    }

                    updateUI(recommendedProducts);
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

    private static class ProductFeatures {
        Set<String> categories;
        Set<String> keywords;
        String name;

        ProductFeatures(Set<String> categories, Set<String> keywords, String name) {
            this.categories = categories;
            this.keywords = keywords;
            this.name = name;
        }
    }

    private ProductFeatures extractProductFeatures(Product product, DataSnapshot snapshot) {
        // Extract categories
        Set<String> categories = new HashSet<>(
                Arrays.asList(product.getProduct_Category().toLowerCase().split("\\s*,\\s*"))
        );

        // Extract keywords
        Set<String> keywords = new HashSet<>();

        // Add product name words as keywords
        keywords.addAll(Arrays.asList(
                product.getProduct_Name().toLowerCase().replaceAll("[^a-zA-Z0-9\\s]", "").split("\\s+")
        ));

        // Add numbered keywords from database
        for (DataSnapshot keywordSnapshot : snapshot.getChildren()) {
            String key = keywordSnapshot.getKey();
            if (key != null && key.matches("\\d+")) {
                String keyword = keywordSnapshot.getValue(String.class);
                if (keyword != null) {
                    keywords.add(keyword.toLowerCase());
                }
            }
        }

        return new ProductFeatures(categories, keywords, product.getProduct_Name().toLowerCase());
    }

    private double calculateInitialSimilarity(ProductFeatures scanned, ProductFeatures compare) {
        // Calculate category similarity (Jaccard similarity)
        double categorySimilarity = calculateSetSimilarity(scanned.categories, compare.categories);

        // Calculate keyword similarity (Jaccard similarity)
        double keywordSimilarity = calculateSetSimilarity(scanned.keywords, compare.keywords);

//        // Calculate name similarity using Levenshtein distance
//        double nameSimilarity = calculateNameSimilarity(scanned.name, compare.name);

        // Weighted combination of similarities
        return (0.4 * categorySimilarity) + (0.4 * keywordSimilarity) ; //+ (0.2 * nameSimilarity)
    }

    private double calculateSetSimilarity(Set<String> set1, Set<String> set2) {
        if (set1.isEmpty() || set2.isEmpty()) return 0.0;

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        return (double) intersection.size() / union.size();
    }


    // only for more strictness
//    private double calculateNameSimilarity(String name1, String name2) {
//        if (name1.isEmpty() || name2.isEmpty()) return 0.0;
//
//        int distance = levenshteinDistance(name1, name2);
//        int maxLength = Math.max(name1.length(), name2.length());
//
//        return 1.0 - ((double) distance / maxLength);
//    }

//    private int levenshteinDistance(String s1, String s2) {
//        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
//
//        for (int i = 0; i <= s1.length(); i++) {
//            for (int j = 0; j <= s2.length(); j++) {
//                if (i == 0) {
//                    dp[i][j] = j;
//                } else if (j == 0) {
//                    dp[i][j] = i;
//                } else {
//                    dp[i][j] = Math.min(
//                            dp[i - 1][j - 1] + (s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1),
//                            Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1)
//                    );
//                }
//            }
//        }
//
//        return dp[s1.length()][s2.length()];
//    }

    private String createProductDocument(Product product, ProductFeatures features) {
        StringBuilder document = new StringBuilder();

        // Add product name with higher weight (repeat 3 times)
        document.append(features.name).append(" ")
                .append(features.name).append(" ")
                .append(features.name).append(" ");

        // Add categories
        features.categories.forEach(category ->
                document.append(category).append(" "));

        // Add keywords
        features.keywords.forEach(keyword ->
                document.append(keyword).append(" "));

        return document.toString();
    }

    private Map<String, Map<String, Double>> calculateTFIDF(List<String> documents) {
        // Step 1: Calculate term frequencies for each document
        Map<String, Map<String, Integer>> termFrequencies = new HashMap<>();
        Set<String> vocabulary = new HashSet<>();

        for (String doc : documents) {
            Map<String, Integer> frequencies = new HashMap<>();
            String[] terms = doc.split("\\s+");

            for (String term : terms) {
                frequencies.put(term, frequencies.getOrDefault(term, 0) + 1);
                vocabulary.add(term);
            }

            termFrequencies.put(doc, frequencies);
        }

        // Step 2: Calculate IDF for each term
        Map<String, Double> idf = new HashMap<>();
        int totalDocs = documents.size();

        for (String term : vocabulary) {
            int docCount = 0;
            for (String doc : documents) {
                if (termFrequencies.get(doc).containsKey(term)) {
                    docCount++;
                }
            }
            idf.put(term, Math.log((double) totalDocs / docCount));
        }

        // Step 3: Calculate TF-IDF vectors
        Map<String, Map<String, Double>> tfidfVectors = new HashMap<>();

        for (String doc : documents) {
            Map<String, Double> tfidf = new HashMap<>();
            Map<String, Integer> frequencies = termFrequencies.get(doc);

            for (String term : vocabulary) {
                double tf = (double) frequencies.getOrDefault(term, 0) / frequencies.size();
                tfidf.put(term, tf * idf.get(term));
            }

            tfidfVectors.put(doc, tfidf);
        }

        return tfidfVectors;
    }

    private double calculateCosineSimilarity(Map<String, Double> vector1, Map<String, Double> vector2) {
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (String term : vector1.keySet()) {
            double v1 = vector1.get(term);
            double v2 = vector2.getOrDefault(term, 0.0);

            dotProduct += v1 * v2;
            norm1 += v1 * v1;
        }

        for (double value : vector2.values()) {
            norm2 += value * value;
        }

        if (norm1 == 0.0 || norm2 == 0.0) return 0.0;

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }


    private void updateUI(List<Product> products) {
        // Stop shimmer animation
        shimmerLayout.stopShimmer();
        shimmerLayout.setVisibility(View.GONE);

        // Show RecyclerView
        recyclerView.setVisibility(View.VISIBLE);

        // Update adapter with new products
        adapter.updateProducts(products);
    }

//    private void resetView() {
//        // Reset the last scanned barcode
//        lastScannedBarcode = null;
//
//        // Clear any existing search
//        if (searchEditText != null) {
//            searchEditText.setText("");
//        }
//
//        // Reset the adapter
//        if (adapter != null) {
//            adapter.updateProducts(new ArrayList<>());
//        }
//    }

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
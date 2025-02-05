package com.example.nutri_scan.ui;

import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.nutri_scan.R;
import com.example.nutri_scan.adapter.AttributesAdapter;
import com.example.nutri_scan.data.Product;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;
import java.util.Locale;
import java.util.Map;
public class ProductDetailActivity extends AppCompatActivity {
    private DatabaseReference database;
    private ImageView productImage;
    private TextView nameText, brandText, scoreText, scoreDescription;
    private ImageView scoreIcon;
    private ShimmerFrameLayout shimmerLayout;
    private LinearLayout noShimmerLayout;
    private View divider;
    private RecyclerView positivesRecyclerView;
    private TextView additivesList;
    private LinearLayout hypertensionLayout, diabetesLayout, cholesterolLayout, noooneLayout;
    private TextView ingredientsList;
    private LinearLayout ingredientsCardView;
    private ImageView ingredientsExpandIcon;
    private ImageView additivesExpandIcon;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.white));

        initializeViews();

        String barcode = getIntent().getStringExtra("barcode");
        if (barcode != null) {
            database = FirebaseDatabase.getInstance().getReference()
                    .child("products").child(barcode);
            loadProductDetails(barcode);
        }
    }

    private void initializeViews() {
        productImage = findViewById(R.id.product_image_recommendation);
        nameText = findViewById(R.id.name_recommendation);
        brandText = findViewById(R.id.brand_recommendation);
        scoreText = findViewById(R.id.score_recommendation);
        scoreDescription = findViewById(R.id.score_description_recommendation);
        scoreIcon = findViewById(R.id.score_icon_recommendation);
        shimmerLayout = findViewById(R.id.shimmer_recommendation);
        noShimmerLayout = findViewById(R.id.no_shimmer_recommendation);
        divider = findViewById(R.id.divider_recommendation);
        positivesRecyclerView = findViewById(R.id.positives_recycler_view_recommendation);
        additivesList = findViewById(R.id.additives_list_recommendation);

        ingredientsList = findViewById(R.id.ingredients_list_recommendation);

        ingredientsExpandIcon = findViewById(R.id.ingredients_expand_icon);
        additivesExpandIcon = findViewById(R.id.additives_expand_icon);

        // Add references for Unsuitable For section
        noooneLayout = findViewById(R.id.Noone_layout);
        hypertensionLayout = findViewById(R.id.hypertension_layout);
        diabetesLayout = findViewById(R.id.diabetes_layout);
        cholesterolLayout = findViewById(R.id.cholesterol_layout);

        positivesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        setupExpandCollapse();

    }

    private void displayProductDetails(Product product) {
        // Stop shimmer and show content
        shimmerLayout.stopShimmer();
        shimmerLayout.setVisibility(View.GONE);
        noShimmerLayout.setVisibility(View.VISIBLE);
        divider.setVisibility(View.VISIBLE);

        // Set basic product information
        nameText.setText(product.getProduct_Name());
        brandText.setText(product.getProduct_Brand());
        scoreText.setText(String.format(Locale.getDefault(), "%d", (int) product.getScore()));
        scoreDescription.setText(product.getRecommendation());

        setScoreIcon(product.getScore());

        // Load product image
        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(product.getImageUrl())
                    .placeholder(R.drawable.recommendation_placeholder_image)
                    .error(R.drawable.recommendation_placeholder_image)
                    .into(productImage);
        } else {
            productImage.setImageResource(R.drawable.recommendation_placeholder_image);
        }

        // Set up nutrition RecyclerView
        setupAttributesRecyclerView(positivesRecyclerView, product.getNutrition());

        // Check unsuitable conditions
        checkUnsuitableConditions(product.getNutrition());

        // Fetch and display additives
        if (product.getAdditives() != null && !product.getAdditives().isEmpty()) {
            String additives = String.join(", ", product.getAdditives());
            additivesList.setText(additives);
            // Initially hide additives list
            additivesList.setVisibility(View.GONE);
            additivesExpandIcon.setImageResource(R.drawable.icon_down_scanner);
        } else {
            additivesList.setText("No additives found.");
            // Optionally, hide the expand icon if no additives
            additivesExpandIcon.setVisibility(View.GONE);
        }

        // Display ingredients with initial collapse
        displayIngredients(product);

    }

    private void checkUnsuitableConditions(Map<String, String> nutrition) {
        // Parse nutrition values
        double sodium = parseNutritionValue(nutrition, "sodium_value");
        double salt = parseNutritionValue(nutrition, "salt");
        double sugars = parseNutritionValue(nutrition, "sugars");
        double saturatedFat = parseNutritionValue(nutrition, "saturated-fat");

        // Check conditions and update visibility
        if (sodium >= 252 && salt >= 1.62) {
            noooneLayout.setVisibility(View.GONE);
            hypertensionLayout.setVisibility(View.VISIBLE);
        }
        if (sugars >= 12.6) {
            noooneLayout.setVisibility(View.GONE);
            diabetesLayout.setVisibility(View.VISIBLE);
        }
        if (saturatedFat >= 2.8) {
            noooneLayout.setVisibility(View.GONE);
            cholesterolLayout.setVisibility(View.VISIBLE);
        }
    }

    private double parseNutritionValue(Map<String, String> nutrition, String key) {
        if (nutrition.containsKey(key)) {
            try {
                // Remove units (e.g., "g", "mg") and parse the value
                String value = nutrition.get(key).replaceAll("[^0-9.]", "");
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
    }

    private void loadProductDetails(String barcode) {
        shimmerLayout.startShimmer();
        shimmerLayout.setVisibility(View.VISIBLE);
        noShimmerLayout.setVisibility(View.GONE);
        divider.setVisibility(View.GONE);

        database.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                Product product = task.getResult().getValue(Product.class);
                if (product != null) {
                    displayProductDetails(product);
                }
            } else {
                shimmerLayout.stopShimmer();
                shimmerLayout.setVisibility(View.GONE);
                noShimmerLayout.setVisibility(View.VISIBLE);
                // Optionally, show a message that product details couldn't be loaded
            }
        }).addOnFailureListener(e -> {
            shimmerLayout.stopShimmer();
            shimmerLayout.setVisibility(View.GONE);
            noShimmerLayout.setVisibility(View.VISIBLE);
            // Optionally, handle the error (e.g., show a Toast or Log the error)
        });
    }


        private void setScoreIcon(double score) {
        int iconResource;
        int iconTint;

        if (score >= 65) {
            iconResource = R.drawable.icon_thumbs_up; // You'll need to add these drawables
            iconTint = ContextCompat.getColor(this, R.color.green);
        } else if (score >= 45) {
            iconResource = R.drawable.icon_thumbs_up;
            iconTint = ContextCompat.getColor(this, R.color.yellow);
        } else {
            iconResource = R.drawable.icon_thumb_down;
            iconTint = ContextCompat.getColor(this, R.color.red);
        }

        scoreIcon.setImageResource(iconResource);
        scoreIcon.setColorFilter(iconTint);
    }


    // Overloaded method for handling Map<String, String>
    private void setupAttributesRecyclerView(RecyclerView recyclerView, Map<String, String> nutrition) {
        AttributesAdapter adapter = new AttributesAdapter(nutrition);
        recyclerView.setAdapter(adapter);
    }

    private void displayIngredients(Product product) {
        // Check if ingredients exist and are not empty
        if (product.getIngredients() != null && !product.getIngredients().isEmpty()) {
            // Join ingredients with comma and show in the TextView
            String ingredientsText = String.join(", ", product.getIngredients());
            ingredientsList.setText(ingredientsText);

            // Initially hide ingredients list
            ingredientsList.setVisibility(View.GONE);
            ingredientsExpandIcon.setImageResource(R.drawable.icon_down_scanner);

            // Make sure the ingredients section is visible
            if (ingredientsCardView != null) {
                ingredientsCardView.setVisibility(View.VISIBLE);
            }
        } else {
            // Hide ingredients section if no ingredients
            ingredientsList.setText("No ingredients found.");
            ingredientsExpandIcon.setVisibility(View.GONE);

            if (ingredientsCardView != null) {
                ingredientsCardView.setVisibility(View.GONE);
            }
        }
    }

    private void setupExpandCollapse() {
        // Ingredients expand/collapse
        ingredientsExpandIcon.setOnClickListener(v -> {
            if (ingredientsList.getVisibility() == View.GONE) {
                // Expand ingredients
                ingredientsList.setVisibility(View.VISIBLE);
                ingredientsExpandIcon.setImageResource(R.drawable.icon__up_scanner);
            } else {
                // Collapse ingredients
                ingredientsList.setVisibility(View.GONE);
                ingredientsExpandIcon.setImageResource(R.drawable.icon_down_scanner);
            }
        });

        // Additives expand/collapse
        additivesExpandIcon.setOnClickListener(v -> {
            if (additivesList.getVisibility() == View.GONE) {
                // Expand additives
                additivesList.setVisibility(View.VISIBLE);
                additivesExpandIcon.setImageResource(R.drawable.icon__up_scanner);
            } else {
                // Collapse additives
                additivesList.setVisibility(View.GONE);
                additivesExpandIcon.setImageResource(R.drawable.icon_down_scanner);
            }
        });
    }

}







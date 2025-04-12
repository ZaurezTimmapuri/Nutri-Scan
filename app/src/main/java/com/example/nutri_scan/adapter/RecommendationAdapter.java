package com.example.nutri_scan.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.bumptech.glide.Glide;
import com.example.nutri_scan.R;
import com.example.nutri_scan.data.Product;
import com.example.nutri_scan.ui.ProductDetailActivity;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class RecommendationAdapter extends RecyclerView.Adapter<RecommendationAdapter.ViewHolder> {
    private List<Product> products;
    private Context context;
    private static List<Product> filteredProducts;
    private String currentSearchText = "";

    public RecommendationAdapter(Context context) {
        this.context = context;
        this.products = new ArrayList<>();
        this.filteredProducts = new ArrayList<>();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView productImage;
        public TextView productName;
        public ImageView productTypeIcon;
        public TextView productScore;
        public TextView productRecommendation;


        public ViewHolder(View view) {
            super(view);
            productImage = view.findViewById(R.id.product_image);
            productName = view.findViewById(R.id.product_name);
            productTypeIcon = view.findViewById(R.id.product_type_icon);
            productScore = view.findViewById(R.id.product_score);
            productRecommendation = view.findViewById(R.id.product_recommendation);

            view.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Product product = filteredProducts.get(position); // Use filteredProducts here
                    Intent intent = new Intent(context, ProductDetailActivity.class);
                    intent.putExtra("barcode", product.getBarcode());
                    context.startActivity(intent);
                }
            });
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recommendation_item, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = filteredProducts.get(position); // Use filteredProducts here

        holder.productName.setText(product.getProduct_Name());
        holder.productScore.setText(String.format(Locale.getDefault(), "%d", (int) product.getScore()));
        holder.productRecommendation.setText(product.getRecommendation());

        switch (product.getDisplayType()) {
            case SCANNED:
                holder.productTypeIcon.setImageResource(R.drawable.icon_scanned_product);
                holder.productTypeIcon.setVisibility(View.VISIBLE);
                break;
            case ALTERNATIVE:
                holder.productTypeIcon.setImageResource(R.drawable.icon_alternative_product);
                holder.productTypeIcon.setVisibility(View.VISIBLE);
                break;
            default:
                holder.productTypeIcon.setVisibility(View.GONE);
                break;
        }


        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(product.getImageUrl())
                    .placeholder(R.drawable.recommendation_placeholder_image)
                    .into(holder.productImage);
        } else {
            holder.productImage.setImageResource(R.drawable.recommendation_placeholder_image);
        }
    }

    @Override
    public int getItemCount() {
        return filteredProducts.size(); // Use filteredProducts here
    }

    public void updateProducts(List<Product> newProducts) {
        products.clear();
        // Sort the new products in descending order of score
        Collections.sort(newProducts, (p1, p2) -> Float.compare((float) p2.getScore(), (float) p1.getScore()));
        products.addAll(newProducts);
        filteredProducts.clear();
        filteredProducts.addAll(products); // Update filteredProducts with the new list
        notifyDataSetChanged();
    }

    public void filterProducts(String searchText) {
        currentSearchText = searchText.toLowerCase().trim();
        filteredProducts.clear();

        if (currentSearchText.isEmpty()) {
            filteredProducts.addAll(products); // Show all products if search text is empty
        } else {
            for (Product product : products) {
                if (product.getProduct_Name().toLowerCase().contains(currentSearchText)) {
                    filteredProducts.add(product); // Add products that match the search text
                } else if (product.getProduct_Brand().toLowerCase().contains(currentSearchText)){
                    filteredProducts.add(product);
                }
            }
        }
        notifyDataSetChanged(); // Notify the adapter of data changes
    }


}

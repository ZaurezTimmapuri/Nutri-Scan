package com.example.nutri_scan.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nutri_scan.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AttributesAdapter extends RecyclerView.Adapter<AttributesAdapter.ViewHolder> {
    private final List<String> items;
    private final List<String> keys;
    private final Map<String, String> nutritionMap;
    private final boolean isPositive;
    private final boolean isNutrition;

    // Constructor for attributes (List<String>)
    public AttributesAdapter(List<String> items, boolean isPositive) {
        this.items = items != null ? items : new ArrayList<>();
        this.keys = null;
        this.nutritionMap = null;
        this.isPositive = isPositive;
        this.isNutrition = false;
    }

    // Constructor for nutrition (Map<String, String>)
    public AttributesAdapter(Map<String, String> nutritionMap) {
        this.items = null;
        this.keys = nutritionMap != null ? new ArrayList<>(nutritionMap.keySet()) : new ArrayList<>();
        this.nutritionMap = nutritionMap;
        this.isPositive = true; // Nutrients are always considered positive
        this.isNutrition = true;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_attribute, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (isNutrition) {
            // Handle key-value pair for nutrition
            String key = keys.get(position);
            String value = nutritionMap.get(key);

            // Split the key at the underscore and take the first part
            String displayKey = key.split("_")[0];

            holder.textView.setText(String.format("%s: %s", displayKey, value));
            holder.iconView.setImageResource(R.drawable.icon_recommendation_right_arrow);
            holder.iconView.setColorFilter(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.black)
            );
        } else if (items != null) {
            // Handle additives or other attributes
            String item = items.get(position);
            holder.textView.setText(item);

            int iconRes = isPositive ? R.drawable.icon_recommendation_right_arrow : R.drawable.ic_close;
            int colorRes = isPositive ? R.color.black : R.color.red;

            holder.iconView.setImageResource(iconRes);
            holder.iconView.setColorFilter(
                    ContextCompat.getColor(holder.itemView.getContext(), colorRes)
            );
        }
    }

    @Override
    public int getItemCount() {
        return isNutrition ? keys.size() : items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView iconView;
        TextView textView;

        ViewHolder(View view) {
            super(view);
            iconView = view.findViewById(R.id.attribute_icon);
            textView = view.findViewById(R.id.attribute_text);
        }
    }
}
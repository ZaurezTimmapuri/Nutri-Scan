package com.example.nutri_scan.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nutri_scan.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NutritionAdapter extends RecyclerView.Adapter<NutritionAdapter.ViewHolder> {

    private final Map<String, String> nutritionMap;
    private final List<String> nutritionKeys;

    public NutritionAdapter(Map<String, String> nutrition) {
        this.nutritionMap = nutrition;
        this.nutritionKeys = new ArrayList<>(nutrition.keySet());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.nutrtion_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String key = nutritionKeys.get(position);
        String value = nutritionMap.get(key);

        holder.nutrientName.setText(key);
        holder.nutrientValue.setText(value);
    }

    @Override
    public int getItemCount() {
        return nutritionMap.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nutrientName, nutrientValue;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nutrientName = itemView.findViewById(R.id.nutrient_name);
            nutrientValue = itemView.findViewById(R.id.nutrient_value);
        }
    }
}

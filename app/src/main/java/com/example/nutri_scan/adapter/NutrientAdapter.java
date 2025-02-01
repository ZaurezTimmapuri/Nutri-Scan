package com.example.nutri_scan.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nutri_scan.ui.Additive;
import com.example.nutri_scan.data.AdditiveItem;
import com.example.nutri_scan.data.NutrientItem;
import com.example.nutri_scan.R;

import java.util.ArrayList;
import java.util.List;

public class NutrientAdapter extends RecyclerView.Adapter<NutrientAdapter.NutrientViewHolder> {

    private List<NutrientItem> nutrients;
    private Context context;
    private List<AdditiveItem> additivesList; // Changed to AdditiveItem list

    public NutrientAdapter(Context context, List<NutrientItem> nutrients, List<AdditiveItem> additivesList) {
        this.context = context;
        this.nutrients = nutrients;
        this.additivesList = additivesList; // Assign the additives list
    }

    public static class NutrientViewHolder extends RecyclerView.ViewHolder {
        public ImageView icon;
        public TextView name;
        public TextView description;
        public TextView value;
        public ImageButton expandIcon;
        public TextView additivesLink;

        public NutrientViewHolder(View view) {
            super(view);
            icon = view.findViewById(R.id.nutrientIcon);
            name = view.findViewById(R.id.nutrientName);
            description = view.findViewById(R.id.nutrientDescription);
            value = view.findViewById(R.id.nutrientValue);
            expandIcon = view.findViewById(R.id.expandIcon);
            additivesLink = view.findViewById(R.id.additivesLink);
        }
    }

    @NonNull
    @Override
    public NutrientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_nutrient, parent, false);
        return new NutrientViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NutrientViewHolder holder, int position) {
        NutrientItem nutrient = nutrients.get(position);
        Context context = holder.itemView.getContext();

        holder.icon.setImageResource(nutrient.getIcon());
        holder.name.setText(nutrient.getName());
        holder.description.setText(nutrient.getDescription());
        holder.value.setText(nutrient.getValue());
        holder.value.setTextColor(ContextCompat.getColor(context, nutrient.getColor()));

        if (nutrient.isAdditives()) {
            holder.expandIcon.setVisibility(View.VISIBLE);
            holder.expandIcon.setOnClickListener(v -> {
                if (holder.additivesLink.getVisibility() == View.VISIBLE) {
                    // Hide the link and change the icon to 'down'
                    holder.additivesLink.setVisibility(View.GONE);
                    holder.expandIcon.setImageResource(R.drawable.icon_down_scanner); // Change to 'down' icon
                } else {
                    // Show the link and change the icon to 'up'
                    holder.additivesLink.setVisibility(View.VISIBLE);
                    holder.expandIcon.setImageResource(R.drawable.icon__up_scanner); // Change to 'up' icon
                }
            });

            // Create an ArrayList<String> for the Enumber (code) of additives
            ArrayList<String> additivesCodes = new ArrayList<>();
            for (AdditiveItem additive : additivesList) {
                additivesCodes.add(additive.getCode()); // Add the Enumber of each additive
            }

            // Set up the link to open Additive class with the additives list
            holder.additivesLink.setOnClickListener(v -> {
                Intent intent = new Intent(context, Additive.class);
                intent.putStringArrayListExtra("additives", additivesCodes); // Pass the Enumber list
                context.startActivity(intent);
            });

        } else {
            holder.expandIcon.setVisibility(View.GONE);
            holder.additivesLink.setVisibility(View.GONE);
            holder.name.setTextColor(ContextCompat.getColor(context, android.R.color.black));
            holder.name.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return nutrients.size();
    }
}

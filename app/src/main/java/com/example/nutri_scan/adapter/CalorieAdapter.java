package com.example.nutri_scan.adapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nutri_scan.data.CalorieItem;
import com.example.nutri_scan.R;

import java.util.List;

public class CalorieAdapter extends RecyclerView.Adapter<CalorieAdapter.NutritionViewHolder> {
    private List<CalorieItem> CalorieItems;

    public CalorieAdapter(List<CalorieItem> CalorieItems) {
        this.CalorieItems = CalorieItems;
    }

    @NonNull
    @Override
    public NutritionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.calorie_card, parent, false);
        return new NutritionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NutritionViewHolder holder, int position) {
        CalorieItem item = CalorieItems.get(position);
        holder.nameTextView.setText(item.getName());
        holder.caloriesTextView.setText("Calories: " + item.getCalories() + " cal");
        holder.scoreTextView.setText(item.getScore() + "/100");
        holder.scoreDescription.setText(item.getDescription());
        if (item.getDescription() == "Bad"){
            holder.scoreIcon.setImageResource(R.drawable.icon_red_circle);
        } else if (item.getDescription()=="Poor") {
            holder.scoreIcon.setImageResource(R.drawable.icon_orange_circle);
        }else{
            holder.scoreIcon.setImageResource(R.drawable.icon_green_circle);
        }

        String info =
                "Sodium: " + item.getSodium() + "mg\n" +
                "Sugars: " + item.getSugar() + "g\n" +
                "Saturated Fats: " + item.getSaturatedFats() + "g\n" +
                "Carbohydrates: " + item.getCarbohydrates() + "g\n" +
                "Fiber: " + item.getFiber() + "g\n" +
                "Proteins: " + item.getProteins() + "g";

        holder.informationView.setText(info);
    }

    @Override
    public int getItemCount() {
        return CalorieItems.size();
    }

    static class NutritionViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView caloriesTextView;
        TextView informationView;
        TextView scoreTextView;
        TextView scoreDescription;
        ImageView scoreIcon;

        NutritionViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.itemName);
            caloriesTextView = itemView.findViewById(R.id.itemCalories);
            scoreTextView = itemView.findViewById(R.id.itemScore);
            scoreDescription = itemView.findViewById(R.id.scoreDescribe);
            informationView = itemView.findViewById(R.id.itemInfo);
            scoreIcon = itemView.findViewById(R.id.scoreIcon);
        }
    }
}
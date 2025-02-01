package com.example.nutri_scan.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nutri_scan.R;
import com.example.nutri_scan.data.SliderItem;

import java.util.ArrayList;
import java.util.List;

public class ImageSliderAdapter extends RecyclerView.Adapter<ImageSliderAdapter.SliderViewHolder> {

    private List<SliderItem> sliderItems;
    private Context context;

    public ImageSliderAdapter(Context context) {
        this.context = context;
        this.sliderItems = new ArrayList<>();

        // Add your slider items with descriptions
        sliderItems.add(new SliderItem(R.drawable.slider_scanner, "Discover nutritious meals with our scanning feature (Scanner)"));
        sliderItems.add(new SliderItem(R.drawable.slider_dieto, "Get answered your nutritional and fitness queries (Dieto)"));
        sliderItems.add(new SliderItem(R.drawable.slider_food_calories, "Get detailed nutritional information of food you consume (Fitpal)"));
        sliderItems.add(new SliderItem(R.drawable.slider_recommendation, "Explore healthy recommendations (Recom)"));
    }

    @NonNull
    @Override
    public SliderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.slider_item, parent, false);
        return new SliderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SliderViewHolder holder, int position) {
        SliderItem sliderItem = sliderItems.get(position);
        holder.imageView.setImageResource(sliderItem.getImage());
        holder.descriptionView.setText(sliderItem.getDescription());
    }

    @Override
    public int getItemCount() {
        return sliderItems.size();
    }

    public class SliderViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView descriptionView;

        public SliderViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.slider_image);
            descriptionView = itemView.findViewById(R.id.imageDescription);
        }
    }
}

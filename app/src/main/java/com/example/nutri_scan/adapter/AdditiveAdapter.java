package com.example.nutri_scan.adapter;

import static com.example.nutri_scan.R.color.negative;
import static com.example.nutri_scan.R.color.positive;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nutri_scan.data.AdditiveDatabase;
import com.example.nutri_scan.data.AdditiveItem;
import com.example.nutri_scan.data.DangerItem;
import com.example.nutri_scan.R;

import java.util.List;

public class AdditiveAdapter extends RecyclerView.Adapter<AdditiveAdapter.AdditiveViewHolder> {
    private List<AdditiveItem> additives;
    private AdditiveDatabase database;

    public AdditiveAdapter(List<AdditiveItem> additives, AdditiveDatabase database) {
        this.additives = additives;
        this.database = database;
    }

    @NonNull
    @Override
    public AdditiveViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_additive, parent, false);
        return new AdditiveViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdditiveViewHolder holder, int position) {
        AdditiveItem additive = additives.get(position);

        holder.codeTextView.setText(additive.getCode());
        holder.nameTextView.setText(additive.getName());
        holder.descriptionTextView.setText(additive.getDescription());
        holder.usageTextView.setText(additive.getUsage());
        holder.cautionTextView.setText("Caution");

        holder.causesLabel.setVisibility(View.GONE);
        holder.causesLabel.setVisibility(View.GONE);

        DangerItem dangerItem = database.getDangerInfo(additive.getCode());

        if (dangerItem != null) {
            holder.riskLevelTextView.setText("High Risk");
            holder.riskLevelTextView.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), negative));
            holder.causesLabel.setVisibility(View.GONE);
            holder.causesContainer.setVisibility(View.GONE);

            // Enable the expand button for high-risk items
            holder.expandIcon.setEnabled(true);
            holder.expandIcon.setAlpha(1.0f);  // Set to fully visible

            // Show causes if present
            holder.causeImage3.setVisibility(!dangerItem.getHyperactivity().isEmpty() ? View.VISIBLE : View.GONE);
            holder.causeLabel3.setVisibility(!dangerItem.getHyperactivity().isEmpty() ? View.VISIBLE : View.GONE);

            holder.causeImage2.setVisibility(!dangerItem.getAsthma().isEmpty() ? View.VISIBLE : View.GONE);
            holder.causeLabel2.setVisibility(!dangerItem.getAsthma().isEmpty() ? View.VISIBLE : View.GONE);

            holder.causeImage1.setVisibility(!dangerItem.getCancer().isEmpty() ? View.VISIBLE : View.GONE);
            holder.causeLabel1.setVisibility(!dangerItem.getCancer().isEmpty() ? View.VISIBLE : View.GONE);
        } else {
            // This means the additive is not in the danger list, hence set to "No Risk"
            holder.riskLevelTextView.setText("No Risk");
            holder.riskLevelTextView.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), positive));            holder.causesLabel.setVisibility(View.GONE);  // Hide causes label if not applicable
            holder.causesContainer.setVisibility(View.GONE);  // Hide causes container

            // Disable the expand button for no-risk items
            holder.expandIcon.setEnabled(false);
            holder.expandIcon.setAlpha(0.5f);  // Set to semi-transparent to indicate it's disabled
        }

        // Set up the expand icon click listener, only works if enabled
        holder.expandIcon.setOnClickListener(v -> {
            if (holder.expandIcon.isEnabled()) {
                boolean isExpanded = holder.causesLabel.getVisibility() == View.VISIBLE;
                holder.causesLabel.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
                holder.causesContainer.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
                holder.expandIcon.setImageResource(isExpanded ?
                        R.drawable.icon_down_scanner : R.drawable.icon__up_scanner);
            }
        });
    }

    @Override
    public int getItemCount() {
        return additives.size();
    }

    static class AdditiveViewHolder extends RecyclerView.ViewHolder {
        TextView codeTextView, nameTextView, descriptionTextView, usageTextView;
        TextView cautionTextView, riskLevelTextView, causesLabel;
        ImageButton expandIcon;
        LinearLayout causesContainer;
        ImageView causeImage1, causeImage2, causeImage3;  // Icons for cancer, asthma, hyperactivity
        TextView causeLabel1, causeLabel2, causeLabel3;   // Labels for each cause

        AdditiveViewHolder(View itemView) {
            super(itemView);
            codeTextView = itemView.findViewById(R.id.additive_code);
            nameTextView = itemView.findViewById(R.id.additive_name);
            descriptionTextView = itemView.findViewById(R.id.additive_description);
            usageTextView = itemView.findViewById(R.id.additive_usage);
            cautionTextView = itemView.findViewById(R.id.caution);
            riskLevelTextView = itemView.findViewById(R.id.risk_level);
            causesLabel = itemView.findViewById(R.id.causes_label);
            expandIcon = itemView.findViewById(R.id.expandIcon);
            causesContainer = itemView.findViewById(R.id.causes_container);

            // Get the cause icons and labels
            causeImage1 = itemView.findViewById(R.id.cause_image1);  // Cancer icon
            causeImage2 = itemView.findViewById(R.id.cause_image2);  // Asthma icon
            causeImage3 = itemView.findViewById(R.id.cause_image3);  // Hyperactivity icon

            causeLabel1 = itemView.findViewById(R.id.cause_label1);  // Cancer label
            causeLabel2 = itemView.findViewById(R.id.cause_label2);  // Asthma label
            causeLabel3 = itemView.findViewById(R.id.cause_label3);  // Hyperactivity label
        }
    }
}

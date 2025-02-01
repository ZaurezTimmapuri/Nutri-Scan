package com.example.nutri_scan.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nutri_scan.R;
import com.example.nutri_scan.data.FunctionalCard;

import java.util.ArrayList;
import java.util.List;

public class FunctionalCardsAdapter extends RecyclerView.Adapter<FunctionalCardsAdapter.CardViewHolder> {
    private List<FunctionalCard> cards;
    private Context context;

    public FunctionalCardsAdapter(Context context) {
        this.context = context;
        initializeCards();
    }

    private void initializeCards() {
        cards = new ArrayList<>();
        cards.add(new FunctionalCard(
                R.color.green,
                "BMI Calculator",
                "Calculate your Body Mass Index",
                R.drawable.icon_bmi,
                FunctionalCard.CardType.BMI_CALCULATOR
        ));
        cards.add(new FunctionalCard(
                R.color.green,
                "Calorie Calculator",
                "Find your daily caloric needs",
                R.drawable.icon_calorie_requirement,
                FunctionalCard.CardType.CALORIE_CALCULATOR
        ));
        cards.add(new FunctionalCard(
                R.color.green,
                "Water Intake",
                "Calculate your daily water needs",
                R.drawable.icon_water_intake,
                FunctionalCard.CardType.WATER_INTAKE
        ));
        cards.add(new FunctionalCard(
                R.color.green,
                "Macro Calculator",
                "Get your macro nutrient split",
                R.drawable.icon_macros,
                FunctionalCard.CardType.MACRO_CALCULATOR
        ));
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.functional_card_item, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        FunctionalCard card = cards.get(position);
        holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, card.getBackgroundColor()));
        holder.titleText.setText(card.getTitle());
        holder.descriptionText.setText(card.getDescription());
        holder.iconImage.setImageResource(card.getIcon());

        holder.cardView.setOnClickListener(v -> {
            switch (card.getType()) {
                case BMI_CALCULATOR:
                    showBMICalculator();
                    break;
                case CALORIE_CALCULATOR:
                    showCalorieCalculator();
                    break;
                case WATER_INTAKE:
                    showWaterIntakeCalculator();
                    break;
                case MACRO_CALCULATOR:
                    showMacroCalculator();
                    break;
            }
        });
    }

    @Override
    public int getItemCount() {
        return cards.size();
    }

    static class CardViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView iconImage;
        TextView titleText;
        TextView descriptionText;

        CardViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            iconImage = itemView.findViewById(R.id.iconImage);
            titleText = itemView.findViewById(R.id.titleText);
            descriptionText = itemView.findViewById(R.id.descriptionText);
        }
    }

    // Calculator Dialog Methods
    private void showBMICalculator() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_bmi_calculator, null);

        EditText heightInput = view.findViewById(R.id.heightInput);
        EditText weightInput = view.findViewById(R.id.weightInput);
        Button calculateButton = view.findViewById(R.id.calculateButton);
        TextView resultText = view.findViewById(R.id.resultText);

        calculateButton.setOnClickListener(v -> {
            float height = Float.parseFloat(heightInput.getText().toString()) / 100; // cm to m
            float weight = Float.parseFloat(weightInput.getText().toString());
            float bmi = weight / (height * height);
            resultText.setText(String.format("Your BMI: %.1f", bmi));
        });

        builder.setView(view);
        builder.show();
    }

    private void showCalorieCalculator() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_calorie_calculator, null);

        // Initialize views
        RadioGroup genderRadioGroup = view.findViewById(R.id.genderRadioGroup);
        EditText ageInput = view.findViewById(R.id.ageInput);
        EditText heightInput = view.findViewById(R.id.heightInputCalorie);
        EditText weightInput = view.findViewById(R.id.weightInputCalorie);
        Spinner activityLevelSpinner = view.findViewById(R.id.activityLevelSpinner);
        Button calculateButton = view.findViewById(R.id.calculateCalorieButton);
        TextView resultText = view.findViewById(R.id.calorieResultText);
        TextView breakdownText = view.findViewById(R.id.calorieBreakdownText);

        // Setup activity level spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item,
                new String[]{
                        "Sedentary (little or no exercise)",
                        "Lightly active (light exercise/sports 1-3 days/week)",
                        "Moderately active (moderate exercise/sports 3-5 days/week)",
                        "Very active (hard exercise/sports 6-7 days/week)",
                        "Extra active (very hard exercise/sports & physical job)"
                });
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        activityLevelSpinner.setAdapter(adapter);

        calculateButton.setOnClickListener(v -> {
            try {
                // Get inputs
                boolean isMale = genderRadioGroup.getCheckedRadioButtonId() == R.id.maleRadio;
                int age = Integer.parseInt(ageInput.getText().toString());
                float height = Float.parseFloat(heightInput.getText().toString());
                float weight = Float.parseFloat(weightInput.getText().toString());
                int activityLevel = activityLevelSpinner.getSelectedItemPosition();

                // Calculate BMR using Mifflin-St Jeor Equation
                double bmr;
                if (isMale) {
                    bmr = (10 * weight) + (6.25 * height) - (5 * age) + 5;
                } else {
                    bmr = (10 * weight) + (6.25 * height) - (5 * age) - 161;
                }

                // Apply activity multiplier
                double[] activityMultipliers = {1.2, 1.375, 1.55, 1.725, 1.9};
                double tdee = bmr * activityMultipliers[activityLevel];

                // Calculate different calorie targets
                double weightLoss = tdee - 500;    // 500 calorie deficit for weight loss
                double weightGain = tdee + 500;    // 500 calorie surplus for weight gain

                // Display results
                resultText.setText(String.format("Your Daily Calorie Needs: %.0f calories", tdee));
                breakdownText.setText(String.format(
                        "For weight loss: %.0f calories/day\n" +
                                "For maintenance: %.0f calories/day\n" +
                                "For weight gain: %.0f calories/day",
                        weightLoss, tdee, weightGain));

            } catch (NumberFormatException e) {
                Toast.makeText(context, "Please fill all fields correctly", Toast.LENGTH_SHORT).show();
            }
        });

        AlertDialog dialog = builder.setView(view).create();
        dialog.show();
    }

    private void showMacroCalculator() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_macro_calculator, null);

        // Initialize views
        Spinner goalSpinner = view.findViewById(R.id.goalSpinner);
        EditText caloriesInput = view.findViewById(R.id.caloriesInput);
        Button calculateButton = view.findViewById(R.id.calculateMacrosButton);
        TextView macroResultText = view.findViewById(R.id.macroResultText);
        TextView proteinText = view.findViewById(R.id.proteinText);
        TextView carbsText = view.findViewById(R.id.carbsText);
        TextView fatsText = view.findViewById(R.id.fatsText);

        // Setup goal spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item,
                new String[]{
                        "Weight Loss",
                        "Maintenance",
                        "Muscle Gain"
                });
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        goalSpinner.setAdapter(adapter);

        calculateButton.setOnClickListener(v -> {
            try {
                int calories = Integer.parseInt(caloriesInput.getText().toString());
                int goalIndex = goalSpinner.getSelectedItemPosition();

                // Macro ratios based on goal (Protein/Carbs/Fats)
                double[][] ratios = {
                        {0.40, 0.35, 0.25}, // Weight Loss
                        {0.30, 0.45, 0.25}, // Maintenance
                        {0.30, 0.50, 0.20}  // Muscle Gain
                };

                // Calculate macros in grams
                double protein = (calories * ratios[goalIndex][0]) / 4; // 4 calories per gram of protein
                double carbs = (calories * ratios[goalIndex][1]) / 4;   // 4 calories per gram of carbs
                double fats = (calories * ratios[goalIndex][2]) / 9;    // 9 calories per gram of fat

                macroResultText.setText("Your Daily Macro Split");
                proteinText.setText(String.format("Protein: %.0fg (%.0f%%)", protein, ratios[goalIndex][0] * 100));
                carbsText.setText(String.format("Carbs: %.0fg (%.0f%%)", carbs, ratios[goalIndex][1] * 100));
                fatsText.setText(String.format("Fats: %.0fg (%.0f%%)", fats, ratios[goalIndex][2] * 100));

            } catch (NumberFormatException e) {
                Toast.makeText(context, "Please enter valid calories", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setView(view);
        builder.show();
    }

    private void showWaterIntakeCalculator() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_water_intake_calculator, null);

        // Initialize views
        EditText weightInput = view.findViewById(R.id.weightInputWater);
        Spinner activityLevelSpinner = view.findViewById(R.id.activityLevelSpinnerWater);
        Spinner climateSpinner = view.findViewById(R.id.climateSpinner);
        Button calculateButton = view.findViewById(R.id.calculateWaterButton);
        TextView resultText = view.findViewById(R.id.waterResultText);
        TextView tipsText = view.findViewById(R.id.waterTipsText);

        // Setup activity level spinner
        ArrayAdapter<String> activityAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item,
                new String[]{
                        "Sedentary",
                        "Moderately Active",
                        "Very Active"
                });
        activityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        activityLevelSpinner.setAdapter(activityAdapter);

        // Setup climate spinner
        ArrayAdapter<String> climateAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item,
                new String[]{
                        "Moderate",
                        "Hot",
                        "Very Hot"
                });
        climateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        climateSpinner.setAdapter(climateAdapter);

        calculateButton.setOnClickListener(v -> {
            try {
                float weight = Float.parseFloat(weightInput.getText().toString());
                int activityLevel = activityLevelSpinner.getSelectedItemPosition();
                int climate = climateSpinner.getSelectedItemPosition();

                // Base water requirement (30ml per kg of body weight)
                double baseWater = weight * 30;

                // Activity level multiplier
                double[] activityMultipliers = {1.0, 1.3, 1.6};
                double activityAdjustedWater = baseWater * activityMultipliers[activityLevel];

                // Climate adjustment
                double[] climateMultipliers = {1.0, 1.2, 1.4};
                double totalWater = activityAdjustedWater * climateMultipliers[climate];

                // Convert to liters
                double liters = totalWater / 1000;
                int glasses = (int) Math.ceil(liters / 0.25); // Assuming 250ml per glass

                resultText.setText(String.format("Daily Water Intake: %.1f liters", liters));
                tipsText.setText(String.format("That's about %d glasses of water (250ml each)\n\n" +
                        "Tips:\n" +
                        "• Drink a glass of water when you wake up\n" +
                        "• Carry a water bottle\n" +
                        "• Drink before, during, and after exercise\n" +
                        "• Set reminders throughout the day", glasses));

            } catch (NumberFormatException e) {
                Toast.makeText(context, "Please enter valid weight", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setView(view);
        builder.show();
    }
}
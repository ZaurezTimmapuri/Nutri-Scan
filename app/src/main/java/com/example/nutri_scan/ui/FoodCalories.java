package com.example.nutri_scan.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.nutri_scan.BuildConfig;
import com.example.nutri_scan.R;
import com.example.nutri_scan.adapter.CalorieAdapter;
import com.example.nutri_scan.data.CalorieItem;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class FoodCalories extends AppCompatActivity {

    private EditText foodEditText;
    private CalorieAdapter adapter;
    private List<CalorieItem> CalorieItems;

    FloatingActionButton add,edit,clear;
    Animation fabopen,fabclose,rotateforward,rotatebackward;
    boolean is_open = false;

    private static final String API_URL = "https://api.calorieninjas.com/v1/nutrition?query=";
    private static final String API_KEY = BuildConfig.NINJA_API_KEY;



    Dialog dialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_calories);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.green));

        dialog = new Dialog(FoodCalories.this);
        dialog.setContentView(R.layout.dialog_fitpal);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.bg_fitpal_dialog));
        dialog.setCancelable(false);

        dialog.show();

        foodEditText = dialog.findViewById(R.id.food);
        Button searchButton = dialog.findViewById(R.id.searchButton);
        Button clearButton = dialog.findViewById(R.id.clearButton);
        RecyclerView recyclerView = findViewById(R.id.recyclerViewCalories);

        LoadingDialog loadingDialog = new LoadingDialog(FoodCalories.this);


        CalorieItems = new ArrayList<>();
        adapter = new CalorieAdapter(CalorieItems);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        add = (FloatingActionButton) findViewById(R.id.food_calorie_float);
        edit = (FloatingActionButton) findViewById(R.id.food_calorie_edit);
        clear = (FloatingActionButton) findViewById(R.id.food_calorie_clear);

        fabopen = AnimationUtils.loadAnimation(this,R.anim.food_calorie_anim_open);
        fabclose = AnimationUtils.loadAnimation(this,R.anim.food_calorie_anim_close);
        rotateforward = AnimationUtils.loadAnimation(this,R.anim.food_calorie_anim_rotate_forward);
        rotatebackward = AnimationUtils.loadAnimation(this,R.anim.food_calorie_anim_rotate_backward);

        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                animationFab();
            }
        });

        edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                animationFab();
                dialog.show();
            }
        });

        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                animationFab();
                foodEditText.setText("");
                CalorieItems.clear();
                adapter.notifyDataSetChanged();
                dialog.show();
            }
        });

        searchButton.setOnClickListener(v -> {
            String query = foodEditText.getText().toString().trim();
            if (!query.isEmpty()) {
                fetchNutritionData(query);
                dialog.dismiss();
                loadingDialog.startLoadingDialog();

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        loadingDialog.dismissDialog();
                    }
                },4200);

            } else {
                Toast.makeText(FoodCalories.this, "Please enter a food item", Toast.LENGTH_SHORT).show();
            }
        });



        clearButton.setOnClickListener(v -> {
            dialog.dismiss();
        });
    }

    private void fetchNutritionData(String query) {
        String url = FoodCalories.API_URL + query;
        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        if (response == null || response.trim().isEmpty()) {
                            Toast.makeText(FoodCalories.this, "Failed to fetch data", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        JSONObject jsonObject = new JSONObject(response);
                        JSONArray items = jsonObject.getJSONArray("items");

                        if (items.length() == 0) {
                            Toast.makeText(FoodCalories.this, "Failed to fetch data", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        CalorieItems.clear();
                        for (int i = 0; i < items.length(); i++) {
                            JSONObject item = items.getJSONObject(i);
                            String name = item.getString("name").toUpperCase();
                            int calories = item.getInt("calories");
                            int sugar = item.getInt("sugar_g");
                            int saturates = item.getInt("fat_saturated_g");
                            int carbohydrates = item.getInt("carbohydrates_total_g");
                            int sodium = item.getInt("sodium_mg");
                            int fiber = item.getInt("fiber_g");
                            int protein = item.getInt("protein_g");

                            // Calculate scores based on the nutrient values
                            int sodiumScore = calculateNutrientScore("sodium_value", sodium);
                            int energyScore = calculateNutrientScore("energy_value", calories);
                            int fiberScore = calculateNutrientScore("fiber", fiber);
                            int proteinScore = calculateNutrientScore("proteins", protein);
                            int sugarScore = calculateNutrientScore("sugars", sugar);
                            int fatScore = calculateNutrientScore("saturated-fat", saturates);

                            int score = sodiumScore+energyScore+fatScore+fiberScore+proteinScore+sugarScore;
                            String description = getScoreDescription(score);
                            CalorieItems.add(new CalorieItem(name, sugar, i , score, calories, saturates, carbohydrates, fiber, protein, sodium,description));
                        }
                        adapter.notifyDataSetChanged();
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(FoodCalories.this, "Error parsing data", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    error.printStackTrace();
                    Toast.makeText(FoodCalories.this, "Error fetching data", Toast.LENGTH_SHORT).show();
                }) {
            @Override
            public java.util.Map<String, String> getHeaders() {
                java.util.Map<String, String> headers = new java.util.HashMap<>();
                headers.put("X-Api-Key", API_KEY); // Add your API key here
                return headers;
            }
        };

        queue.add(stringRequest);
    }

    // Helper function to calculate score based on nutrient type and value
    private int calculateNutrientScore(String nutrientType, int value) {
        String description;
        int score = 0;

        switch (nutrientType) {
            case "sodium_value":
                if (value == 0) score = 0;
                else if (value < 126) score = 16;
                else if (value < 252) score = 12;
                else if (value < 441) score = 8;
                else score = 4;
                break;
            case "energy_value":
                if (value < 112) score = 16;
                else if (value < 252) score = 12;
                else if (value < 392) score = 8;
                else score = -16;
                break;
            case "fiber":
                score = (value < 2.45) ? 8 : 16;
                break;
            case "proteins":
                score = (value < 5.2) ? 8 : 16;
                break;
            case "sugars":
                if (value == 0) score = 0;
                else if (value < 6.3) score = 16;
                else if (value < 12.6) score = 12;
                else if (value < 21.7) score = 8;
                else score = 4;
                break;
            case "saturated-fat":
                if (value == 0) score = 0;
                else if (value < 1.4) score = 16;
                else if (value < 2.8) score = 12;
                else if (value < 4.9) score = 8;
                else score = 4;
                break;
        }
        if(score < 0){
            score = 0;
        }
        return score;
    }

    private String getScoreDescription(int totalScore) {
        if (totalScore <= 25) return "Bad";
        if (totalScore <= 45) return "Poor";
        if (totalScore <= 65) return "Good";
        return "Excellent";
    }

    private void animationFab(){
        if (is_open){
            add.startAnimation(rotatebackward);
            edit.startAnimation(fabclose);
            clear.startAnimation(fabclose);
            edit.setClickable(false);
            clear.setClickable(false);
            is_open = false;
        }else{
            add.startAnimation(rotateforward);
            edit.startAnimation(fabopen);
            clear.startAnimation(fabopen);
            edit.setClickable(true);
            clear.setClickable(true);
            is_open = true;
        }

    }

}

package com.example.nutri_scan.data;
import android.content.Context;
import android.content.res.AssetManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class AdditiveDatabase {
    private Map<String, AdditiveItem> additives;
    private Map<String, DangerItem> dangerList;

    public AdditiveDatabase(Context context) {
        additives = new HashMap<>();
        dangerList = new HashMap<>();
        loadAdditives(context);
        loadDangerList(context);
    }



    private void loadAdditives(Context context) {
        try {
            AssetManager assetManager = context.getAssets();
            InputStream inputStream = assetManager.open("additives_database.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder jsonBuilder = new StringBuilder();
            String line;

            // Read the entire JSON file into a StringBuilder
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }
            reader.close();

            // Parse JSON array
            JSONArray jsonArray = new JSONArray(jsonBuilder.toString());
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String code = jsonObject.getString("Enumber");
                String name = jsonObject.getString("Name");
                String description = jsonObject.getString("Description");
                String usage = jsonObject.getString("Usage");

                // Add to the additives map
                additives.put(code, new AdditiveItem(code, name, description, usage));
            }

        } catch (IOException | org.json.JSONException e) {
            e.printStackTrace();
        }
    }

    private void loadDangerList(Context context) {
        try {
            AssetManager assetManager = context.getAssets();
            InputStream inputStream = assetManager.open("danger_list.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder jsonBuilder = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }
            reader.close();

            // Parse JSON array
            JSONArray jsonArray = new JSONArray(jsonBuilder.toString());
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String code = jsonObject.getString("Enumber");
                String name = jsonObject.getString("Name");
                String hyperactivity = jsonObject.optString("HyperActivity", "");
                String asthma = jsonObject.optString("Asthma", "");
                String cancer = jsonObject.optString("Cancer", "");

                // Add to the dangerList map
                dangerList.put(code, new DangerItem(code, name, hyperactivity, asthma, cancer));
            }

        } catch (IOException | org.json.JSONException e) {
            e.printStackTrace();
        }
    }

    public AdditiveItem getAdditiveInfo(String additiveCode) {
        return additives.get(additiveCode);
    }

    // Method to get additive information
    public AdditiveItem getAdditiveCheck(String additiveCode) {
        AdditiveItem additive = additives.get(additiveCode);
        if (dangerList.containsKey(additiveCode)) {
            DangerItem dangerInfo = dangerList.get(additiveCode);
            return additive;  // Modify to include danger information if necessary
        } else {
            return additive;  // No information found
        }
    }
    public Map<String, DangerItem> getDangerList() {
        return dangerList;
    }
    // Method to check if the additive is in the danger list and return information
    public DangerItem getDangerInfo(String additiveCode) {
        return dangerList.getOrDefault(additiveCode, null);  // Return danger info or null
    }

}




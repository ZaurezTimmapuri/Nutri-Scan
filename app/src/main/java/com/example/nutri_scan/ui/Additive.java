package com.example.nutri_scan.ui;

import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nutri_scan.R;
import com.example.nutri_scan.adapter.AdditiveAdapter;
import com.example.nutri_scan.data.AdditiveDatabase;
import com.example.nutri_scan.data.AdditiveItem;

import java.util.ArrayList;
import java.util.List;

public class Additive extends AppCompatActivity {
    private RecyclerView recyclerView;
    private AdditiveAdapter adapter;
    private List<AdditiveItem> additiveItems;
    private AdditiveDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_additives);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.green));

        recyclerView = findViewById(R.id.additive_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        additiveItems = new ArrayList<>();
        database = new AdditiveDatabase(this);

        // Get additives from intent
        ArrayList<String> additives = getIntent().getStringArrayListExtra("additives");
        if (additives != null && !additives.isEmpty()) {
            // Load additive information using the AdditiveDatabase or process it accordingly
            AdditiveDatabase database = new AdditiveDatabase(this);
            for (String additiveCode : additives) {
                AdditiveItem item = database.getAdditiveInfo(additiveCode);
                if (item != null) {
                    additiveItems.add(item); // Add to the list of AdditiveItems
                }
            }
        } else {
            // Handle case where additives data is missing or empty
            Toast.makeText(this, "No additives found", Toast.LENGTH_SHORT).show();
        }

        adapter = new AdditiveAdapter(additiveItems,database);
        recyclerView.setAdapter(adapter);
    }
}

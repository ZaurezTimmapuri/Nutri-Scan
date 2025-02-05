package com.example.nutri_scan.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;


import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.nutri_scan.R;
import com.example.nutri_scan.data.TempDataHolder;

public class ProductInformation extends AppCompatActivity {

    private EditText nameEditText, brandEditText;
    private ProgressBar progressBar;
    private Spinner categorySpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_product_information);


        // Initialize views
        nameEditText = findViewById(R.id.nameEditText);
        brandEditText = findViewById(R.id.brandEditText);
        progressBar = findViewById(R.id.progressBar);
        categorySpinner = findViewById(R.id.categorySpinner);

        ValidatingDialog validatingDialog = new ValidatingDialog(ProductInformation.this);


        // Set progress bar values
        progressBar.setMax(100);
        progressBar.setProgress(16);
        progressBar.setSecondaryProgress(100);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.status_bar_color_green));

//        String[] categories = {"Select Category", "Fresh fruits and vegetables",
//                "Prepared fruits and vegetables", "Meat and Fish", "Bread and Pastries" ,
//                "Ready meals and dishes" , "Dairy product" , "Plant based products" ,
//                "Pasta,Rice,Grains","Snacks,Sauces and Condiments","Sweets","Beverages","Baby food",
//                "health and Wellness"};
        String[] categories = {
                "Select Category",
                "Beverages",
                "Prepared Fruits and Vegetables",
                "Seasoning,Spices,Condiments",
                "Meat and Fish",
                "Bread and Pastries",
                "Ready Meals and Dishes",
                "Dairy Products",
                "Plant-Based Products",
                "Pasta, Rice, and Grains",
                "Snacks",
                "Sauces",
                "Sweets",
                "Baby Food",
                "Health and Wellness"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_dropdown,
                categories
        );
        adapter.setDropDownViewResource(R.layout.spinner_dropdown);
        categorySpinner.setAdapter(adapter);

        // Set up Next button click listener
        Button nextButton = findViewById(R.id.next_product);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String productName = nameEditText.getText().toString();
                String productBrand = brandEditText.getText().toString();
                String selectedCategory = categorySpinner.getSelectedItem().toString();

                if(productName.isEmpty() || productName.equals(" ")){
                    Toast.makeText(ProductInformation.this, "Please enter the Product name", Toast.LENGTH_SHORT).show();
                }
                if (productBrand.isEmpty()|| productBrand.equals(" ")){
                    Toast.makeText(ProductInformation.this, "Please enter the Product brand", Toast.LENGTH_SHORT).show();

                }
                if (categorySpinner.getSelectedItemPosition() == 0) {
                    Toast.makeText(ProductInformation.this, "Please select a category", Toast.LENGTH_SHORT).show();
                }
                else{
                    validatingDialog.start_validation_Dialog();
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = new Intent(ProductInformation.this, ProductImage.class);
                            saveProductInfo(productName,productBrand,selectedCategory);
                            startActivity(intent);
                            validatingDialog.validation_dismiss_Dialog();
                        }
                    }, 3200);
                }
            }
        });

    }

    public void saveProductInfo(String productName, String productBrand , String selectedCategory) {
        TempDataHolder.saveProductInfo(productName, productBrand , selectedCategory);
        Intent intent = new Intent(this, OcrEnd.class);
        startActivity(intent);
    }

}


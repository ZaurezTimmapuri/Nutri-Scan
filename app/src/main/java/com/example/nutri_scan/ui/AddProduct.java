package com.example.nutri_scan.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.nutri_scan.R;

public class AddProduct extends AppCompatActivity {
    private Button promise;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_product);
        promise = findViewById(R.id.promiseButton);
        promise.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//               Toast.makeText(AddProduct.this, "We are Working on OCR, Stay tuned! :)", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(AddProduct.this, ProductInformation.class);
                startActivity(intent);
            }
        });
    }
}
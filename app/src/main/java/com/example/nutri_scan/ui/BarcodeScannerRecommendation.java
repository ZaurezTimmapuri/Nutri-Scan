package com.example.nutri_scan.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.budiyev.android.codescanner.DecodeCallback;
import com.example.nutri_scan.R;
import com.google.zxing.Result;

public class BarcodeScannerRecommendation extends AppCompatActivity {

    private CodeScanner rCodeScanner;
    String decoded_result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode_scanner_recommendation);

        CodeScannerView scannerView = findViewById(R.id.scanner_recommendation);
        rCodeScanner = new CodeScanner(this, scannerView);


        rCodeScanner.setDecodeCallback(new DecodeCallback() {
            @Override
            public void onDecoded(@NonNull Result result) {
                runOnUiThread(() -> {
                    Toast.makeText(BarcodeScannerRecommendation.this, result.getText(), Toast.LENGTH_SHORT).show();
                    decoded_result = result.getText();
                    Intent intent = new Intent();
                    intent.putExtra("BARCODE_RESULT", result.getText());
                    setResult(RESULT_OK, intent);
                    finish(); // Close the scanner activity
                });

            }

        });
    }
        @Override
        protected void onResume() {
            super.onResume();
            rCodeScanner.releaseResources();
            rCodeScanner.startPreview();
        }

        @Override
        protected void onPause() {
            rCodeScanner.releaseResources();
            super.onPause();
        }
}
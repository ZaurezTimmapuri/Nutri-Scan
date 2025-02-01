package com.example.nutri_scan.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.nutri_scan.R;

public class Splashscreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For Android 11+ (API 30+)
            getWindow().setDecorFitsSystemWindows(false);
        } else {
            // For Android below API 30
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splashscreen);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(Splashscreen.this,Dashboard.class));
                finish();
            }
        },1200);


    }

}
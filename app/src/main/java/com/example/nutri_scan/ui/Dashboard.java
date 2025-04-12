package com.example.nutri_scan.ui;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.viewpager2.widget.ViewPager2;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.nutri_scan.R;
import com.example.nutri_scan.adapter.FunctionalCardsAdapter;
import com.example.nutri_scan.adapter.ImageSliderAdapter;
import com.example.nutri_scan.databinding.ActivityDashboardBinding;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Calendar;


public class Dashboard extends AppCompatActivity {

    ActivityDashboardBinding binding;

    private SensorManager sensorManager;
    private Sensor gyroscopeSensor;
    private ImageView imageView;

    private float xRotation = 0f;
    private float yRotation = 0f;
    private static final float ROTATION_SENSITIVITY = 0.5f;

    Calendar calendar = Calendar.getInstance();
    int hour = calendar.get(Calendar.HOUR_OF_DAY);
    FloatingActionButton fitpal,food,burnt;
    Animation fabopen,fabclose;
    boolean is_open = false;
    private ViewPager2 viewPager2;
    private Handler sliderHandler;
    private Runnable sliderRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_dashboard);
        binding = ActivityDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NestedScrollView nestedScrollView = findViewById(R.id.card_view);
        nestedScrollView.setNestedScrollingEnabled(true);


        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.status_bar_color_green));

        viewPager2 = findViewById(R.id.viewPager2);
        viewPager2.setNestedScrollingEnabled(false); // Disable nested scrolling for ViewPager2
        setupImageSlider();

        ViewPager2 functionalCardsViewPager = findViewById(R.id.functionalCardsViewPager);
        functionalCardsViewPager.setNestedScrollingEnabled(false); // Disable nested scrolling
        setupFunctionalCards();

        fitpal = (FloatingActionButton) findViewById(R.id.fit_pal);
        food = (FloatingActionButton) findViewById(R.id.fit_pal_food);
//        burnt = (FloatingActionButton) findViewById(R.id.fit_pal_excercise);

        fabopen = AnimationUtils.loadAnimation(this,R.anim.food_calorie_anim_open);
        fabclose = AnimationUtils.loadAnimation(this,R.anim.food_calorie_anim_close);

        fitpal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                animationFab();
            }
        });
        food.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                animationFab();
                Intent intent = new Intent(Dashboard.this, FoodCalories.class);
                startActivity(intent);
            }
        });

        TextView greetingTextView = findViewById(R.id.greetingTextView);
        String greeting;
        if (hour >= 0 && hour < 12) {
            greeting = "Good Morning,\n" + "Welcome to Nutri-Scan";
        } else if (hour >= 12 && hour < 18) {
            greeting = "Good Afternoon,\n" + "Welcome to Nutri-Scan";
        } else {
            greeting = "Good Evening,\n" + "Welcome to Nutri-Scan";
        }

        // Set the greeting text
        greetingTextView.setText(greeting);

        viewPager2 = findViewById(R.id.viewPager2);
        setupImageSlider();
        setupFunctionalCards();

//        burnt.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Toast.makeText(Dashboard.this, "under-development :)", Toast.LENGTH_SHORT).show();
//            }
//        });


        // Check if we should highlight the home icon after returning from another activity

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.settings) {
                Intent intent = new Intent(Dashboard.this, Settings.class);
                startActivity(intent);
            }else if (itemId == R.id.dieto) {
                Intent intent = new Intent(Dashboard.this, Dieto.class);
                startActivity(intent);
            }else if (itemId == R.id.recommendation) {
                Intent intent = new Intent(Dashboard.this, Recommendation.class);
                startActivity(intent);
            }else if (itemId == R.id.home) {
                Intent homeIntent = new Intent(Dashboard.this, Dashboard.class);
                homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(homeIntent);
            }
            return true;
        });


        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            Intent intent = new Intent(Dashboard.this, Scanner.class);
            startActivity(intent);
        });


        ///For gyro-image

        imageView = findViewById(R.id.Gyro_imageView);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        }

}





    @SuppressLint("ClickableViewAccessibility")
    private void setupImageSlider() {
        viewPager2.setAdapter(new ImageSliderAdapter(this));
        viewPager2.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);

        sliderHandler = new Handler(Looper.getMainLooper());
        sliderRunnable = new Runnable() {
            @Override
            public void run() {
                int nextItem = viewPager2.getCurrentItem() + 1;
                if (nextItem >= viewPager2.getAdapter().getItemCount()) {
                    nextItem = 0;
                }
                viewPager2.setCurrentItem(nextItem);
            }
        };

        // Add page change callback to show slide animation
        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                sliderHandler.removeCallbacks(sliderRunnable);
                sliderHandler.postDelayed(sliderRunnable, 3000);
            }
        });

        viewPager2.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.getParent().requestDisallowInterceptTouchEvent(false);
                    break;
            }
            return false;
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        sliderHandler.removeCallbacks(sliderRunnable);

        if (sensorManager != null) {
            sensorManager.unregisterListener(sensorEventListener);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        sliderHandler.postDelayed(sliderRunnable, 3000);

        if (gyroscopeSensor != null) {
            sensorManager.registerListener(sensorEventListener, gyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    private void setupFunctionalCards() {
        ViewPager2 functionalCardsViewPager = findViewById(R.id.functionalCardsViewPager);
        FunctionalCardsAdapter adapter = new FunctionalCardsAdapter(this);
        functionalCardsViewPager.setAdapter(adapter);

        // Add touch listener for functional cards ViewPager2
        functionalCardsViewPager.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.getParent().requestDisallowInterceptTouchEvent(false);
                    break;
            }
            return false;
        });

        // Use direct pixel values instead of resources
        float nextItemVisiblePx = 24 * getResources().getDisplayMetrics().density; // 24dp
        float currentItemHorizontalMarginPx = 42 * getResources().getDisplayMetrics().density; // 42dp
        float pageTranslationX = nextItemVisiblePx + currentItemHorizontalMarginPx;

        ViewPager2.PageTransformer pageTransformer = (page, position) -> {
            page.setTranslationX(-pageTranslationX * position);
            page.setScaleY(1 - (0.25f * Math.abs(position)));
            page.setAlpha(0.25f + (1 - Math.abs(position)));
        };

        functionalCardsViewPager.setPageTransformer(pageTransformer);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Adjust scroll position when keyboard appears/disappears
        NestedScrollView nestedScrollView = findViewById(R.id.card_view);
        nestedScrollView.post(() -> nestedScrollView.scrollTo(0, nestedScrollView.getScrollY()));
    }


    private void animationFab(){
        if (is_open){
            food.startAnimation(fabclose);
//            burnt.startAnimation(fabclose);
            food.setClickable(false);
//            burnt.setClickable(false);
            is_open = false;
        }else{
            food.startAnimation(fabopen);
//            burnt.startAnimation(fabopen);
            food.setClickable(true);
//            burnt.setClickable(true);
            is_open = true;
        }

    }

    //For gyro_image

    private SensorEventListener sensorEventListener = new SensorEventListener() {
        private static final float ROTATION_SENSITIVITY = 0.5f;

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                // Gyroscope values: rotation around x, y, and z axes
                float gyroX = event.values[0];
                float gyroY = event.values[1];

                // Update rotation values
                xRotation -= gyroX * ROTATION_SENSITIVITY;
                yRotation -= gyroY * ROTATION_SENSITIVITY;

                // Limit the rotation to prevent extreme movements
                xRotation = Math.max(-30, Math.min(30, xRotation));
                yRotation = Math.max(-30, Math.min(30, yRotation));

                // Apply translation to the image view on the main thread
                runOnUiThread(() -> {
                    imageView.setTranslationX(yRotation * 10); // Horizontal movement
                    imageView.setTranslationY(xRotation * 10); // Vertical movement
                });
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Not needed for this implementation, but required by SensorEventListener
        }
    };


}
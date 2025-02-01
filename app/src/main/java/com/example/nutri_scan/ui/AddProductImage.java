package com.example.nutri_scan.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.nutri_scan.R;

public class AddProductImage extends AppCompatActivity {

    private ImageView productImageView;
    private Uri imageUri;
    private String imagePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product_image);

        Button nextButton = findViewById(R.id.next_nutrition);

        ValidatingDialog validatingDialog = new ValidatingDialog(AddProductImage.this);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.status_bar_color_green));

        productImageView = findViewById(R.id.imageViewCropped);

        // Get the image data from the intent
        if (getIntent().hasExtra(ProductImage.EXTRA_IMAGE_URI)) {
            try {
                imageUri = Uri.parse(getIntent().getStringExtra(ProductImage.EXTRA_IMAGE_URI));
                imagePath = getIntent().getStringExtra(ProductImage.EXTRA_IMAGE_PATH);

                // Load and display the image
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                if (productImageView != null) {
                    productImageView.setImageBitmap(bitmap);
                }
            } catch (Exception e) {
                e.printStackTrace();
                // Handle error loading image
            }
        }

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                validatingDialog.start_validation_Dialog();

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(AddProductImage.this, FetchNutrition.class);
                        intent.putExtra("ImageUri", imageUri);
                        intent.putExtra("ImagePath", imagePath);
                        startActivity(intent);
                        validatingDialog.validation_dismiss_Dialog();
                    }
                }, 3200);
            }
        });


    }

    // Method to get the image URI if needed elsewhere in the activity
    public Uri getImageUri() {
        return imageUri;
    }

    // Method to get the image file path if needed elsewhere in the activity
    public String getImagePath() {
        return imagePath;
    }
}
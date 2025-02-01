package com.example.nutri_scan.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.nutri_scan.R;
import com.example.nutri_scan.data.TempDataHolder;
import com.google.common.util.concurrent.ListenableFuture;
import com.yalantis.ucrop.UCrop;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class ProductImage extends AppCompatActivity {

    private static final int CAMERA_REQUEST_CODE = 101;
    private static final String FILE_PROVIDER_AUTHORITY = "com.example.nutri_scan.fileprovider";
    private static final String IMAGE_FILE_PREFIX = "JPEG_";
    private static final String IMAGE_FILE_SUFFIX = ".jpg";
    private static final String CROPPED_IMAGE_NAME = "cropped_image.jpg";
    private static final int MAX_IMAGE_DIMENSION = 1080;
    public static final String EXTRA_IMAGE_URI = "image_uri";
    public static final String EXTRA_IMAGE_PATH = "image_path";

    private PreviewView previewView;
    private Button captureButton;
    private Uri photoURI;
    private File photoFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_product_image);

        initializeViews();
        setupClickListeners();
        startCamera();
    }

    private void initializeViews() {
        previewView = findViewById(R.id.camera_preview);
        captureButton = findViewById(R.id.capture_front);
    }

    private void setupClickListeners() {
        if (captureButton != null) {
            captureButton.setOnClickListener(v -> takePhoto());
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                setupCameraProvider(cameraProviderFuture.get());
            } catch (ExecutionException | InterruptedException e) {
                handleCameraError(e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void setupCameraProvider(@NonNull ProcessCameraProvider cameraProvider) {
        try {
            CameraSelector cameraSelector = new CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build();

            Preview preview = new Preview.Builder().build();
            preview.setSurfaceProvider(previewView.getSurfaceProvider());

            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(this, cameraSelector, preview);
        } catch (Exception e) {
            handleCameraError(e);
        }
    }

    private void handleCameraError(Exception e) {
        e.printStackTrace();
        showToast("Failed to start camera: " + e.getMessage());
    }

    private void takePhoto() {
        try {
            photoFile = createImageFile();
            photoURI = FileProvider.getUriForFile(this, FILE_PROVIDER_AUTHORITY, photoFile);

            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE);
            } else {
                showToast("No camera app available");
            }
        } catch (IOException e) {
            e.printStackTrace();
            showToast("Error creating image file");
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = IMAGE_FILE_PREFIX + timeStamp + "_";
        return File.createTempFile(imageFileName, IMAGE_FILE_SUFFIX, getCacheDir());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK) {
            return;
        }

        if (requestCode == CAMERA_REQUEST_CODE && photoURI != null) {
            startCrop(photoURI);
        } else if (requestCode == UCrop.REQUEST_CROP && data != null) {
            handleCroppedImage(data);
        }
    }

    private void handleCroppedImage(Intent data) {
        Uri resultUri = UCrop.getOutput(data);
        if (resultUri != null) {
            try {
                // Save the cropped image to a file
                File croppedFile = new File(getCacheDir(), CROPPED_IMAGE_NAME);

                // Create intent with both URI and file path
                Intent intent = new Intent(this, AddProductImage.class);
                intent.putExtra(EXTRA_IMAGE_URI, resultUri.toString());
                intent.putExtra(EXTRA_IMAGE_PATH, croppedFile.getAbsolutePath());

                // Set flags to grant permissions to the receiving activity
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                saveImageData(resultUri,croppedFile);
                startActivity(intent);
                // Optional: finish this activity if you don't want to return to it
            } catch (Exception e) {
                e.printStackTrace();
                showToast("Failed to process cropped image");
            }
        } else {
            showToast("Failed to crop image");
        }
    }

    private void startCrop(Uri sourceUri) {
        File destinationFile = new File(getCacheDir(), CROPPED_IMAGE_NAME);
        UCrop.of(sourceUri, Uri.fromFile(destinationFile))
                .withMaxResultSize(MAX_IMAGE_DIMENSION, MAX_IMAGE_DIMENSION)
                .start(this);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up temporary files
        if (photoFile != null && photoFile.exists()) {
            photoFile.delete();
        }
    }

    public void saveImageData(Uri resultUri, File croppedFile) {
        TempDataHolder.saveImageData(resultUri.toString(), croppedFile.getAbsolutePath());
        Intent intent = new Intent(this, OcrEnd.class);
        startActivity(intent);
    }

}
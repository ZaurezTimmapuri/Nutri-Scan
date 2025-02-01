package com.example.nutri_scan.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Ingredients extends AppCompatActivity {


    private static final int CAMERA_REQUEST_CODE = 101;
    private static final String FILE_PROVIDER_AUTHORITY = "com.example.nutri_scan.fileprovider";
    private static final String IMAGE_FILE_PREFIX = "JPEG_";
    private static final String IMAGE_FILE_SUFFIX = ".jpg";
    private static final String CROPPED_IMAGE_NAME = "cropped_ingredients.jpg";
    private static final int MAX_IMAGE_DIMENSION = 1080;
    public static final String EXTRA_INGREDIENTS_DATA = "ingredients_data";

    private PreviewView previewView;
    private Button captureButton;
    private Uri photoURI;
    private File photoFile;
    private TextRecognizer textRecognizer;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ingredients);

        initializeViews();
        setupTextRecognizer();
        setupClickListeners();
        startCamera();
    }

    private void initializeViews() {
        previewView = findViewById(R.id.camera_preview);
        captureButton = findViewById(R.id.capture_information);
    }

    private void setupTextRecognizer() {
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
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
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), resultUri);
                processImage(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
                showToast("Failed to process image");
            }
        }
    }

    private void startCrop(Uri sourceUri) {
        File destinationFile = new File(getCacheDir(), CROPPED_IMAGE_NAME);
        UCrop.of(sourceUri, Uri.fromFile(destinationFile))
                .withMaxResultSize(MAX_IMAGE_DIMENSION, MAX_IMAGE_DIMENSION)
                .start(this);
    }

    private void processImage(Bitmap bitmap) {
        if (bitmap == null) {
            showToast("No image available");
            return;
        }

        InputImage image = InputImage.fromBitmap(bitmap, 0);
        textRecognizer.process(image)
                .addOnSuccessListener(this::processExtractedText)
                .addOnFailureListener(e -> showToast("Error extracting text: " + e.getMessage()));
    }

    private static class Ingredient {
        String name;
        boolean isAdditive;

        Ingredient(String name, boolean isAdditive) {
            this.name = name;
            this.isAdditive = isAdditive;
        }
    }

    private static class ProcessedIngredient {
        String name;
        boolean isAdditive;

        ProcessedIngredient(String name, boolean isAdditive) {
            this.name = name;
            this.isAdditive = isAdditive;
        }
    }

    private void processExtractedText(Text text) {
        if (text.getText().isEmpty()) {
            showToast("No text found in image");
            return;
        }

        List<Ingredient> ingredients = new ArrayList<>();
        boolean foundIngredientsSection = false;
        StringBuilder ingredientsText = new StringBuilder();

        for (Text.TextBlock block : text.getTextBlocks()) {
            String currentText = block.getText().toLowerCase();

            // Look for ingredients section markers
            if (currentText.contains("ingredient") ||
                    currentText.contains("contains") ||
                    foundIngredientsSection) {

                foundIngredientsSection = true;
                ingredientsText.append(block.getText()).append(" ");
            }
        }

        if (ingredientsText.length() == 0) {
            showToast("No ingredients section found");
            return;
        }

        processIngredients(ingredientsText.toString(), ingredients);

        if (ingredients.isEmpty()) {
            showToast("No ingredients could be extracted");
            return;
        }

        navigateToDisplayActivity(ingredients);
    }

    private ProcessedIngredient processIngredient(String ingredient) {
        // Match E-numbers in various formats:
        // 1. E followed by space then number (e.g., "E 100")
        // 2. E followed by hyphen then number (e.g., "E-100")
        // 3. E followed directly by number (e.g., "E100")
        // Case insensitive for 'E'
        String pattern = "\\b[Ee][-\\s]?\\d+";
        Pattern eNumberPattern = Pattern.compile(pattern);
        Matcher matcher = eNumberPattern.matcher(ingredient);

        if (matcher.find()) {
            // Extract the matched E-number
            String eNumber = matcher.group();
            // Convert to standard format by removing spaces and hyphens
            String standardizedENumber = "E" + eNumber.replaceAll("[Ee][-\\s]?", "");
            return new ProcessedIngredient(standardizedENumber, true);
        }

        return new ProcessedIngredient(ingredient, false);
    }

    private void processIngredients(String ingredientsText, List<Ingredient> ingredients) {
        // Split ingredients by common delimiters
        String[] parts = ingredientsText.split("[,;()]");

        for (String part : parts) {
            String ingredient = part.trim();
            if (ingredient.isEmpty()) continue;

            // Check for E-numbers with updated pattern
            ProcessedIngredient processed = processIngredient(ingredient);
            ingredients.add(new Ingredient(processed.name, processed.isAdditive));
        }
    }

    private boolean isAdditive(String ingredient) {
        String pattern = ".*\\b[Ee](?:[-\\s]?\\d+|\\d+).*";
        // First check for the pattern
        return ingredient.matches(pattern);
    }

    private void navigateToDisplayActivity(List<Ingredient> ingredients) {
        Bundle ingredientsBundle = new Bundle();

        // Convert ingredients list to arrays for bundle
        ArrayList<String> regularIngredients = new ArrayList<>();
        ArrayList<String> additives = new ArrayList<>();

        for (Ingredient ingredient : ingredients) {
            if (ingredient.isAdditive) {
                additives.add(ingredient.name);
            } else {
                regularIngredients.add(ingredient.name);
            }
        }

        ingredientsBundle.putStringArrayList("regular_ingredients", regularIngredients);
        ingredientsBundle.putStringArrayList("additives", additives);


//        Intent intent = new Intent(this, OcrEnd.class);
//        startActivity(intent);

        saveIngredientsData(ingredientsBundle);

    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (photoFile != null && photoFile.exists()) {
            photoFile.delete();
        }
    }

    public void saveIngredientsData(Bundle ingredientsData) {
        if (ingredientsData != null) {
            TempDataHolder.saveData(TempDataHolder.KEY_INGREDIENTS, ingredientsData);
            Intent intent = new Intent(this, OcrEnd.class);
            startActivity(intent);
        } else {
            Toast.makeText(this, "No ingredients data to save", Toast.LENGTH_SHORT).show();
        }
    }

}
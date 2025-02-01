package com.example.nutri_scan.ui;

import android.content.Intent;
import android.graphics.Bitmap;
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
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FetchNutrition extends AppCompatActivity {

    private static final int CAMERA_REQUEST_CODE = 101;
    private static final String FILE_PROVIDER_AUTHORITY = "com.example.nutri_scan.fileprovider";
    private static final String IMAGE_FILE_PREFIX = "JPEG_";
    private static final String IMAGE_FILE_SUFFIX = ".jpg";
    private static final String CROPPED_IMAGE_NAME = "cropped_nutrition.jpg";
    private static final int MAX_IMAGE_DIMENSION = 1080;
    public static final String EXTRA_NUTRITION_DATA = "nutrition_data";

    private PreviewView previewView;
    private Button captureButton;
    private Uri photoURI;
    private File photoFile;
    private TextRecognizer textRecognizer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_fetch_nutrition);

        initializeViews();
        setupTextRecognizer();
        setupClickListeners();
        startCamera();

    }

    private void initializeViews() {
        previewView = findViewById(R.id.camera_preview);
        captureButton = findViewById(R.id.capture_nutrition);
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

    private static class TextBlockInfo {
        Text.TextBlock block;
        float centerY;
        String text;

        TextBlockInfo(Text.TextBlock block) {
            this.block = block;
            this.text = block.getText().trim();
            this.centerY = (block.getBoundingBox().top + block.getBoundingBox().bottom) / 2.0f;
        }

        boolean isOnSameLine(TextBlockInfo other) {
            return Math.abs(this.centerY - other.centerY) < 10;
        }
    }

    private void processExtractedText(Text text) {
        if (text.getText().isEmpty()) {
            showToast("No text found in image");
            return;
        }

        Map<String, String> nutritionData = extractNutritionData(text);

        if (nutritionData.isEmpty()) {
            showToast("No nutrition information found");
            return;
        }

        navigateToDisplayActivity(nutritionData);
    }

    private Map<String, String> extractNutritionData(Text text) {
        Map<String, String> nutritionData = new HashMap<>();
        Set<String> nutritionKeywords = new HashSet<>(Arrays.asList(
                "calories", "total fat", "total fats", "saturated fat", "saturated fats",
                "trans fat", "trans fats", "salt", "salts", "carbohydrates", "dietary fibers",
                "total sugar", "added sugar", "cholesterol", "sodium", "total carbohydrate",
                "dietary fiber", "dietary fibre", "total sugars", "added sugars", "protein",
                "vitamin", "calcium", "proteins", "iron", "potassium", "energy",
                "monounsaturated fat", "polyunsaturated fat", "omega 3", "omega-3",
                "linolenic acid", "per serve", "per 100g", "%rda", "%daily"
        ));

        List<TextBlockInfo> blockInfos = new ArrayList<>();
        for (Text.TextBlock block : text.getTextBlocks()) {
            blockInfos.add(new TextBlockInfo(block));
        }

        String fullText = text.getText().replaceAll("[•;,.]", " ").replaceAll("\\s{2,}", " ").toLowerCase();




        for (int i = 0; i < blockInfos.size(); i++) {
            TextBlockInfo currentBlock = blockInfos.get(i);
            String currentText = currentBlock.text.toLowerCase();

            for (String keyword : nutritionKeywords) {
                if (currentText.contains(keyword)) {
                    String[] parts = splitNutritionLine(currentBlock.text);
                    if (parts != null) {
                        nutritionData.put(parts[0].trim(), parts[1].trim());
                    } else {
                        String value = findValueInNearbyBlocks(blockInfos, i, currentBlock);
                        if (value != null) {
                            nutritionData.put(currentBlock.text.trim(), value);
                        }
                    }
                    break;
                }
            }
        }
        for (String keyword : nutritionKeywords) {
            if (fullText.contains(keyword)) {
                String value = extractValueForKeyword(fullText, keyword);
                if (value != null) {
                    nutritionData.put(keyword, value);
                }
            }
        }

        return nutritionData;
    }


    private String extractValueForKeyword(String text, String keyword) {
        // Use a regex pattern to match common nutrient formats (e.g., Energy (kcal) 133, Protein (g) 1.1)
        Pattern pattern = Pattern.compile("(?i)" + Pattern.quote(keyword) + ".*?(\\d+(\\.\\d+)?\\s*(g|mg|kcal|%|))");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim(); // Extract the matched nutrient value
        }
        return null;
    }

    private String findValueInNearbyBlocks(List<TextBlockInfo> blocks, int currentIndex, TextBlockInfo currentBlock) {
        for (int i = currentIndex + 1; i < blocks.size(); i++) {
            TextBlockInfo potentialValueBlock = blocks.get(i);
            if (currentBlock.isOnSameLine(potentialValueBlock)) {
                String potentialValue = potentialValueBlock.text.trim();
                if (isNutritionValue(potentialValue)) {
                    return potentialValue;
                }
            }
        }
        return null;
    }

    private boolean isNutritionValue(String text) {
        String pattern = "^\\s*\\d+(\\.\\d+)?\\s*(g|mg|kcal|kJ|%|)\\s*$";
        return text.matches(pattern) || text.matches("^\\s*\\d+(\\.\\d+)?\\s*$");
    }

    private String[] splitNutritionLine(String line) {
        try {
            if (line.contains("...")) {
                return line.split("\\.+");
            }
            if (line.contains(":")) {
                return line.split(":");
            }
            if (line.matches(".*\\s{2,}.*")) {
                return line.split("\\s{2,}", 2);
            }
            Matcher matcher = Pattern.compile("^(.+?)\\s*(\\d+.*?)$").matcher(line);
            if (matcher.find()) {
                return new String[]{matcher.group(1), matcher.group(2)};
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private void navigateToDisplayActivity(Map<String, String> nutritionData) {
        Bundle nutritionBundle = new Bundle();
        for (Map.Entry<String, String> entry : nutritionData.entrySet()) {
            nutritionBundle.putString(entry.getKey(), entry.getValue());
        }

        Intent intent = new Intent(this, CheckNutrition.class);
        intent.putExtra(EXTRA_NUTRITION_DATA, nutritionBundle);
        startActivity(intent);
        finish();
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
}
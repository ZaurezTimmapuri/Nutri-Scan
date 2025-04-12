package com.example.nutri_scan.ui;

import android.content.Intent;
import android.os.Bundle;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.Volley;
import com.example.nutri_scan.BuildConfig;
import com.example.nutri_scan.R;
import com.example.nutri_scan.utils.Typewriter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;



public class Dieto extends AppCompatActivity {
    private LinearLayout chatLayout;
    private EditText messageInput;
    private ImageButton sendButton;
    private enum ModelType { LLAMA, MISTRAL }
    private static final ModelType CURRENT_MODEL = ModelType.MISTRAL;
//    private static final ModelType CURRENT_MODEL = ModelType.LLAMA;
    private ScrollView scrollView;
    private static final int MAX_RETRIES = 6;
    private static final String API_URL = CURRENT_MODEL == ModelType.LLAMA
            ? "https://api-inference.huggingface.co/models/meta-llama/Meta-Llama-3-8B-Instruct"
            : "https://router.huggingface.co/hf-inference/models/mistralai/Mistral-7B-Instruct-v0.2/v1/chat/completions";
    private static final String API_TOKEN = BuildConfig.HUGGING_API_KEY;
    private View currentLoadingView;
    private String productName;
    private String productBrand;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dieto);

        initializeViews();
        setupStatusBar();
        setupSendButton();

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("EXTRA_PRODUCT_NAME")) {
            productName = intent.getStringExtra("EXTRA_PRODUCT_NAME");
            productBrand = intent.getStringExtra("EXTRA_PRODUCT_BRAND");
            if (productName != null && !productName.isEmpty()) {
                // Automatically ask the user about the product
                String autoMessage = "Provide a description of the product  " + productName + " by " + productBrand +" and tell me how good is it for me";
                displayUserMessage(autoMessage);
                sendToApi(autoMessage, 0);
                displayLoadingIndicator();
            }
        }

    }


    private void initializeViews() {
        chatLayout = findViewById(R.id.chatLayout);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        scrollView = findViewById(R.id.scrollView);

        // Add text change listener to enable/disable send button
        messageInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                sendButton.setEnabled(s.toString().trim().length() > 0);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupStatusBar() {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.green));
    }

    private void setupSendButton() {
        sendButton.setOnClickListener(v -> {
            String userMessage = messageInput.getText().toString().trim();
            if (!userMessage.isEmpty()) {
                sendButton.setEnabled(false);
                messageInput.setEnabled(false); // Disable input
                displayUserMessage(userMessage);
                displayLoadingIndicator();
                messageInput.setText("");
                sendToApi(userMessage, 0);
            }
        });
    }

    private static class CustomJsonArrayRequest extends JsonRequest<JSONArray> {
        private final Response.Listener<JSONArray> listener;
        private final JSONObject jsonRequest;
        private static final String PROTOCOL_CHARSET = "utf-8";

        public CustomJsonArrayRequest(int method, String url, JSONObject jsonRequest,
                                      Response.Listener<JSONArray> listener,
                                      Response.ErrorListener errorListener) {
            super(method, url, (jsonRequest == null) ? null : jsonRequest.toString(), listener, errorListener);
            this.jsonRequest = jsonRequest;
            this.listener = listener;
        }

        @Override
        protected Response<JSONArray> parseNetworkResponse(NetworkResponse response) {
            try {
                String jsonString = new String(response.data,
                        HttpHeaderParser.parseCharset(response.headers, PROTOCOL_CHARSET));
                return Response.success(new JSONArray(jsonString),
                        HttpHeaderParser.parseCacheHeaders(response));
            } catch (UnsupportedEncodingException | JSONException e) {
                return Response.error(new ParseError(e));
            }
        }

        @Override
        public Map<String, String> getHeaders() throws AuthFailureError {
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer " + API_TOKEN);
            headers.put("Content-Type", "application/json");
            return headers;
        }

        @Override
        public String getBodyContentType() {
            return "application/json; charset=utf-8";
        }

        @Override
        public byte[] getBody() {
            try {
                return jsonRequest.toString().getBytes(PROTOCOL_CHARSET);
            } catch (UnsupportedEncodingException e) {
                return null;
            }
        }
    }

//    private void sendToApi(final String userMessage, final int retryCount) {
//        RequestQueue queue = Volley.newRequestQueue(this);
//        queue.add(new CustomJsonArrayRequest(
//                Request.Method.POST,
//                API_URL,
//                createApiPayload(constructPrompt(userMessage)),
//                response -> handleApiResponse(response, userMessage),
//                error -> handleApiError(error, userMessage, retryCount)
//        ));
//    }

    private void sendToApi(final String userMessage, final int retryCount) {
        RequestQueue queue = Volley.newRequestQueue(this);
        JSONObject payload;

        if (CURRENT_MODEL == ModelType.LLAMA) {
            String prompt = constructLlamaPrompt(userMessage);
            payload = createLlamaPayload(prompt);
            queue.add(new CustomJsonArrayRequest(
                    Request.Method.POST,
                    API_URL,
                    payload,
                    response -> handleLlamaResponse(response, userMessage),
                    error -> handleApiError(error, userMessage, retryCount)
            ));
        } else {
            payload = createMistralPayload(userMessage);
            JsonObjectRequest mistralRequest = new JsonObjectRequest(
                    Request.Method.POST,
                    API_URL,
                    payload,
                    response -> handleMistralResponse(response, userMessage),
                    error -> handleApiError(error, userMessage, retryCount)
            ) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Authorization", "Bearer " + API_TOKEN);
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };
            queue.add(mistralRequest);
        }
    }

    private String constructLlamaPrompt(String userMessage) {
        return String.format(Locale.US,
                "<|begin_of_text|>\n" +
                        "<|start_header_id|>system<|end_header_id|>\n" +
                        "You are Dieto, an expert nutritionist dietitian and fitness expert.\n"+
                        "Follow these rules strictly:\n" +
                        "1. Only answer questions about nutrition, diets, fitness or food science\n" +
                        "2. If asked about other topics, respond: \"I specialize in nutrition , fitness and diet advice. Please ask food and fitness-related questions.\"\n" +
                        "3. Keep responses under 50 words\n" +
                        "4. Use simple, clear language\n" +
                        "5. Always provide practical advice\n" +
                        "Current question: %s\n" +
                        "<|eot_id|>\n" +
                        "<|start_header_id|>assistant<|end_header_id|>\n",
                userMessage
        );
    }



    private JSONObject createLlamaPayload(String prompt) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("inputs", prompt);
            JSONObject parameters = new JSONObject()
                    .put("max_new_tokens", 120)
                    .put("temperature", 0.2)
                    .put("top_p", 0.95)
                    .put("repetition_penalty", 1.2)
                    .put("return_full_text", false);
            payload.put("parameters", parameters);
        } catch (JSONException e) {
            Log.e("Dieto", "Payload error", e);
        }
        return payload;
    }

    private JSONObject createMistralPayload(String userMessage) {
        JSONObject payload = new JSONObject();
        try {
            JSONArray messages = new JSONArray();

            // System message
            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            systemMessage.put("content",
                    "You are Dieto, an expert nutritionist dietitian and fitness expert.\n" +
                            "Follow these rules strictly:\n" +
                            "1. Only answer questions about nutrition, diets, fitness or food science\n" +
                            "2. If asked about other topics, respond: \"I specialize in nutrition , fitness and diet advice. Please ask food and fitness-related questions.\"\n" +
                            "3. Keep responses under 50 words\n" +
                            "4. Use simple, clear language\n" +
                            "5. Always provide practical advice");

            // User message
            JSONObject userMessageObj = new JSONObject();
            userMessageObj.put("role", "user");
            userMessageObj.put("content", userMessage);

            messages.put(systemMessage);
            messages.put(userMessageObj);

            payload.put("messages", messages);
            payload.put("max_tokens", 120);
            payload.put("temperature", 0.2);
            payload.put("top_p", 0.95);
        } catch (JSONException e) {
            Log.e("Dieto", "Mistral payload error", e);
        }
        return payload;
    }


    private void handleLlamaResponse(JSONArray response, String userMessage) {
        try {
            String generatedText = response.getJSONObject(0)
                    .getString("generated_text")
                    .trim();
            displayResponse(generatedText);
        } catch (Exception e) {
            handleError("Couldn't process response");
        }
    }

    private void handleMistralResponse(JSONObject response, String userMessage) {
        try {
            JSONArray choices = response.getJSONArray("choices");
            if (choices.length() > 0) {
                JSONObject message = choices.getJSONObject(0)
                        .getJSONObject("message");
                String generatedText = message.getString("content").trim();
                displayResponse(generatedText);
            } else {
                handleEmptyResponse();
            }
        } catch (Exception e) {
            handleError("Couldn't process Mistral response");
        }
    }

    private void displayResponse(String generatedText) {
        runOnUiThread(() -> {
            if (currentLoadingView != null) {
                chatLayout.removeView(currentLoadingView);
                currentLoadingView = null;
            }
            displayAIResponse(cleanResponse(generatedText));
            sendButton.setEnabled(true);
            messageInput.setEnabled(true);
        });
    }


//    private void handleApiResponse(JSONArray response, String userMessage) {
//        try {
//            String generatedText = response.getJSONObject(0)
//                    .getString("generated_text")
//                    .trim();
//
//            runOnUiThread(() -> {
//                if (currentLoadingView != null) {
//                    chatLayout.removeView(currentLoadingView);
//                    currentLoadingView = null;
//                }
//                displayAIResponse(cleanResponse(generatedText));
//                sendButton.setEnabled(true);
//                messageInput.setEnabled(true); // Re-enable input
//            });
//        } catch (Exception e) {
//            handleError("Couldn't process response");
//        }
//    }

    private String cleanResponse(String response) {
        // Basic cleaning without content validation
        return response.replaceAll("(?i)<.*?>", "")  // Remove HTML tags
                .replaceAll("\\n+", " ")       // Replace newlines
                .replaceAll("\\s{2,}", " ")    // Collapse spaces
                .trim();
    }

    private void handleApiError(VolleyError error, String userMessage, int retryCount) {
        NetworkResponse response = error.networkResponse;
        if (retryCount < MAX_RETRIES) {
            new Handler().postDelayed(() -> sendToApi(userMessage, retryCount + 1), 1000);
        } else {
            runOnUiThread(() -> {
                if (currentLoadingView != null) {
                    chatLayout.removeView(currentLoadingView);
                    currentLoadingView = null;
                }
                displayAIResponse("Connection error. Please try again.");
                sendButton.setEnabled(true);
                messageInput.setEnabled(true); // Re-enable input
            });
        }
    }





    private void handleEmptyResponse() {
        handleError("I don't have a response for that. Please try rephrasing your question.");
        Toast.makeText(this, "\"I don't have a response for that. Please try rephrasing your question.\"", Toast.LENGTH_SHORT).show();
    }


    private void handleError(String errorMessage) {
        runOnUiThread(() -> {
            if (currentLoadingView != null) {
                chatLayout.removeView(currentLoadingView);
                currentLoadingView = null;
            }
            displayAIResponse(errorMessage);
            sendButton.setEnabled(true);
            messageInput.setEnabled(true); // Re-enable input
        });
    }


    //For display
    private void displayUserMessage(String message) {
        View userBubble = getLayoutInflater().inflate(R.layout.user_message_bubble, null);
        TextView userMessageTextView = userBubble.findViewById(R.id.userMessageBubble);
        userMessageTextView.setText(message);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(16, 16, 16, 16);
        params.gravity = Gravity.END;
        userBubble.setLayoutParams(params);

        chatLayout.addView(userBubble);
        scrollToBottom();
    }
    private void displayLoadingIndicator() {
        runOnUiThread(() -> {
            View loadingBubble = getLayoutInflater().inflate(R.layout.ai_loading_bubble, null);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(16, 16, 16, 16);
            params.gravity = Gravity.START;
            loadingBubble.setLayoutParams(params);

            chatLayout.addView(loadingBubble);
            currentLoadingView = loadingBubble;
            scrollToBottom();
        });
    }
    private void displayAIResponse(String response) {
        View aiBubble = getLayoutInflater().inflate(R.layout.ai_response_bubble, null);
        Typewriter aiMessageTextView = aiBubble.findViewById(R.id.aiResponseBubble);
        aiMessageTextView.setText("AI is typing...");
        aiMessageTextView.setCharacterDelay(20);
        aiMessageTextView.animateText(response);
//        aiMessageTextView.setText(response);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(16, 16, 16, 16);
        params.gravity = Gravity.START;
        aiBubble.setLayoutParams(params);

        chatLayout.addView(aiBubble);
        scrollToBottom();
    }

    private void scrollToBottom() {
        scrollView.post(() -> {
            // Add slight delay to ensure proper measurement
            new Handler().postDelayed(() -> {
                scrollView.fullScroll(View.FOCUS_DOWN);
                scrollView.smoothScrollTo(0, scrollView.getBottom());
            }, 100);
        });
    }
}






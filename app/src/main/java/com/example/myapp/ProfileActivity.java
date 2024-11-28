package com.example.myapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {
    private static final String TAG = "ProfileActivity";
    private static final int PICK_IMAGE = Constants.PICK_IMAGE_REQUEST;

    private ImageView profileImageView;
    private TextView usernameTextView;
    private TextView emailTextView;
    private EditText newUsernameEditText;
    private EditText currentPasswordEditText;
    private EditText newPasswordEditText;
    private Button updateUsernameButton;
    private Button updatePasswordButton;
    private Button selectImageButton;
    private Button logoutButton;
    private ImageButton backButton;
    private Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_page);

        initializeViews();
        setupClickListeners();
        loadUserData();
    }

    private void initializeViews() {
        profileImageView = findViewById(R.id.profileImageView);
        usernameTextView = findViewById(R.id.usernameTextView);
        emailTextView = findViewById(R.id.emailTextView);
        newUsernameEditText = findViewById(R.id.newUsernameEditText);
        currentPasswordEditText = findViewById(R.id.currentPasswordEditText);
        newPasswordEditText = findViewById(R.id.newPasswordEditText);
        updateUsernameButton = findViewById(R.id.updateUsernameButton);
        updatePasswordButton = findViewById(R.id.updatePasswordButton);
        selectImageButton = findViewById(R.id.selectImageButton);
        backButton = findViewById(R.id.backButton);
        logoutButton = findViewById(R.id.logoutButton);
    }

    private void setupClickListeners() {
        updateUsernameButton.setOnClickListener(v -> updateUsername());
        updatePasswordButton.setOnClickListener(v -> updatePassword());
        selectImageButton.setOnClickListener(v -> openGallery());
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, DashboardPage.class);
            startActivity(intent);
            finish();
        });
        logoutButton.setOnClickListener(v -> showLogoutConfirmationDialog());
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> performLogout())
                .setNegativeButton("No", null)
                .show();
    }

    private void performLogout() {
        SharedPreferences prefs = getSharedPreferences(Constants.SHARED_PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(Constants.ACCESS_TOKEN_KEY);
        editor.remove(Constants.REFRESH_TOKEN_KEY);
        editor.clear(); // Clear all stored data
        editor.apply();

        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, LoginPage.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void loadUserData() {
        String token = getAccessToken();
        if (token.isEmpty()) {
            Log.e(TAG, "No token available for loading user data");
            redirectToLogin();
            return;
        }

        new Thread(() -> {
            try {
                URL url = new URL(Constants.BASE_URL + Constants.ENDPOINT_PROFILE);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty(Constants.AUTHORIZATION_HEADER,
                        Constants.BEARER_PREFIX + token);

                logRequestDetails(conn);

                int responseCode = conn.getResponseCode();
                Log.d(TAG, "Profile fetch response code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());

                    runOnUiThread(() -> {
                        try {
                            usernameTextView.setText(jsonResponse.getString("username"));
                            emailTextView.setText(jsonResponse.getString("email"));
                            if (!jsonResponse.isNull("profile_photo")) {
                                loadProfileImage(jsonResponse.getString("profile_photo"));
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error updating UI with profile data", e);
                        }
                    });
                } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    Log.e(TAG, "Unauthorized access, token might be invalid");
                    runOnUiThread(this::redirectToLogin);
                } else {
                    handleErrorResponse(conn);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading user data", e);
                showError("Error loading profile: " + e.getMessage());
            }
        }).start();
    }

    private void updateUsername() {
        String newUsername = newUsernameEditText.getText().toString().trim();
        if (newUsername.isEmpty()) {
            showToast("Please enter a new username");
            return;
        }

        new Thread(() -> {
            try {
                URL url = new URL(Constants.BASE_URL + Constants.ENDPOINT_UPDATE_USERNAME);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty(Constants.AUTHORIZATION_HEADER,
                        Constants.BEARER_PREFIX + getAccessToken());
                conn.setDoOutput(true);

                logRequestDetails(conn);

                JSONObject jsonInput = new JSONObject();
                jsonInput.put("username", newUsername);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonInput.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    runOnUiThread(() -> {
                        showToast("Username updated successfully");
                        newUsernameEditText.setText("");
                        loadUserData();
                    });
                } else {
                    handleErrorResponse(conn);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating username", e);
                showError("Error: " + e.getMessage());
            }
        }).start();
    }

    private void updatePassword() {
        String currentPassword = currentPasswordEditText.getText().toString().trim();
        String newPassword = newPasswordEditText.getText().toString().trim();

        if (currentPassword.isEmpty() || newPassword.isEmpty()) {
            showToast("Please enter both current and new passwords");
            return;
        }

        new Thread(() -> {
            try {
                URL url = new URL(Constants.BASE_URL + Constants.ENDPOINT_UPDATE_PASSWORD);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty(Constants.AUTHORIZATION_HEADER,
                        Constants.BEARER_PREFIX + getAccessToken());
                conn.setDoOutput(true);

                logRequestDetails(conn);

                JSONObject jsonInput = new JSONObject();
                jsonInput.put("current_password", currentPassword);
                jsonInput.put("new_password", newPassword);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonInput.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    runOnUiThread(() -> {
                        showToast("Password updated successfully");
                        currentPasswordEditText.setText("");
                        newPasswordEditText.setText("");
                    });
                } else {
                    handleErrorResponse(conn);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating password", e);
                showError("Error: " + e.getMessage());
            }
        }).start();
    }

    private void openGallery() {
        Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(gallery, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE && data != null) {
            selectedImageUri = data.getData();
            uploadProfileImage();
        }
    }

    private void uploadProfileImage() {
        if (selectedImageUri == null) return;

        new Thread(() -> {
            try {
                String boundary = "*****" + System.currentTimeMillis() + "*****";
                URL url = new URL(Constants.BASE_URL + Constants.ENDPOINT_UPDATE_PHOTO);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                conn.setRequestProperty(Constants.AUTHORIZATION_HEADER,
                        Constants.BEARER_PREFIX + getAccessToken());
                conn.setDoOutput(true);

                logRequestDetails(conn);

                try (OutputStream os = conn.getOutputStream();
                     DataOutputStream writer = new DataOutputStream(os)) {

                    writer.writeBytes("--" + boundary + "\r\n");
                    writer.writeBytes("Content-Disposition: form-data; name=\"profile_photo\"; filename=\"profile.jpg\"\r\n");
                    writer.writeBytes("Content-Type: image/jpeg\r\n\r\n");

                    InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        writer.write(buffer, 0, bytesRead);
                    }
                    inputStream.close();

                    writer.writeBytes("\r\n");
                    writer.writeBytes("--" + boundary + "--\r\n");
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    runOnUiThread(() -> {
                        showToast("Profile photo updated successfully");
                        loadUserData();
                    });
                } else {
                    handleErrorResponse(conn);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error uploading profile image", e);
                showError("Error: " + e.getMessage());
            }
        }).start();
    }

    private void loadProfileImage(String photoUrl) {
        String fullUrl = photoUrl.startsWith("http") ? photoUrl : Constants.BASE_URL + photoUrl;
        Log.d(TAG, "Loading profile image from: " + fullUrl);

        Glide.with(this)
                .load(fullUrl)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .placeholder(R.drawable.default_profile)
                .error(R.drawable.default_profile)
                .into(profileImageView);
    }

    private void logRequestDetails(HttpURLConnection conn) {
        try {
            Log.d(TAG, "Request URL: " + conn.getURL());
            Log.d(TAG, "Request Method: " + conn.getRequestMethod());
            Log.d(TAG, "Headers:");
            for (Map.Entry<String, List<String>> header : conn.getRequestProperties().entrySet()) {
                Log.d(TAG, header.getKey() + ": " + header.getValue());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error logging request details", e);
        }
    }

    private void handleErrorResponse(HttpURLConnection conn) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            response.append(line);
        }
        final String errorMessage = response.toString();
        Log.e(TAG, "Error response: " + errorMessage);
        showError("Failed to update: " + errorMessage);
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginPage.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String getAccessToken() {
        SharedPreferences prefs = getSharedPreferences(Constants.SHARED_PREFS_NAME, MODE_PRIVATE);
        String token = prefs.getString(Constants.ACCESS_TOKEN_KEY, "");
        Log.d(TAG, "Token from SharedPreferences: " + token);
        return token;
    }

    private void showError(String message) {
        runOnUiThread(() -> showToast(message));
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        String token = getAccessToken();
        if (token.isEmpty()) {
            showError("No token found. Please login again");
            redirectToLogin();
        }
    }
}
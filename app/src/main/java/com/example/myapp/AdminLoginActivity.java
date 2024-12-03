package com.example.myapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class AdminLoginActivity extends AppCompatActivity {
    private EditText adminUsername, adminPassword;
    private Button loginButton;
    private TextView gotoUserLogin;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_login_page);

        // Check if already logged in
        if (checkAdminLoginStatus()) {
            navigateToAdminDashboard();
            return;
        }

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        adminUsername = findViewById(R.id.admin_username);
        adminPassword = findViewById(R.id.admin_password);
        loginButton = findViewById(R.id.admin_login_button);
        gotoUserLogin = findViewById(R.id.goto_user_login);
        progressBar = findViewById(R.id.loading_progress);
    }

    private void setupClickListeners() {
        loginButton.setOnClickListener(v -> performLogin());
        gotoUserLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginPage.class));
            finish();
        });
    }

    private boolean checkAdminLoginStatus() {
        SharedPreferences prefs = getSharedPreferences("AdminPrefs", MODE_PRIVATE);
        return prefs.getBoolean("is_admin_logged_in", false);
    }

    private void navigateToAdminDashboard() {
        Intent intent = new Intent(this, AdminDashBoardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void performLogin() {
        String username = adminUsername.getText().toString().trim();
        String password = adminPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showToast("Please enter both username and password");
            return;
        }

        setLoading(true);

        new Thread(() -> {
            try {
                URL url = new URL(Constants.BASE_URL + "/admin/login/");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                // Create JSON request body
                JSONObject jsonInput = new JSONObject();
                jsonInput.put("username", username);
                jsonInput.put("password", password);

                // Send request
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonInput.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), "utf-8"));
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }

                    JSONObject jsonResponse = new JSONObject(response.toString());

                    if (jsonResponse.getString("status").equals("success")) {
                        JSONObject data = jsonResponse.getJSONObject("data");
                        String accessToken = data.getString("access");
                        String refreshToken = data.getString("refresh");
                        String adminUsername = data.getString("username");
                        String adminEmail = data.getString("email");
                        boolean isAdmin = data.getBoolean("is_admin");

                        if (isAdmin) {
                            // Save admin session data
                            SharedPreferences prefs = getSharedPreferences("AdminPrefs", MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString("admin_access_token", accessToken);
                            editor.putString("admin_refresh_token", refreshToken);
                            editor.putString("admin_username", adminUsername);
                            editor.putString("admin_email", adminEmail);
                            editor.putBoolean("is_admin_logged_in", true);
                            editor.apply();

                            runOnUiThread(() -> {
                                setLoading(false);
                                try {
                                    showToast(jsonResponse.getString("message"));
                                } catch (JSONException e) {
                                    throw new RuntimeException(e);
                                }
                                // Navigate to admin dashboard
                                Intent intent = new Intent(AdminLoginActivity.this, AdminDashBoardActivity.class);
                                intent.putExtra("admin_username", adminUsername);  // Add these extras
                                intent.putExtra("admin_email", adminEmail);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            });
                        } else {
                            runOnUiThread(() -> {
                                setLoading(false);
                                showToast("Access denied: Not an admin user");
                            });
                        }
                    } else {
                        runOnUiThread(() -> {
                            setLoading(false);
                            showToast("Login failed: " + jsonResponse.optString("message", "Unknown error"));
                        });
                    }
                } else {
                    // Handle error response
                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getErrorStream(), "utf-8"));
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }

                    final String errorMessage = response.toString();
                    runOnUiThread(() -> {
                        setLoading(false);
                        showToast("Login failed: " + errorMessage);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    setLoading(false);
                    showToast("Error: " + e.getMessage());
                });
            }
        }).start();
    }

    private void saveAdminSession(String token) {
        SharedPreferences prefs = getSharedPreferences("AdminPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("admin_token", token);
        editor.putBoolean("is_admin_logged_in", true);
        editor.apply();
    }

    private void setLoading(boolean isLoading) {
        runOnUiThread(() -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            loginButton.setEnabled(!isLoading);
            adminUsername.setEnabled(!isLoading);
            adminPassword.setEnabled(!isLoading);
        });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Exit")
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Yes", (dialog, which) -> finish())
                .setNegativeButton("No", null)
                .show();
    }
}
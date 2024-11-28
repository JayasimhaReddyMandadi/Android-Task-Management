package com.example.myapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class LoginPage extends AppCompatActivity {
    private static final String TAG = "LoginPage";
    private EditText usernameInput, passwordInput;
    private ProgressBar progressBar;
    private String username, password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_page);

        initializeViews();
        setupClickListeners();

        // Check if user is already logged in
        SharedPreferences prefs = getSharedPreferences(Constants.SHARED_PREFS_NAME, MODE_PRIVATE);
        String token = prefs.getString(Constants.ACCESS_TOKEN_KEY, "");

        if (!token.isEmpty()) {
            // Verify token validity
            verifyTokenAndRedirect(token);
        }
    }

    private void initializeViews() {
        usernameInput = findViewById(R.id.username_input);
        passwordInput = findViewById(R.id.password_input);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupClickListeners() {
        findViewById(R.id.login_btn).setOnClickListener(v -> login());
        findViewById(R.id.register_redirect_btn).setOnClickListener(v -> navigateToRegister());
    }

    private void setLoadingVisible(boolean visible) {
        if (progressBar != null) {
            progressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
        if (usernameInput != null) usernameInput.setEnabled(!visible);
        if (passwordInput != null) passwordInput.setEnabled(!visible);
        View loginButton = findViewById(R.id.login_btn);
        if (loginButton != null) loginButton.setEnabled(!visible);
        View registerButton = findViewById(R.id.register_redirect_btn);
        if (registerButton != null) registerButton.setEnabled(!visible);
    }

    private void verifyTokenAndRedirect(String token) {
        setLoadingVisible(true);
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(Constants.BASE_URL + Constants.ENDPOINT_PROFILE);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty(Constants.AUTHORIZATION_HEADER,
                        Constants.BEARER_PREFIX + token);
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Token verification response code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d(TAG, "Token is valid, redirecting to dashboard");
                    runOnUiThread(this::navigateToDashBoard);
                } else {
                    Log.d(TAG, "Token is invalid, clearing token");
                    clearToken();
                    runOnUiThread(() -> setLoadingVisible(false));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error verifying token", e);
                clearToken();
                runOnUiThread(() -> setLoadingVisible(false));
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    private void clearToken() {
        SharedPreferences prefs = getSharedPreferences(Constants.SHARED_PREFS_NAME, MODE_PRIVATE);
        prefs.edit()
                .remove(Constants.ACCESS_TOKEN_KEY)
                .remove(Constants.REFRESH_TOKEN_KEY)
                .apply();
    }

    public void login() {
        username = usernameInput.getText().toString().trim();
        password = passwordInput.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password");
            return;
        }

        setLoadingVisible(true);

        new Thread(() -> {
            HttpURLConnection connection = null;
            BufferedReader reader = null;
            try {
                URL url = new URL(Constants.BASE_URL + Constants.ENDPOINT_LOGIN);
                Log.d(TAG, "Login URL: " + url.toString());

                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                JSONObject loginData = new JSONObject();
                loginData.put("username", username);
                loginData.put("password", password);

                Log.d(TAG, "Attempting login for user: " + username);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = loginData.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Login response code: " + responseCode);

                reader = new BufferedReader(new InputStreamReader(
                        responseCode == HttpURLConnection.HTTP_OK ?
                                connection.getInputStream() :
                                connection.getErrorStream()
                ));

                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                Log.d(TAG, "Login response: " + response.toString());

                final JSONObject jsonResponse = new JSONObject(response.toString());

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    runOnUiThread(() -> handleLoginSuccess(jsonResponse));
                } else {
                    String errorMessage = jsonResponse.optString("detail", "Login failed");
                    runOnUiThread(() -> {
                        showError(errorMessage);
                        setLoadingVisible(false);
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "Login error", e);
                runOnUiThread(() -> {
                    showError("Login failed: " + e.getMessage());
                    setLoadingVisible(false);
                });
            } finally {
                try {
                    if (reader != null) reader.close();
                    if (connection != null) connection.disconnect();
                } catch (Exception e) {
                    Log.e(TAG, "Error closing resources", e);
                }
            }
        }).start();
    }

    private void handleLoginSuccess(JSONObject response) {
        try {
            if (!response.has("access")) {
                showError("Invalid server response: missing access token");
                setLoadingVisible(false);
                return;
            }

            String accessToken = response.getString("access");
            if (accessToken.isEmpty()) {
                showError("Invalid access token received");
                setLoadingVisible(false);
                return;
            }

            SharedPreferences prefs = getSharedPreferences(Constants.SHARED_PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(Constants.ACCESS_TOKEN_KEY, accessToken);
            editor.apply();

            String savedToken = prefs.getString(Constants.ACCESS_TOKEN_KEY, "");
            Log.d(TAG, "Token saved successfully: " + (savedToken.equals(accessToken)));

            if (!savedToken.equals(accessToken)) {
                showError("Failed to save login token");
                setLoadingVisible(false);
                return;
            }

            showToast("Login successful");
            navigateToDashBoard();
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing login response", e);
            showError("Login failed: Invalid response format");
            setLoadingVisible(false);
        }
    }

    private void navigateToDashBoard() {
        Intent intent = new Intent(LoginPage.this, DashboardPage.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToRegister() {
        Intent intent = new Intent(LoginPage.this, RegistrationPage.class);
        startActivity(intent);
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
        passwordInput.setText("");
        setLoadingVisible(false);
    }
}
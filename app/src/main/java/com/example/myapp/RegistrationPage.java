package com.example.myapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class RegistrationPage extends AppCompatActivity {

    private EditText usernameInput, emailInput, passwordInput;
    private String username, email, password;
    private final String REGISTER_URL = "http://172.16.20.92:8000/api/register/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration_page);

        // Initialize views
        initializeViews();

        // Setup click listeners
        setupClickListeners();
    }

    private void initializeViews() {
        usernameInput = findViewById(R.id.usernameEditText);
        emailInput = findViewById(R.id.emailEditText);
        passwordInput = findViewById(R.id.passwordEditText);
    }

    private void setupClickListeners() {
        findViewById(R.id.button3).setOnClickListener(this::registerUser);
        findViewById(R.id.cancel_button).setOnClickListener(view -> navigateToLogin());
    }

    public void registerUser(View view) {
        // Get input values
        username = usernameInput.getText().toString().trim();
        email = emailInput.getText().toString().trim();
        password = passwordInput.getText().toString().trim();

        // Validate input
        if (!validateInput()) {
            return;
        }

        // Perform registration
        performRegistration();
    }

    private boolean validateInput() {
        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void performRegistration() {
        new Thread(() -> {
            try {
                URL url = new URL(REGISTER_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setDoOutput(true);

                // Prepare post data
                String postData = "username=" + URLEncoder.encode(username, "UTF-8") +
                        "&email=" + URLEncoder.encode(email, "UTF-8") +
                        "&password=" + URLEncoder.encode(password, "UTF-8");

                // Send data
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(postData.getBytes());
                    os.flush();
                }

                // Handle response
                int responseCode = connection.getResponseCode();
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        responseCode == 201 ? connection.getInputStream() : connection.getErrorStream()
                ));

                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // Handle result
                handleRegistrationResult(responseCode, response.toString());

            } catch (Exception e) {
                Log.e("RegistrationError", "Error: ", e);
                showError(e.getMessage());
            }
        }).start();
    }

    private void handleRegistrationResult(int responseCode, String response) {
        runOnUiThread(() -> {
            if (responseCode == 201) {
                Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show();
                navigateToLogin();
            } else {
                Toast.makeText(this, "Registration failed: " + response, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showError(String message) {
        runOnUiThread(() ->
                Toast.makeText(this, "An error occurred: " + message, Toast.LENGTH_SHORT).show()
        );
    }

    private void navigateToLogin() {
        Intent i = new Intent(RegistrationPage.this, LoginPage.class);
        startActivity(i);
        finish(); // Close the registration activity
    }
}
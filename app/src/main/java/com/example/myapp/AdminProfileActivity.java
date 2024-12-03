package com.example.myapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class AdminProfileActivity extends AppCompatActivity {
    private EditText nameEditText;
    private EditText emailEditText;
    private EditText currentPasswordEditText;
    private EditText newPasswordEditText;
    private EditText confirmPasswordEditText;
    private Button updateProfileButton;
    private Button changePasswordButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_profile);

        initializeViews();
        setupToolbar();
        loadAdminProfile();
        setupButtons();
    }

    private void initializeViews() {
        nameEditText = findViewById(R.id.editTextName);
        emailEditText = findViewById(R.id.editTextEmail);
        currentPasswordEditText = findViewById(R.id.editTextCurrentPassword);
        newPasswordEditText = findViewById(R.id.editTextNewPassword);
        confirmPasswordEditText = findViewById(R.id.editTextConfirmPassword);
        updateProfileButton = findViewById(R.id.buttonUpdateProfile);
        changePasswordButton = findViewById(R.id.buttonChangePassword);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Admin Profile");
        }
    }

    private void loadAdminProfile() {
        SharedPreferences prefs = getSharedPreferences("AdminPrefs", MODE_PRIVATE);
        String name = prefs.getString("admin_name", "");
        String email = prefs.getString("admin_email", "");

        nameEditText.setText(name);
        emailEditText.setText(email);
    }

    private void setupButtons() {
        updateProfileButton.setOnClickListener(v -> updateProfile());
        changePasswordButton.setOnClickListener(v -> changePassword());
    }

    private void updateProfile() {
        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();

        if (validateProfileInputs(name, email)) {
            // TODO: Implement API call to update profile
            saveProfileLocally(name, email);
            showToast("Profile updated successfully");
        }
    }

    private void changePassword() {
        String currentPassword = currentPasswordEditText.getText().toString();
        String newPassword = newPasswordEditText.getText().toString();
        String confirmPassword = confirmPasswordEditText.getText().toString();

        if (validatePasswordInputs(currentPassword, newPassword, confirmPassword)) {
            // TODO: Implement API call to change password
            showToast("Password changed successfully");
            clearPasswordFields();
        }
    }

    private boolean validateProfileInputs(String name, String email) {
        if (name.isEmpty() || email.isEmpty()) {
            showToast("All fields are required");
            return false;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showToast("Please enter a valid email address");
            return false;
        }
        return true;
    }

    private boolean validatePasswordInputs(String currentPassword, String newPassword, String confirmPassword) {
        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            showToast("All password fields are required");
            return false;
        }
        if (newPassword.length() < 6) {
            showToast("New password must be at least 6 characters");
            return false;
        }
        if (!newPassword.equals(confirmPassword)) {
            showToast("New passwords do not match");
            return false;
        }
        return true;
    }

    private void saveProfileLocally(String name, String email) {
        SharedPreferences.Editor editor = getSharedPreferences("AdminPrefs", MODE_PRIVATE).edit();
        editor.putString("admin_name", name);
        editor.putString("admin_email", email);
        editor.apply();
    }

    private void clearPasswordFields() {
        currentPasswordEditText.setText("");
        newPasswordEditText.setText("");
        confirmPasswordEditText.setText("");
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
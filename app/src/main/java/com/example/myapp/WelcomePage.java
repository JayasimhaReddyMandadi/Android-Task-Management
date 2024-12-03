package com.example.myapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Button;

public class WelcomePage extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_welcome_page);

        initializeButtons();
        checkLoginStatus();
    }

    private void initializeButtons() {
        findViewById(R.id.login_button).setOnClickListener(v -> showLoginOptions());
        findViewById(R.id.signup_button).setOnClickListener(v -> navigateToRegister());
    }

    private void checkLoginStatus() {
        // Check if user is already logged in
        if (isUserLoggedIn()) {
            navigateToDashboard();
        }
    }

    private boolean isUserLoggedIn() {
        // Check shared preferences for login status
        return getSharedPreferences("UserPrefs", MODE_PRIVATE)
                .getBoolean("isLoggedIn", false);
    }

    private void showLoginOptions() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_login_options, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();

        Button adminLoginBtn = dialogView.findViewById(R.id.admin_login_btn);
        Button userLoginBtn = dialogView.findViewById(R.id.user_login_btn);

        adminLoginBtn.setOnClickListener(v -> {
            dialog.dismiss();
            navigateToAdminLogin();
        });

        userLoginBtn.setOnClickListener(v -> {
            dialog.dismiss();
            navigateToUserLogin();
        });
    }

    private void navigateToAdminLogin() {
        Intent intent = new Intent(this, AdminLoginActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void navigateToUserLogin() {
        Intent intent = new Intent(this, LoginPage.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void navigateToRegister() {
        Intent intent = new Intent(this, RegistrationPage.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void navigateToDashboard() {
        Intent intent = new Intent(this, DashboardPage.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        new AlertDialog.Builder(this)
                .setTitle("Exit App")
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Yes", (dialog, which) -> finish())
                .setNegativeButton("No", null)
                .show();
    }
}
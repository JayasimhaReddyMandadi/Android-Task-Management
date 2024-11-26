package com.example.myapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

public class WelcomePage extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_welcome_page); // Ensure this matches your main page layout


        // Set a click listener for the login button
        findViewById(R.id.login_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToLogin();
            }
        });

        // Set a click listener for the register button (if needed)
        findViewById(R.id.signup_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToRegister();
            }
        });
    }

    // Navigate to the login page
    private void navigateToLogin() {
        Intent intent = new Intent(WelcomePage.this, LoginPage.class);
        startActivity(intent);
    }

    // Navigate to the registration page (optional)
    private void navigateToRegister() {
        Intent intent = new Intent(WelcomePage.this, RegistrationPage.class);
        startActivity(intent);
    }
}

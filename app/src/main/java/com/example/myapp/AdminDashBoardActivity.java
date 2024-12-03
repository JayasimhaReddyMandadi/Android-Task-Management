package com.example.myapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class AdminDashBoardActivity extends AppCompatActivity {

    private TextView usernameText, emailText;
    private MaterialCardView viewTasks, viewUsers;  // Changed from Button to MaterialCardView
    private MaterialButton logoutBtn;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle si) {
        super.onCreate(si);
        setContentView(R.layout.activity_admin_dashboard);

        usernameText = findViewById(R.id.admin_username);
        emailText = findViewById(R.id.admin_email);
        viewTasks = findViewById(R.id.view_tasks_btn);
        viewUsers = findViewById(R.id.view_users_btn);
        logoutBtn=findViewById(R.id.logout_btn);

        sharedPreferences = getSharedPreferences("AdminPrefs", MODE_PRIVATE);

        String username = sharedPreferences.getString("admin_username", "");
        String email = sharedPreferences.getString("admin_email", "");

        if (username.isEmpty() || email.isEmpty()) {
            username = getIntent().getStringExtra("admin_username");
            email = getIntent().getStringExtra("admin_email");

            if (username != null && email != null) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("admin_username", username);
                editor.putString("admin_email", email);
                editor.apply();
            }
        }
        usernameText.setText("Username: " + username);
        emailText.setText("Email: " + email);

        viewTasks.setOnClickListener(v -> {
            Intent i = new Intent(AdminDashBoardActivity.this, AdminTaskList.class);
            startActivity(i);
        });

        viewUsers.setOnClickListener(v -> {
            Intent i = new Intent(AdminDashBoardActivity.this, UsersList.class);
            startActivity(i);
        });

        logoutBtn.setOnClickListener(v -> logout());
    }

    private void logout() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        Intent intent = new Intent(this, WelcomePage.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        String username = sharedPreferences.getString("admin_username", "");
        String email = sharedPreferences.getString("admin_email", "");

        if (!username.isEmpty() && !email.isEmpty()) {
            usernameText.setText("Username: " + username);
            emailText.setText("Email: " + email);
        }
    }
}
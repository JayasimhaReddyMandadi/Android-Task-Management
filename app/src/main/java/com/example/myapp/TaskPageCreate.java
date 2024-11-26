package com.example.myapp;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TaskPageCreate extends AppCompatActivity {
    private static final String TAG = "TaskPageCreate";
    private static final String BASE_URL = "http://172.16.20.92:8000";

    private EditText titleEditText;
    private EditText descriptionEditText;
    private Spinner statusSpinner;
    private Spinner prioritySpinner;
    private TextView dueDatePicker;
    private Button createButton;
    private Button cancelButton;
    private Date dueDate;
    private SimpleDateFormat apiDateFormat;
    private SimpleDateFormat displayDateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_task_page);
        setTitle("Create New Task");

        apiDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        displayDateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

        initializeViews();
        setupSpinners();
        setupDatePicker();
        setupCreateButton();
        setupCancelButton();
    }

    private void initializeViews() {
        titleEditText = findViewById(R.id.create_task_title);
        descriptionEditText = findViewById(R.id.create_task_description);
        statusSpinner = findViewById(R.id.create_task_status);
        prioritySpinner = findViewById(R.id.create_task_priority);
        dueDatePicker = findViewById(R.id.create_task_due_date);
        createButton = findViewById(R.id.create_task_button);
        cancelButton = findViewById(R.id.cancel_button);
    }

    private void setupSpinners() {
        // Status Spinner
        String[] statusOptions = new String[]{
                "yet-to-start",
                "in-progress",
                "completed",
                "on_hold"
        };
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, statusOptions);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(statusAdapter);

        // Priority Spinner
        String[] priorityOptions = new String[]{"low", "medium", "high"};
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, priorityOptions);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        prioritySpinner.setAdapter(priorityAdapter);
    }

    private void setupDatePicker() {
        dueDatePicker.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            if (dueDate != null) {
                calendar.setTime(dueDate);
            }

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        Calendar selectedDate = Calendar.getInstance();
                        selectedDate.set(year, month, dayOfMonth);
                        dueDate = selectedDate.getTime();
                        dueDatePicker.setText(displayDateFormat.format(dueDate));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });
    }

    private void setupCreateButton() {
        createButton.setOnClickListener(v -> {
            if (validateInput()) {
                createButton.setEnabled(false);
                createTask();
            }
        });
    }

    private void setupCancelButton() {
        cancelButton.setOnClickListener(v -> {
            Intent intent = new Intent(TaskPageCreate.this, DashboardPage.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
    }

    private boolean validateInput() {
        String title = titleEditText.getText().toString().trim();
        if (title.isEmpty()) {
            titleEditText.setError("Title is required");
            return false;
        }

        String description = descriptionEditText.getText().toString().trim();
        if (description.isEmpty()) {
            descriptionEditText.setError("Description is required");
            return false;
        }

        return true;
    }

    private void createTask() {
        new Thread(() -> {
            HttpURLConnection connection = null;
            BufferedReader reader = null;
            try {
                // Create the task data
                JSONObject taskData = new JSONObject();
                taskData.put("title", titleEditText.getText().toString().trim());
                taskData.put("description", descriptionEditText.getText().toString().trim());
                taskData.put("status", statusSpinner.getSelectedItem().toString());
                taskData.put("priority", prioritySpinner.getSelectedItem().toString());
                if (dueDate != null) {
                    taskData.put("deadline", apiDateFormat.format(dueDate));
                }

                // Log the request data
                Log.d(TAG, "Creating task with data: " + taskData.toString());

                // Setup connection
                URL url = new URL(BASE_URL + "/api/tasks/");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Authorization", "Bearer " + getAccessToken());
                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                // Send the request
                try (OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream())) {
                    writer.write(taskData.toString());
                    writer.flush();
                }

                // Get the response
                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Server responded with code: " + responseCode);

                // Read the response
                reader = new BufferedReader(new InputStreamReader(
                        responseCode >= 400 ? connection.getErrorStream() : connection.getInputStream()));
                StringBuilder responseBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    responseBuilder.append(line);
                }
                String responseBody = responseBuilder.toString();
                Log.d(TAG, "Server response: " + responseBody);

                // Handle the response
                if (responseCode == HttpURLConnection.HTTP_CREATED ||
                        responseCode == HttpURLConnection.HTTP_OK) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(this, "Task created successfully", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(TaskPageCreate.this, DashboardPage.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.putExtra("REFRESH_TASKS", true);
                        startActivity(intent);
                        finish();
                    });
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(this,
                                "Failed to create task: " + responseBody,
                                Toast.LENGTH_LONG).show();
                        createButton.setEnabled(true);
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error creating task", e);
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(this,
                            "Error: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    createButton.setEnabled(true);
                });
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (Exception e) {
                        Log.e(TAG, "Error closing reader", e);
                    }
                }
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    private String getAccessToken() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        return sharedPreferences.getString("access_token", null);
    }
}
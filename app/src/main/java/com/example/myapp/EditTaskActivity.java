package com.example.myapp;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AutoCompleteTextView;

import androidx.appcompat.app.AppCompatActivity;


import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class EditTaskActivity extends AppCompatActivity {
    private static final String TAG = "EditTaskActivity";
    private EditText titleEditText;
    private EditText descriptionEditText;
    private AutoCompleteTextView statusDropdown;
    private AutoCompleteTextView priorityDropdown;
    private TextView dueDatePicker;
    private Button updateButton;
    private Date dueDate;
    private int taskId;
    private SimpleDateFormat apiDateFormat;
    private SimpleDateFormat displayDateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_task);

        apiDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        displayDateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

        initializeViews();
        setupSpinners();
        loadTaskData();
        setupDatePicker();
        setupUpdateButton();
    }

    @SuppressLint("WrongViewCast")
    private void initializeViews() {
        titleEditText = findViewById(R.id.input_task_name);
        descriptionEditText = findViewById(R.id.input_task_description);
        statusDropdown = findViewById(R.id.task_status_dropdown);
        priorityDropdown = findViewById(R.id.task_priority_dropdown);
        dueDatePicker = findViewById(R.id.task_due_date_picker);
        updateButton = findViewById(R.id.btn_save_task);
    }

    private void setupSpinners() {
        // Setup status dropdown
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(
                this,
                R.layout.dropdown_item,
                new String[]{"yet-to-start", "in-progress", "completed","on_hold"}
        );
        statusDropdown.setAdapter(statusAdapter);
        statusDropdown.setKeyListener(null);

        // Setup priority dropdown
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(
                this,
                R.layout.dropdown_item,
                new String[]{"low", "medium", "high"}
        );
        priorityDropdown.setAdapter(priorityAdapter);
        priorityDropdown.setKeyListener(null);
    }

    private void loadTaskData() {
        Intent intent = getIntent();
        taskId = intent.getIntExtra("task_id", -1);

        if (taskId == -1) {
            Toast.makeText(this, "Error: Invalid task ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        titleEditText.setText(intent.getStringExtra("task_title"));
        descriptionEditText.setText(intent.getStringExtra("task_description"));

        // Set dropdown selections
        String status = intent.getStringExtra("task_status");
        String priority = intent.getStringExtra("task_priority");
        statusDropdown.setText(status, false);
        priorityDropdown.setText(priority, false);

        // Set due date
        String dueDateStr = intent.getStringExtra("task_due_date");
        if (dueDateStr != null) {
            try {
                dueDate = apiDateFormat.parse(dueDateStr);
                dueDatePicker.setText(displayDateFormat.format(dueDate));
            } catch (ParseException e) {
                Log.e(TAG, "Error parsing date: " + dueDateStr, e);
                Toast.makeText(this, "Error loading due date", Toast.LENGTH_SHORT).show();
            }
        }
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

    private void setupUpdateButton() {
        updateButton.setOnClickListener(v -> updateTask());
    }

    private void updateTask() {
        // Validate input fields
        String title = titleEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        String status = statusDropdown.getText().toString();
        String priority = priorityDropdown.getText().toString();

        if (title.isEmpty()) {
            titleEditText.setError("Title is required");
            return;
        }

        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL("http://172.16.20.92:8000/api/tasks/" + taskId + "/");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("PUT");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Authorization", "Bearer " + getAccessToken());
                connection.setDoOutput(true);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                JSONObject taskData = new JSONObject();
                taskData.put("title", title);
                taskData.put("description", description);
                taskData.put("status", status);
                taskData.put("priority", priority);
                if (dueDate != null) {
                    taskData.put("deadline", apiDateFormat.format(dueDate));
                }

                // Log the request data
                final String requestBody = taskData.toString();
                Log.d(TAG, "Request Body: " + requestBody);
                Log.d(TAG, "Token: " + getAccessToken());

                runOnUiThread(() ->
                        Toast.makeText(this, "Updating task...", Toast.LENGTH_SHORT).show()
                );

                try (OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream())) {
                    writer.write(requestBody);
                    writer.flush();
                }

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Response Code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Task updated successfully", Toast.LENGTH_SHORT).show();
                        Intent resultIntent = new Intent();
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    });
                } else {
                    // Read error response
                    BufferedReader reader;
                    if (responseCode >= 400) {
                        reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    } else {
                        reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    }

                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    final String errorMessage = response.toString();
                    Log.e(TAG, "Error Response: " + errorMessage);

                    runOnUiThread(() -> {
                        try {
                            JSONObject errorJson = new JSONObject(errorMessage);
                            String displayError = errorJson.optString("detail", errorMessage);
                            Toast.makeText(this, "Failed to update task: " + displayError,
                                    Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Toast.makeText(this, "Failed to update task: " + errorMessage,
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating task", e);
                final String errorMessage = e.getMessage();
                runOnUiThread(() ->
                        Toast.makeText(this, "Error: " + errorMessage, Toast.LENGTH_LONG).show()
                );
            } finally {
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
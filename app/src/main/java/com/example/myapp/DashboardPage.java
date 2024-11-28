package com.example.myapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardPage extends AppCompatActivity implements TaskAdapter.OnTaskActionListener {
    private static final String TAG = "DashboardPage";
    private TextView totalTasksTextView;
    private TextView completedTasksTextView;
    private TextView pendingTasksTextView;
    private TextView onHoldTasksTextView;
    private RecyclerView tasksRecyclerView;
    private TaskAdapter taskAdapter;
    private Button createTaskButton;
    private Button applyFiltersButton;
    private ImageButton profileButton;
    private List<Task> allTasks;

    private static final String DATE_FORMAT_API = "yyyy-MM-dd";
    private SimpleDateFormat apiDateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard_page);

        String token = getAccessToken();
        if (token.isEmpty()) {
            Log.e(TAG, "No token found, redirecting to login");
            redirectToLogin();
            return;
        }

        apiDateFormat = new SimpleDateFormat(DATE_FORMAT_API, Locale.getDefault());
        initializeViews();
        setupRecyclerView();
        setupButtons();
        fetchTasksFromServer();
    }

    private void initializeViews() {
        totalTasksTextView = findViewById(R.id.total_task_count);
        completedTasksTextView = findViewById(R.id.completed_task_count);
        pendingTasksTextView = findViewById(R.id.pending_task_count);
        onHoldTasksTextView = findViewById(R.id.on_hold_task_count);
        tasksRecyclerView = findViewById(R.id.tasks_recycler_view);
        createTaskButton = findViewById(R.id.btn_create_task);
        applyFiltersButton = findViewById(R.id.btn_apply_filters);
        profileButton = findViewById(R.id.profile_button);
    }

    private void setupRecyclerView() {
        taskAdapter = new TaskAdapter(this);
        tasksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        tasksRecyclerView.setAdapter(taskAdapter);
        tasksRecyclerView.setHasFixedSize(true);

        int spacing = getResources().getDimensionPixelSize(R.dimen.task_item_spacing);
        tasksRecyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                                       @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                outRect.bottom = spacing;
            }
        });
    }

    private void setupButtons() {
        createTaskButton.setOnClickListener(v -> navigateToCreateTask());
        applyFiltersButton.setOnClickListener(v -> showFiltersDialog());
        profileButton.setOnClickListener(v -> navigateToProfile());
    }

    private void navigateToProfile() {
        Intent intent = new Intent(DashboardPage.this, ProfileActivity.class);
        startActivity(intent);
    }

    private void navigateToCreateTask() {
        Intent intent = new Intent(DashboardPage.this, TaskPageCreate.class);
        startActivity(intent);
    }

    private void showFiltersDialog() {
        Toast.makeText(this, "Filter functionality coming soon!", Toast.LENGTH_SHORT).show();
    }

    private String getAccessToken() {
        SharedPreferences prefs = getSharedPreferences(Constants.SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        String token = prefs.getString(Constants.ACCESS_TOKEN_KEY, "");
        Log.d(TAG, "Token from SharedPreferences: " + token);
        return token;
    }

    private void fetchTasksFromServer() {
        String token = getAccessToken();
        if (token.isEmpty()) {
            Log.e(TAG, "No token available for fetching tasks");
            redirectToLogin();
            return;
        }

        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(Constants.BASE_URL + Constants.ENDPOINT_TASKS);
                Log.d(TAG, "Fetching tasks from URL: " + url.toString());

                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                String authHeader = Constants.BEARER_PREFIX + token;
                Log.d(TAG, "Authorization header: " + authHeader);
                connection.setRequestProperty(Constants.AUTHORIZATION_HEADER, authHeader);

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Tasks fetch response code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    Log.d(TAG, "Tasks response: " + response.toString());
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    JSONArray tasksArray = jsonResponse.getJSONArray("results");
                    List<Task> tasks = new ArrayList<>();

                    for (int i = 0; i < tasksArray.length(); i++) {
                        JSONObject taskObject = tasksArray.getJSONObject(i);
                        Task task = parseTaskFromJson(taskObject);
                        tasks.add(task);
                    }

                    runOnUiThread(() -> {
                        allTasks = tasks;
                        updateUIWithTasks(tasks);
                    });
                } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    Log.e(TAG, "Unauthorized access, token might be invalid");
                    runOnUiThread(this::redirectToLogin);
                } else {
                    String errorMessage = getErrorMessage(connection);
                    Log.e(TAG, "Failed to fetch tasks: " + errorMessage);
                    runOnUiThread(() ->
                            Toast.makeText(this, "Failed to fetch tasks: " + errorMessage,
                                    Toast.LENGTH_SHORT).show()
                    );
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching tasks", e);
                runOnUiThread(() ->
                        Toast.makeText(this, "Error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    private Task parseTaskFromJson(JSONObject taskObject) throws JSONException {
        int id = taskObject.getInt("id");
        String status = taskObject.getString("status");
        String priority = taskObject.getString("priority");
        String title = taskObject.getString("title");
        String description = taskObject.getString("description");

        Date dueDate = null;
        if (!taskObject.isNull("deadline")) {
            try {
                dueDate = apiDateFormat.parse(taskObject.getString("deadline"));
            } catch (ParseException e) {
                Log.e(TAG, "Error parsing deadline", e);
            }
        }

        return new Task(id, status, priority, dueDate, title, description);
    }

    private void updateUIWithTasks(List<Task> tasks) {
        Log.d(TAG, "Updating UI with tasks: " + tasks.size());

        int totalTasks = tasks.size();
        int completedTasks = 0;
        int pendingTasks = 0;
        int onHoldTasks = 0;

        for (Task task : tasks) {
            Log.d(TAG, "Task: " + task.getTitle() + ", Status: " + task.getStatus());

            switch (task.getStatus().toLowerCase()) {
                case "completed":
                    completedTasks++;
                    break;
                case "on_hold":
                    onHoldTasks++;
                    break;
                case "yet-to-start":
                case "in-progress":
                    pendingTasks++;
                    break;
                default:
                    Log.w(TAG, "Unknown status: " + task.getStatus());
                    pendingTasks++;
                    break;
            }
        }

        totalTasksTextView.setText(String.valueOf(totalTasks));
        completedTasksTextView.setText(String.valueOf(completedTasks));
        pendingTasksTextView.setText(String.valueOf(pendingTasks));
        onHoldTasksTextView.setText(String.valueOf(onHoldTasks));

        taskAdapter.setTasks(tasks);
        tasksRecyclerView.post(() -> {
            taskAdapter.notifyDataSetChanged();
            Log.d(TAG, "RecyclerView data updated");
        });
    }

    private String getErrorMessage(HttpURLConnection connection) {
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getErrorStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        } catch (Exception e) {
            return "Unknown error";
        }
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginPage.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onDeleteTask(Task task) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(Constants.BASE_URL + Constants.ENDPOINT_DELETE_TASK + task.getId() + "/");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("DELETE");
                connection.setRequestProperty(Constants.AUTHORIZATION_HEADER,
                        Constants.BEARER_PREFIX + getAccessToken());

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_NO_CONTENT ||
                        responseCode == HttpURLConnection.HTTP_OK) {
                    runOnUiThread(() -> {
                        allTasks.remove(task);
                        updateUIWithTasks(allTasks);
                        Toast.makeText(this, "Task deleted successfully",
                                Toast.LENGTH_SHORT).show();
                    });
                } else {
                    String errorMessage = getErrorMessage(connection);
                    runOnUiThread(() ->
                            Toast.makeText(this, "Failed to delete task: " + errorMessage,
                                    Toast.LENGTH_SHORT).show()
                    );
                }
            } catch (Exception e) {
                Log.e(TAG, "Error deleting task", e);
                runOnUiThread(() ->
                        Toast.makeText(this, "Error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    @Override
    public void onEditTask(Task task) {
        Intent intent = new Intent(this, EditTaskActivity.class);
        intent.putExtra("task_id", task.getId());
        intent.putExtra("task_title", task.getTitle());
        intent.putExtra("task_description", task.getDescription());
        intent.putExtra("task_status", task.getStatus());
        intent.putExtra("task_priority", task.getPriority());
        if (task.getDueDate() != null) {
            intent.putExtra("task_due_date", apiDateFormat.format(task.getDueDate()));
        }
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String token = getAccessToken();
        if (token.isEmpty()) {
            Log.e(TAG, "No token found in onResume, redirecting to login");
            redirectToLogin();
            return;
        }
        Log.d(TAG, "onResume called, fetching tasks");
        new Handler().postDelayed(this::fetchTasksFromServer, 500);
    }
}
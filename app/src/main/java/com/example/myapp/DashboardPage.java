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
    private TextView totalTasksTextView;
    private TextView completedTasksTextView;
    private TextView pendingTasksTextView;
    private TextView onHoldTasksTextView;
    private RecyclerView tasksRecyclerView;
    private TaskAdapter taskAdapter;
    private Button createTaskButton;
    private Button applyFiltersButton;
    private List<Task> allTasks;

    private static final String DATE_FORMAT_API = "yyyy-MM-dd";
    private SimpleDateFormat apiDateFormat;
    private static final String BASE_URL = "http://172.16.20.92:8000";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard_page);

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
    }

    private void navigateToCreateTask() {
        Intent intent = new Intent(DashboardPage.this, TaskPageCreate.class);
        startActivity(intent);
    }

    private void showFiltersDialog() {
        Toast.makeText(this, "Filter functionality coming soon!", Toast.LENGTH_SHORT).show();
    }

    private String getAccessToken() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        return sharedPreferences.getString("access_token", null);
    }

    private void fetchTasksFromServer() {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(BASE_URL + "/api/tasks/list/");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Authorization", "Bearer " + getAccessToken());

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

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
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Failed to fetch tasks: " + responseCode,
                                    Toast.LENGTH_SHORT).show()
                    );
                }
            } catch (Exception e) {
                e.printStackTrace();
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
                Log.e("DateParse", "Error parsing deadline", e);
            }
        }

        return new Task(id, status, priority, dueDate, title, description);
    }

    private void updateUIWithTasks(List<Task> tasks) {
        Log.d("DashboardPage", "Updating UI with tasks: " + tasks.size());

        int totalTasks = tasks.size();
        int completedTasks = 0;
        int pendingTasks = 0;
        int onHoldTasks = 0;

        for (Task task : tasks) {
            Log.d("DashboardPage", "Task: " + task.getTitle() + ", Status: " + task.getStatus());

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
                    Log.w("DashboardPage", "Unknown status: " + task.getStatus());
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
            Log.d("DashboardPage", "RecyclerView data updated");
        });
    }

    @Override
    public void onDeleteTask(Task task) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(BASE_URL + "/api/tasks/delete/" + task.getId() + "/");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("DELETE");
                connection.setRequestProperty("Authorization", "Bearer " + getAccessToken());

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
                    runOnUiThread(() ->
                            Toast.makeText(this, "Failed to delete task: " + responseCode,
                                    Toast.LENGTH_SHORT).show()
                    );
                }
            } catch (Exception e) {
                e.printStackTrace();
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
        Log.d("DashboardPage", "onResume called, fetching tasks");
        new Handler().postDelayed(this::fetchTasksFromServer, 1000);
    }
}
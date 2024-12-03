package com.example.myapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UserTaskActivity extends AppCompatActivity implements TaskAdapter.OnTaskActionListener {
    private RecyclerView rc;
    private TaskAdapter ad;
    private TextView tx;
    private ImageView backArrow;
    private SharedPreferences sp;
    private final String DASHBOARD_URL = "http://172.16.20.76:8000/api/superuser/dashboard/";

    @Override
    protected void onCreate(Bundle si) {
        super.onCreate(si);
        setContentView(R.layout.activity_admin_task_list);

        int userId = getIntent().getIntExtra("user_id", -1);
        String username = getIntent().getStringExtra("username");

        if (userId == -1 || username == null) {
            Toast.makeText(this, "No users found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        rc = findViewById(R.id.user_tasks_recycler_view);
        tx = findViewById(R.id.user_tasks_title);
        backArrow = findViewById(R.id.back_arrow);
        backArrow.setOnClickListener(v -> finish());
        sp = getSharedPreferences("AdminPrefs", MODE_PRIVATE);
        tx.setText(username + "'s Tasks");
        rc.setLayoutManager(new LinearLayoutManager(this));
        ad = new TaskAdapter(this);
        rc.setAdapter(ad);
        fetchUserTasks(username);
    }

    @Override
    public void onDeleteTask(Task task) {
        Toast.makeText(this, "Task deletion not allowed in view mode",
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onEditTask(Task task) {
        Toast.makeText(this, "Task editing not allowed in view mode",
                Toast.LENGTH_SHORT).show();
    }

    private void fetchUserTasks(String username) {
        String accessToken = sp.getString("admin_access_token", "");

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                DASHBOARD_URL,
                null,
                response -> {
                    try {
                        if (response.getString("status").equals("success")) {
                            JSONObject data = response.getJSONObject("data");
                            JSONArray allTasks = data.getJSONArray("all_tasks");
                            List<Task> taskList = new ArrayList<>();
                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                            for (int i = 0; i < allTasks.length(); i++) {
                                JSONObject taskObj = allTasks.getJSONObject(i);
                                if (taskObj.getString("user").equals(username)) {
                                    Date dueDate = null;
                                    try {
                                        dueDate = dateFormat.parse(taskObj.getString("deadline"));
                                    } catch (ParseException e) {
                                        e.printStackTrace();
                                    }

                                    Task task = new Task(
                                            taskObj.getInt("id"),
                                            taskObj.getString("status"),
                                            taskObj.getString("priority"),
                                            dueDate,
                                            taskObj.getString("title"),
                                            taskObj.getString("description")
                                    );
                                    taskList.add(task);
                                }
                            }

                            if (taskList.isEmpty()) {
                                Toast.makeText(this, "No tasks for this user",
                                        Toast.LENGTH_SHORT).show();
                            }
                            ad.setTasks(taskList);
                        } else {
                            Toast.makeText(this, "Failed to load tasks",
                                    Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(this, "Error parsing tasks: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    if (error.networkResponse != null && error.networkResponse.statusCode == 401) {
                        finish();
                    } else {
                        Toast.makeText(this, "Error fetching tasks: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + accessToken);
                return headers;
            }
        };
        Volley.newRequestQueue(this).add(req);
    }
}
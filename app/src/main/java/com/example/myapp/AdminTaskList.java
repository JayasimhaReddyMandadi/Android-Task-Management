package com.example.myapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
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

public class AdminTaskList extends AppCompatActivity implements TaskAdapter.OnTaskActionListener {
    private RecyclerView rc;
    private TaskAdapter ad;
    private final String Task_List_Url="http://172.16.20.92:8000/api/admin/dashboard/";
    private SharedPreferences sp;
    private ImageView backArrow;

    @Override
    protected void onCreate(Bundle si){
        super.onCreate(si);
        setContentView(R.layout.activity_admin_task_list);

        backArrow=findViewById(R.id.back_arrow);
        backArrow.setOnClickListener(v->{
            Intent i=new Intent(AdminTaskList.this,AdminDashBoardActivity.class);
            startActivity(i);
            finish();
        });

        rc=findViewById(R.id.task_recycler_view);
        rc.setLayoutManager(new LinearLayoutManager(this));

        sp=getApplicationContext().getSharedPreferences("AdminPrefs",MODE_PRIVATE);
        ad=new TaskAdapter(this);
        rc.setAdapter(ad);
        fetchTasks();
    }

    private void fetchTasks(){
        String accessToken=sp.getString("admin_access_token","");
        JsonObjectRequest req=new JsonObjectRequest(
                Request.Method.GET,
                Task_List_Url,
                null,
                response -> {
                    try {
                        if(response.getString("status").equals("success")){
                            JSONObject data=response.getJSONObject("data");
                            JSONArray ja=data.getJSONArray("all_tasks");
                            List<Task> tl=new ArrayList<>();

                            SimpleDateFormat d=new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                            for(int i=0;i<ja.length();i++) {
                                JSONObject jb = ja.getJSONObject(i);
                                Date dueDate = null;
                                Date createdAt = null;
                                try {
                                    dueDate = d.parse(jb.getString("deadline"));
                                    createdAt = d.parse(jb.getString("created_at").split("T")[0]);
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                                Task t = new Task(
                                        jb.getInt("id"),
                                        jb.getString("title"),
                                        jb.getString("description"),
                                        jb.getString("status"),
                                        jb.getString("priority"),
                                        dueDate,
                                        createdAt,
                                        jb.getString("user")
                                );
                                tl.add(t);
                            }
                            ad.setTasks(tl);
                        } else {
                            Toast.makeText(AdminTaskList.this,
                                    "Failed to fetch tasks", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(AdminTaskList.this,
                                "Error parsing tasks: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    if (error.networkResponse != null && error.networkResponse.statusCode == 401) {
                        redirectToLogin();
                    } else {
                        Toast.makeText(AdminTaskList.this,
                                "Error fetching tasks: " + error.getMessage(), Toast.LENGTH_SHORT).show();
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

    private void redirectToLogin() {
        SharedPreferences.Editor editor = sp.edit();
        editor.clear();
        editor.apply();

        Intent intent = new Intent(this, AdminLoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onDeleteTask(Task task) {
        // Implement delete functionality
    }

    @Override
    public void onEditTask(Task task) {
        // Implement edit functionality
    }
}
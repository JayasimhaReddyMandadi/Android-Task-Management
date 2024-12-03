package com.example.myapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Image;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class UsersList extends AppCompatActivity implements UserAdapter.OnUserClickListener {
    private RecyclerView rv;
    private UserAdapter ad;
    private List<User> ul;
    private final String User_List_Url="http://172.16.20.92:8000/api/admin/dashboard/";
    private SharedPreferences sp;
    private ImageView backArrow;

    @Override
    protected void onCreate(Bundle si){
        super.onCreate(si);
        setContentView(R.layout.activity_admin_user_list);

        backArrow = findViewById(R.id.back_arrow);
        backArrow.setOnClickListener(v -> {
            finish();
        });

        rv=findViewById(R.id.user_recycler_view);
        rv.setLayoutManager(new LinearLayoutManager(this));

        sp=getApplicationContext().getSharedPreferences("AdminPrefs",MODE_PRIVATE);

        ul=new ArrayList<>();
        ad=new UserAdapter(this,ul,this);
        rv.setAdapter(ad);
        fetchUsers();
    }
    @Override
    public void onUserClick(User user) {
        // Navigate to UserTasksActivity with the selected user's ID
        Intent intent = new Intent(this, UserTaskActivity.class);
        intent.putExtra("user_id", user.getId());
        intent.putExtra("username", user.getUsername());
        startActivity(intent);
    }
    private void fetchUsers(){
        String accessToken=sp.getString("admin_access_token","");
        JsonObjectRequest req=new JsonObjectRequest(
                Request.Method.GET,
                User_List_Url,
                null,
                response -> {
                    try {
                        if(response.getString("status").equals("success")){
                            JSONObject data=response.getJSONObject("data");
                            JSONArray users=data.getJSONArray("users");
                            ul.clear();

                            for(int i=0;i< users.length();i++){
                                JSONObject userob=users.getJSONObject(i);
                                User user=new User();
                                user.setId(userob.getInt("id"));
                                user.setUsername(userob.getString("username"));
                                user.setEmail(userob.getString("email"));
                                ul.add(user);
                            }
                            ad.notifyDataSetChanged();
                        }else {
                            Toast.makeText(this,"failed to fetch users",Toast.LENGTH_SHORT).show();
                        }
                    }catch (JSONException e){
                        Toast.makeText(this,"Error Parsing users: "+e.getMessage(),Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    if(error.networkResponse!=null && error.networkResponse.statusCode==401){
                        redirectToLogin();
                    }else {
                        Toast.makeText(this,"Error fetching users: "+error.getMessage(),Toast.LENGTH_SHORT).show();
                    }
                }
        ){
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
}
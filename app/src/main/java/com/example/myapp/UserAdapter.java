package com.example.myapp;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
    private List<User> users;
    private Context context;
    private OnUserClickListener listener;

    public interface OnUserClickListener {
        void onUserClick(User user);
    }
    public UserAdapter(Context context, List<User> users, OnUserClickListener listener) {
        this.context = context;
        this.users = users;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.user_item, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = users.get(position);
        holder.bind(user);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    class UserViewHolder extends RecyclerView.ViewHolder {
        TextView usernameText;
        TextView emailText;

        UserViewHolder(View itemView) {
            super(itemView);
            usernameText = itemView.findViewById(R.id.user_item_username);
            emailText = itemView.findViewById(R.id.user_item_email);
        }

        void bind(User user) {
            usernameText.setText("Username: " + user.getUsername());
            emailText.setText("Email: " + user.getEmail());

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUserClick(user);
                } else {
                    // Fallback to direct intent if no listener is set
                    Intent intent = new Intent(context, UserTaskActivity.class);
                    intent.putExtra("user_id", user.getId());
                    intent.putExtra("username", user.getUsername());
                    intent.putExtra("email", user.getEmail());
                    context.startActivity(intent);
                }
            });
        }
    }
    public void updateUsers(List<User> newUsers) {
        this.users = newUsers;
        notifyDataSetChanged();
    }
}
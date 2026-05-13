package com.hm.synapse;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<UserEntity> users;
    private OnUserDeleteListener listener;

    public interface OnUserDeleteListener {
        void onUserDelete(UserEntity user);
    }

    public UserAdapter(List<UserEntity> users, OnUserDeleteListener listener) {
        this.users = users;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserEntity user = users.get(position);
        holder.email.setText(user.getEmail());
        holder.role.setText("Role: " + user.getRole());
        holder.deleteBtn.setOnClickListener(v -> listener.onUserDelete(user));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView email, role;
        ImageButton deleteBtn;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            email = itemView.findViewById(R.id.tv_user_email);
            role = itemView.findViewById(R.id.tv_user_role);
            deleteBtn = itemView.findViewById(R.id.btn_delete_user);
        }
    }
}

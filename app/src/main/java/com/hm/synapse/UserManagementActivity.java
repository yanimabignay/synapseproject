package com.hm.synapse;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.hm.synapse.databinding.ActivityUserManagementBinding;

import java.util.ArrayList;
import java.util.concurrent.Executors;

public class UserManagementActivity extends AppCompatActivity implements UserAdapter.OnUserDeleteListener {

    private ActivityUserManagementBinding binding;
    private UserAdapter adapter;
    private SynapseDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserManagementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        database = SynapseDatabase.getDatabase(this);

        adapter = new UserAdapter(new ArrayList<>(), this);
        binding.userRecycler.setLayoutManager(new LinearLayoutManager(this));
        binding.userRecycler.setAdapter(adapter);

        // Observe all users from SQLite
        database.userDao().getAllUsers().observe(this, users -> {
            adapter = new UserAdapter(users, this);
            binding.userRecycler.setAdapter(adapter);
        });
    }

    @Override
    public void onUserDelete(UserEntity user) {
        // Confirmation could be added here
        Executors.newSingleThreadExecutor().execute(() -> {
            database.userDao().delete(user);
            runOnUiThread(() -> Toast.makeText(this, "User deleted: " + user.getEmail(), Toast.LENGTH_SHORT).show());
        });
    }
}

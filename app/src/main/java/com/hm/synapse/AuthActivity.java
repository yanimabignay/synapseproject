package com.hm.synapse;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.hm.synapse.databinding.ActivityAuthBinding;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuthActivity extends AppCompatActivity {

    private ActivityAuthBinding binding;
    private SynapseDatabase database;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAuthBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        database = SynapseDatabase.getDatabase(this);
        prefs = getSharedPreferences("SynapsePrefs", MODE_PRIVATE);

        // Session Persistence: Stay logged in
        if (prefs.getString("current_user_id", null) != null) {
            proceedToMain();
        }

        binding.tvSignupLink.setOnClickListener(v -> startActivity(new Intent(this, SignUpActivity.class)));

        binding.btnAuth.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Neural identity required", Toast.LENGTH_SHORT).show();
                return;
            }

            verifyUserAndChallenge2FA(email);
        });
    }

    private void verifyUserAndChallenge2FA(String email) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            UserEntity user = database.userDao().getUserByEmail(email);
            
            runOnUiThread(() -> {
                if (user != null) {
                    Intent intent = new Intent(this, TwoFactorActivity.class);
                    intent.putExtra("USER_EMAIL", email);
                    intent.putExtra("USER_ID", user.getId());
                    intent.putExtra("IS_SIGNUP", false);
                    startActivity(intent);
                    finish();
                } else {
                    // Improved error message to guide user to Sign Up
                    Toast.makeText(this, "Identity not found. Click 'Create a workspace' below.", Toast.LENGTH_LONG).show();
                }
            });
            executor.shutdown();
        });
    }

    private void proceedToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}

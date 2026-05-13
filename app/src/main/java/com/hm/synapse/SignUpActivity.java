package com.hm.synapse;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.hm.synapse.databinding.ActivitySignupBinding;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SignUpActivity extends AppCompatActivity {

    private ActivitySignupBinding binding;
    private SynapseDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        database = SynapseDatabase.getDatabase(this);

        binding.btnCreateAccount.setOnClickListener(v -> {
            String name = binding.etFullName.getText().toString().trim();
            String email = binding.etSignupEmail.getText().toString().trim();
            String password = binding.etSignupPassword.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if user already exists before proceeding to 2FA
            checkExistingAndProceed(email, name);
        });

        binding.btnBackToLogin.setOnClickListener(v -> finish());
    }

    private void checkExistingAndProceed(String email, String fullName) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            UserEntity existing = database.userDao().getUserByEmail(email);
            if (existing != null) {
                runOnUiThread(() -> Toast.makeText(this, "User already exists", Toast.LENGTH_SHORT).show());
                executor.shutdown();
                return;
            }

            runOnUiThread(() -> {
                // Do NOT save to database yet. Pass data to 2FA activity.
                Intent intent = new Intent(SignUpActivity.this, TwoFactorActivity.class);
                intent.putExtra("USER_EMAIL", email);
                intent.putExtra("USER_NAME", fullName);
                intent.putExtra("IS_SIGNUP", true);
                startActivity(intent);
                finish();
            });
            executor.shutdown();
        });
    }
}

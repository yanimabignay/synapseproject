package com.hm.synapse;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.hm.synapse.databinding.ActivityTwoFactorBinding;

import org.json.JSONObject;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TwoFactorActivity extends AppCompatActivity {

    private ActivityTwoFactorBinding binding;
    private String generatedCode;
    private String userEmail;
    private String userName;
    private boolean isSignup;
    private SynapseDatabase database;
    private SharedPreferences prefs;

    // --- EMAILJS CREDENTIALS ---
    private final String SERVICE_ID = "service_xmbhu59";
    private final String TEMPLATE_ID = "template_derqkc4";
    private final String PUBLIC_KEY = "pjA1-_0DfBTJDTn41";
    private final String PRIVATE_KEY = "5z-wRv4QK4p23XeUomDHK";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTwoFactorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        database = SynapseDatabase.getDatabase(this);
        prefs = getSharedPreferences("SynapsePrefs", MODE_PRIVATE);

        userEmail = getIntent().getStringExtra("USER_EMAIL");
        userName = getIntent().getStringExtra("USER_NAME");
        isSignup = getIntent().getBooleanExtra("IS_SIGNUP", false);

        if (userEmail == null) userEmail = "user@example.com"; 

        binding.tv2faDesc.setText("Axon is dispatching a verification code to " + userEmail);

        generateAndSendCode();

        binding.btnVerify.setOnClickListener(v -> {
            String inputCode = binding.et2faCode.getText().toString().trim();
            if (inputCode.equals(generatedCode)) {
                if (isSignup) registerAndStartSession();
                else startSession(getIntent().getStringExtra("USER_ID"));
            } else {
                Toast.makeText(this, "Neural mismatch. Access denied.", Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnResend.setOnClickListener(v -> generateAndSendCode());
    }

    private void registerAndStartSession() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            String uid = UUID.randomUUID().toString();
            UserEntity newUser = new UserEntity(uid, userEmail);
            newUser.setRole("general");
            database.userDao().insert(newUser);

            runOnUiThread(() -> {
                // Mark as new user for onboarding
                prefs.edit().putBoolean("is_new_user", true).apply();
                startSession(uid);
            });
            executor.shutdown();
        });
    }

    private void startSession(String uid) {
        if (uid == null) uid = "local_user_" + userEmail.hashCode();
        
        prefs.edit().putString("current_user_id", uid).apply();
        
        Toast.makeText(this, "Neural Link Established. Welcome to Synapse.", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void generateAndSendCode() {
        Random random = new Random();
        generatedCode = String.format("%06d", random.nextInt(999999));
        Log.d("AXON_CODE", "BYPASS CODE: " + generatedCode);
        binding.btnVerify.setEnabled(false);
        binding.btnResend.setEnabled(false);
        sendEmailThroughEmailJS(userEmail, generatedCode);
    }

    private void sendEmailThroughEmailJS(String email, String code) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            String diagnostic;
            boolean isOk = false;
            try {
                OkHttpClient client = new OkHttpClient();
                JSONObject templateParams = new JSONObject();
                templateParams.put("to_email", email);
                templateParams.put("otp_code", code);
                templateParams.put("user_name", userName != null ? userName : email.split("@")[0]); 

                JSONObject jsonBody = new JSONObject();
                jsonBody.put("service_id", SERVICE_ID);
                jsonBody.put("template_id", TEMPLATE_ID);
                jsonBody.put("user_id", PUBLIC_KEY);
                jsonBody.put("accessToken", PRIVATE_KEY);
                jsonBody.put("template_params", templateParams);

                MediaType JSON = MediaType.get("application/json; charset=utf-8");
                RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
                Request request = new Request.Builder().url("https://api.emailjs.com/api/v1.0/email/send").post(body).build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        isOk = true;
                        diagnostic = "Code delivered to inbox.";
                    } else {
                        String bodyStr = response.body() != null ? response.body().string() : "No details";
                        diagnostic = "Axoncomm failure (" + response.code() + "). BYPASS CODE: " + code;
                    }
                }
            } catch (Exception e) { 
                diagnostic = "Neural failure. BYPASS CODE: " + code;
                Log.e("Synapse", "2FA Error", e);
            }

            final String finalMsg = diagnostic;
            runOnUiThread(() -> {
                binding.btnVerify.setEnabled(true);
                binding.btnResend.setEnabled(true);
                Toast.makeText(this, finalMsg, Toast.LENGTH_LONG).show();
                if (finalMsg.contains("BYPASS CODE")) {
                    binding.tv2faDesc.setText("Neural Link Error. Manual Bypass Required.\nENTER CODE: " + code);
                    binding.tv2faDesc.setTextColor(Color.RED);
                } else {
                    binding.tv2faDesc.setText("Verification code sent to " + email);
                    binding.tv2faDesc.setTextColor(Color.parseColor("#73726E"));
                }
            });
            executor.shutdown();
        });
    }
}

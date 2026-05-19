package com.hm.synapse;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.hm.synapse.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private MainAdapter adapter;
    private List<SynapseBlockEntity> allBlocks = new ArrayList<>();
    private final List<SynapseBlockEntity> filteredBlocks = new ArrayList<>();
    private SynapseDao synapseDao;
    private UserDao userDao;
    private UserEntity currentUser;
    private String currentUserId;
    private String currentTab = "tasks";
    private int stalenessThreshold = 5;
    private TextToSpeech tts;
    private GenerativeModelFutures axonModel;
    private SharedPreferences prefs;

    // Onboarding variables
    private PopupWindow onboardingPopup;
    private int onboardingStep = 0;
    private View highlightedView;
    private float originalElevation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("SynapsePrefs", MODE_PRIVATE);
        currentUserId = prefs.getString("current_user_id", null);
        
        if (currentUserId == null) {
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return;
        }

        applySavedTheme();
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        currentTab = prefs.getString("current_tab", "tasks");
        stalenessThreshold = prefs.getInt("staleness_threshold", 5);

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNav, (v, insets) -> {
            Insets navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            binding.bottomNav.setPadding(0, 0, 0, navBars.bottom + 42); 
            return insets;
        });

        SynapseDatabase db = SynapseDatabase.getDatabase(this);
        synapseDao = db.synapseDao();
        userDao = db.userDao();
        
        setupAxonAI();
        setupRecyclerView();
        setupNavigation();
        setupTouchHelper();
        setupTTS();
        loadUserData();
        loadBlocks();

        binding.fabAdd.setOnClickListener(v -> showAddBlockDialog());
        binding.btnAskAi.setOnClickListener(v -> {
            currentTab = "ai";
            updateUIForTab();
        });
        binding.btnLogout.setOnClickListener(v -> logout());
        
        binding.btnManageUsers.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, UserManagementActivity.class));
        });

        setupSettingsLogic();

        if (prefs.getBoolean("is_new_user", false)) {
            binding.getRoot().post(this::startOnboarding);
        }
    }

    private void startOnboarding() {
        onboardingStep = 0;
        binding.blurOverlay.setVisibility(View.VISIBLE);
        currentTab ="tasks";
        binding.bottomNav.setSelectedItemId(R.id.nav_tasks);
        updateUIForTab();
        showOnboardingStep();
    }

    private void resetHighlight() {
        if (highlightedView != null) {
            highlightedView.setElevation(originalElevation);
            highlightedView = null;
        }
        binding.header.setElevation(0f);
    }

    private void showOnboardingStep() {
        if (onboardingPopup != null && onboardingPopup.isShowing()) {
            onboardingPopup.dismiss();
        }
        resetHighlight();

        binding.blurOverlay.setVisibility(View.VISIBLE);

        if (isFinishing() || isDestroyed()) return;

        View popupView = LayoutInflater.from(this).inflate(R.layout.layout_onboarding_bubble, null);
        onboardingPopup = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, false);

        LinearLayout bubbleRoot = popupView.findViewById(R.id.bubble_root);
        TextView title = popupView.findViewById(R.id.bubble_title);
        TextView desc = popupView.findViewById(R.id.bubble_desc);
        Button btnNext = popupView.findViewById(R.id.btn_next_onboarding);
        Button btnSkip = popupView.findViewById(R.id.btn_skip_onboarding);

        View anchor;
        float density = getResources().getDisplayMetrics().density;
        int xAdjustment = 0;
        int yOffset;
        int bgResId;
        int verticalGap = (int) (14 * density);

        switch (onboardingStep) {
            case 0:
                title.setText("Workspace Assistant");
                desc.setText("Consult your Assistant for workspace analysis or workflow suggestions.");
                anchor = binding.btnAskAi;
                bgResId = R.drawable.bg_speech_bubble_tr;
                xAdjustment = (int) (120 * density); 
                break;
            case 1:
                title.setText("Synapse Core");
                desc.setText("This tracks your efficiency level. Complete tasks to unlock advanced features.");
                anchor = binding.levelText;
                bgResId = R.drawable.bg_speech_bubble_tl;
                break;
            case 2:
                title.setText("Element Creation");
                desc.setText("Tap the \u0027+\u0027 button to add headers, notes, tasks, or finance records.");
                anchor = binding.fabAdd;
                bgResId = R.drawable.bg_speech_bubble_br;
                xAdjustment = (int) (270 * density);
                verticalGap = (int) (40 * density);
                break;
            case 3:
                title.setText("System Navigation");
                desc.setText("Efficiently switch between your Hub, Board, Finance, and System Configuration.");
                anchor = binding.bottomNav;
                bgResId = R.drawable.bg_speech_bubble_bl;
                btnNext.setText("Finish");
                xAdjustment = (int) (-100 * density);
                verticalGap = (int) (15 * density);
                break;
            default:
                prefs.edit().putBoolean("is_new_user", false).apply();
                binding.blurOverlay.setVisibility(View.GONE);
                return;
        }

        if (anchor != null) {
            highlightedView = anchor;
            originalElevation = anchor.getElevation();
            anchor.setElevation(20 * density);
            if (onboardingStep == 0 || onboardingStep == 1) {
                binding.header.setElevation(20 * density);
            }
        }

        bubbleRoot.setBackgroundResource(bgResId);
        int cardBgColor = ContextCompat.getColor(this, R.color.card_bg);
        if (bubbleRoot.getBackground() != null) {
            bubbleRoot.getBackground().setTint(cardBgColor);
        }

        int pSide = (int) (24 * density);
        int pBase = (int) (22 * density);
        int pTail = (int) (12 * density); 
        
        if (bgResId == R.drawable.bg_speech_bubble_tl || bgResId == R.drawable.bg_speech_bubble_tr) {
            bubbleRoot.setPadding(pSide, pBase + pTail, pSide, pBase);
        } else {
            bubbleRoot.setPadding(pSide, pBase, pSide, pBase + pTail);
        }

        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int bubbleWidth = popupView.getMeasuredWidth();
        int bubbleHeight = popupView.getMeasuredHeight();

        if (onboardingStep >= 2) {
            yOffset = -bubbleHeight - verticalGap;
        } else {
            yOffset = (anchor != null ? anchor.getHeight() : 0) + verticalGap;
        }

        btnNext.setOnClickListener(v -> {
            onboardingStep++;
            showOnboardingStep();
        });

        btnSkip.setOnClickListener(v -> {
            prefs.edit().putBoolean("is_new_user", false).apply();
            onboardingPopup.dismiss();
            binding.blurOverlay.setVisibility(View.GONE);
            resetHighlight();
        });

        if (anchor != null) {
            int[] location = new int[2];
            anchor.getLocationInWindow(location);
            float tailRelativeX = (bgResId == R.drawable.bg_speech_bubble_tl || bgResId == R.drawable.bg_speech_bubble_bl) ? 35f / 200f : 165f / 200f;
            int xOffset = (int) (anchor.getWidth() / 2f - bubbleWidth * tailRelativeX) + xAdjustment;
            onboardingPopup.showAtLocation(binding.getRoot(), Gravity.TOP | Gravity.START, location[0] + xOffset, location[1] + yOffset);
        }
    }

    private void loadUserData() {
        userDao.getUser(currentUserId).observe(this, user -> {
            if (user != null) {
                currentUser = user;
                updateLevelUI();
            } else {
                Executors.newSingleThreadExecutor().execute(() -> userDao.insert(new UserEntity(currentUserId, "Explorer")));
            }
        });
    }

    private void updateLevelUI() {
        if (currentUser == null) return;
        int exp = currentUser.getExp();
        int level = currentUser.getLevel();
        int progress = (exp % 1000) / 10;
        String levelName;
        if (level == 1) levelName = getString(R.string.lvl1_name);
        else if (level == 2) levelName = getString(R.string.lvl2_name);
        else if (level == 3) levelName = getString(R.string.lvl3_name);
        else if (level == 4) levelName = getString(R.string.lvl4_name);
        else levelName = getString(R.string.lvl5_name);
        binding.expProgress.setProgress(progress);
        binding.levelText.setText(String.format(Locale.getDefault(), "Lvl %d %s", level, levelName));
        if (level >= 5) binding.txtRewardStatus.setText("Templates: UNLOCKED");
        else binding.txtRewardStatus.setText("Unlock Templates at Level 5");
    }

    private void setupAxonAI() {
        String apiKey = "AIzaSyAYpd3V1N1t5WnrPfVvp4ozDivMzGX7HMA"; 
        try {
            GenerativeModel gm = new GenerativeModel("gemini-3.1-flash-lite-preview", apiKey);
            axonModel = GenerativeModelFutures.from(gm);
        } catch (Exception e) {
            Log.e("Axon", "AI Init Failed");
        }
        binding.btnSendAxon.setOnClickListener(v -> {
            String prompt = binding.axonInput.getText().toString();
            if (!prompt.isEmpty()) askAxon(prompt);
        });
    }

    private void askAxon(String prompt) {
        binding.axonResponseText.setText("Assistant is processing query...");
        binding.axonInput.setText("");
        Content content = new Content.Builder().addText(prompt).build();
        ListenableFuture<GenerateContentResponse> response = axonModel.generateContent(content);
        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String text = result.getText();
                runOnUiThread(() -> {
                    binding.axonResponseText.setText(text);
                    if (tts != null) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
                });
            }
            @Override
            public void onFailure(Throwable t) {
                runOnUiThread(() -> binding.axonResponseText.setText("Query synchronization failed. Please retry."));
            }
        }, Executors.newSingleThreadExecutor());
    }

    private void setupRecyclerView() {
        adapter = new MainAdapter(filteredBlocks, new MainAdapter.OnBlockInteractionListener() {
            @Override
            public void onFinanceEdit(SynapseBlockEntity block) { showFinanceEditDialog(block); }
            @Override
            public void onContentChanged(SynapseBlockEntity block) { 
                Executors.newSingleThreadExecutor().execute(() -> synapseDao.update(block)); 
            }
            @Override
            public void onStatusChanged(SynapseBlockEntity block) { 
                Executors.newSingleThreadExecutor().execute(() -> synapseDao.update(block)); 
                if (block.isCompleted()) addExp(50);
            }
            @Override
            public void onDeleteBlock(SynapseBlockEntity block) {
                Executors.newSingleThreadExecutor().execute(() -> synapseDao.delete(block));
            }
        });
        binding.taskRecycler.setLayoutManager(new LinearLayoutManager(this));
        binding.taskRecycler.setAdapter(adapter);
    }

    private void setupNavigation() {
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_tasks) currentTab = "tasks";
            else if (id == R.id.nav_finance) currentTab = "finance";
            else if (id == R.id.nav_kanban) currentTab = "kanban";
            else if (id == R.id.nav_templates) currentTab = "templates";
            else if (id == R.id.nav_settings) currentTab = "settings";
            prefs.edit().putString("current_tab", currentTab).apply();
            updateUIForTab();
            return true;
        });
        updateUIForTab();
    }

    private void updateUIForTab() {
        binding.taskRecycler.setVisibility((currentTab.equals("tasks") || currentTab.equals("finance") || currentTab.equals("kanban")) ? View.VISIBLE : View.GONE);
        binding.axonAiContainer.setVisibility(currentTab.equals("ai") ? View.VISIBLE : View.GONE);
        binding.templatesContainer.setVisibility(currentTab.equals("templates") ? View.VISIBLE : View.GONE);
        binding.settingsContainer.setVisibility(currentTab.equals("settings") ? View.VISIBLE : View.GONE);
        binding.financeSummaryCard.setVisibility(currentTab.equals("finance") ? View.VISIBLE : View.GONE);
        binding.fabAdd.setVisibility((currentTab.equals("settings") || currentTab.equals("ai") || currentTab.equals("templates")) ? View.GONE : View.VISIBLE);
        
        if (currentTab.equals("finance")) binding.workspaceTitle.setText(R.string.finance_ledger);
        else if (currentTab.equals("kanban")) binding.workspaceTitle.setText(R.string.workflow_board);
        else if (currentTab.equals("settings")) binding.workspaceTitle.setText(R.string.system_config);
        else if (currentTab.equals("ai")) binding.workspaceTitle.setText("Assistant Console");
        else if (currentTab.equals("templates")) binding.workspaceTitle.setText(R.string.nav_templates);
        else binding.workspaceTitle.setText(R.string.axon_hub);
        
        filterBlocks();
        calculateFinanceTotal();
    }

    private void filterBlocks() {
        filteredBlocks.clear();
        for (SynapseBlockEntity b : allBlocks) {
            if (currentTab.equals("tasks") && (b.getType().equals("TODO") || b.getType().equals("HEADER") || b.getType().equals("TEXT"))) filteredBlocks.add(b);
            else if (currentTab.equals("finance") && b.getType().equals("FINANCE")) filteredBlocks.add(b);
            else if (currentTab.equals("kanban") && b.getType().equals("TODO")) filteredBlocks.add(b);
        }
        adapter.notifyDataSetChanged();
    }

    private void loadBlocks() {
        synapseDao.getAllBlocks(currentUserId).observe(this, blocks -> {
            allBlocks = blocks;
            filterBlocks();
            calculateFinanceTotal();
            checkBurnout();
        });
    }

    private void calculateFinanceTotal() {
        double total = 0;
        for (SynapseBlockEntity b : allBlocks) {
            if (b.getType().equals("FINANCE")) total += b.getAmount();
        }
        binding.tvFinanceTotal.setText(String.format(Locale.getDefault(), "$%.2f", total));
    }

    private void checkBurnout() {
        int pendingTasks = 0;
        for (SynapseBlockEntity b : allBlocks) {
            if (b.getType().equals("TODO") && !b.isCompleted()) pendingTasks++;
        }
        if (pendingTasks > 7) {
            binding.burnoutAlert.setVisibility(View.VISIBLE);
            binding.txtAffirmation.setText(String.format(Locale.getDefault(), "High Workload Detected: %d tasks pending.", pendingTasks));
        } else binding.burnoutAlert.setVisibility(View.GONE);
    }

    private void showAddBlockDialog() {
        String[] types = {"Header", "Note", "Task", "Finance Entry"};
        new AlertDialog.Builder(this)
                .setTitle("Insert Workspace Component")
                .setItems(types, (dialog, which) -> {
                    String type = "HEADER";
                    if (which == 1) type = "TEXT";
                    else if (which == 2) type = "TODO";
                    else if (which == 3) type = "FINANCE";
                    final String finalType = type;
                    Executors.newSingleThreadExecutor().execute(() -> {
                        SynapseBlockEntity block = new SynapseBlockEntity(UUID.randomUUID().toString(), currentUserId, finalType, "");
                        synapseDao.insert(block);
                    });
                })
                .show();
    }

    private void showFinanceEditDialog(SynapseBlockEntity block) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_finance_edit, null);
        EditText etDesc = v.findViewById(R.id.et_finance_desc);
        EditText etAmount = v.findViewById(R.id.et_finance_amount);
        RadioGroup rdoType = v.findViewById(R.id.rdo_finance_type);
        
        etDesc.setText(block.getContent());
        etAmount.setText(String.valueOf(Math.abs(block.getAmount())));
        
        new AlertDialog.Builder(this)
                .setTitle("Process Finance Data")
                .setView(v)
                .setPositiveButton("Sync", (dialog, which) -> {
                    String content = etDesc.getText().toString();
                    double amount = 0;
                    try {
                        amount = Double.parseDouble(etAmount.getText().toString());
                        if (rdoType.getCheckedRadioButtonId() == R.id.rdo_expense) amount *= -1;
                    } catch (Exception ignored) {}
                    
                    block.setContent(content);
                    block.setAmount(amount);
                    Executors.newSingleThreadExecutor().execute(() -> synapseDao.update(block));
                })
                .show();
    }

    private void setupTouchHelper() {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder target) { return false; }
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getBindingAdapterPosition();
                SynapseBlockEntity block = filteredBlocks.get(pos);
                Executors.newSingleThreadExecutor().execute(() -> synapseDao.delete(block));
            }
        };
        new ItemTouchHelper(callback).attachToRecyclerView(binding.taskRecycler);
    }

    private void setupSettingsLogic() {
        binding.themeToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                int mode = (checkedId == R.id.btnLightTheme) ? AppCompatDelegate.MODE_NIGHT_NO : (checkedId == R.id.btnDarkTheme ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                AppCompatDelegate.setDefaultNightMode(mode);
                prefs.edit().putInt("theme_mode", mode).apply();
            }
        });
        binding.rgStaleness.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_2days) stalenessThreshold = 2;
            else if (checkedId == R.id.rb_5days) stalenessThreshold = 5;
            else if (checkedId == R.id.rb_7days) stalenessThreshold = 7;
            prefs.edit().putInt("staleness_threshold", stalenessThreshold).apply();
        });
    }

    private void applySavedTheme() {
        AppCompatDelegate.setDefaultNightMode(prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM));
    }

    private void addExp(int amount) {
        Executors.newSingleThreadExecutor().execute(() -> {
            UserEntity user = userDao.getUserSync(currentUserId);
            if (user != null) {
                user.setExp(user.getExp() + amount);
                user.setLevel(1 + (user.getExp() / 1000));
                userDao.update(user);
            }
        });
    }

    private void setupTTS() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) tts.setLanguage(Locale.US);
        });
    }

    private void logout() {
        prefs.edit().remove("current_user_id").apply();
        startActivity(new Intent(this, AuthActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}

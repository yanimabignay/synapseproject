package com.hm.synapse;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncAdapterType;
import android.graphics.Color;
import android.os.Build;
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
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private Button dateButton;
    private DatePickerDialog datePickerDialog;
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
        binding.btnAskAi.setOnClickListener(v -> showAxonPromptDialog());
        binding.btnLogout.setOnClickListener(v -> logout());
        
        binding.btnManageUsers.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, UserManagementActivity.class));
        });

        setupSettingsLogic();

        // Check for first-time user onboarding
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
            if(highlightedView == binding.fabAdd){
                binding.fabAdd.setOnClickListener(v -> showAddBlockDialog());
            }
            highlightedView = null;
        }
        binding.header.setElevation(0f);
    }

    private void showOnboardingStep() {
        if (onboardingPopup != null && onboardingPopup.isShowing()) {
            onboardingPopup.dismiss();
        }
        resetHighlight();

        // Show the blur overlay immediately
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
                title.setText("Axon AI");
                desc.setText("Tap here to ask Axon for workspace analysis or creative suggestions.");
                anchor = binding.btnAskAi;
                bgResId = R.drawable.bg_speech_bubble_tr;
                xAdjustment = (int) (120 * density); 
                break;
            case 1:
                title.setText("Synapse Core");
                desc.setText("This shows your Neural Level. Complete tasks to gain EXP and unlock templates.");
                anchor = binding.levelText;
                bgResId = R.drawable.bg_speech_bubble_tl;
                break;
            case 2:
                title.setText("Create Neural Blocks");
                desc.setText("Tap the \u0027+\u0027 button to see the options");
                anchor = binding.fabAdd;
                bgResId = R.drawable.bg_speech_bubble_br;
                xAdjustment = (int) (270 * density);
                verticalGap = (int) (40 * density);

                anchor.setOnClickListener(v -> {
                    Dialog dialog = showAddBlockDialog();

                    onboardingStep = 3;
                    showOnboardingStep();
                });
                break;
            case 3:
                title.setText("The 4 Choices");
                desc.setText("Header for organization, Note for quick thoughts, To-do for tasks, Finance for tracking expenses.");
                anchor = binding.bottomNav;
                bgResId = R.drawable.bg_speech_bubble_bl;
                btnNext.setText("Finish");
                xAdjustment = (int) (-100 * density);
                verticalGap = (int) (200 * density);
                break;
            case 4:
                title.setText("Neural Navigation");
                desc.setText("Switch between Tasks, Finance, Kanban, and System Settings here.");
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

        // Highlight anchor area by lifting it above the blur overlay (elevation 9dp)
        if (anchor != null) {
            highlightedView = anchor;
            originalElevation = anchor.getElevation();
            anchor.setElevation(20 * density);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                anchor.setOutlineAmbientShadowColor(Color.TRANSPARENT);
                anchor.setOutlineSpotShadowColor(Color.TRANSPARENT);

                if (onboardingStep == 0 || onboardingStep == 1) {
                    binding.header.setOutlineAmbientShadowColor(Color.TRANSPARENT);
                    binding.header.setOutlineSpotShadowColor(Color.TRANSPARENT);
                }
            }
            // Fix: lift header for cases 0 and 1 because their anchors are nested inside the header
            if (onboardingStep == 0 || onboardingStep == 1) {
                binding.header.setElevation(20 * density);
            }
        }

        // Apply background
        bubbleRoot.setBackgroundResource(bgResId);
        int cardBgColor = ContextCompat.getColor(this, R.color.card_bg);

        if (bubbleRoot.getBackground() != null) {
            bubbleRoot.getBackground().setTint(cardBgColor);
        }

        // Consistent padding
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

        if (onboardingStep == 2 || onboardingStep == 3 || onboardingStep == 4) {
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

            float tailRelativeX;
            if (bgResId == R.drawable.bg_speech_bubble_tl || bgResId == R.drawable.bg_speech_bubble_bl) {
                tailRelativeX = 35f / 200f; 
            } else {
                tailRelativeX = 165f / 200f;
            }
            
            int xOffset = (int) (anchor.getWidth() / 2f - bubbleWidth * tailRelativeX) + xAdjustment;

            onboardingPopup.showAtLocation(binding.getRoot(), Gravity.TOP | Gravity.START,
                    location[0] + xOffset,
                    location[1] + yOffset);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("current_tab", currentTab);
    }

    private void loadUserData() {
        userDao.getUser(currentUserId).observe(this, user -> {
            if (user != null) {
                currentUser = user;
                updateLevelUI();
            } else {
                Executors.newSingleThreadExecutor().execute(() -> {
                    UserEntity newUser = new UserEntity(currentUserId, "Explorer");
                    userDao.insert(newUser);
                });
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
        else binding.txtRewardStatus.setText("Reach Lvl 5 for Custom Templates");

        updateInsights();
    }

    private void setupAxonAI() {
        String apiKey = BuildConfig.GEMINI_API_KEY;

        try {
            GenerativeModel gm = new GenerativeModel("gemini-3.1-flash-lite-preview", apiKey);
            axonModel = GenerativeModelFutures.from(gm);
        } catch (Exception e) {
            Log.e("Axon", "AI Init Failed");
        }
    }
    private void setupRecyclerView() {
        adapter = new MainAdapter(filteredBlocks, new MainAdapter.OnBlockInteractionListener() {
            @Override
            public void onFinanceEdit(SynapseBlockEntity block, MainAdapter.FinanceViewHolder view) {
                showFinanceEditDialog(block, view);
            }
            @Override
            public void onContentChanged(SynapseBlockEntity block) { 
                Executors.newSingleThreadExecutor().execute(() -> synapseDao.update(block)); 
            }
            @Override
            public void onStatusChanged(SynapseBlockEntity block) {
                SynapseBlockEntity foundBlock = null;
                for(SynapseBlockEntity b : allBlocks){
                    if(b.getId().equals(block.getId())) {
                        foundBlock = b;
                        break;
                    }
                }

                final SynapseBlockEntity existingInList = foundBlock;
                Log.d("debug", "block complete = " + block.isCompleted());

                final boolean isFirstTimeCompletion = block.isCompleted() &&
                        (!block.isFirstTimeCompleted());

                Log.d("debug", "first time = " + block.isFirstTimeCompleted());


                Executors.newSingleThreadExecutor().execute(() -> {
                    block.setCompleted(true);
                    block.setFirstTimeCompleted(true);

                    synapseDao.update(block);

                    runOnUiThread(() ->{
                        if(isFirstTimeCompletion && block.getType().equals("TODO")){
                            addExp(100);
                        }
                        updateInsights();
                        filterBlocks();
                    });
                });
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
            else if (id == R.id.nav_settings) currentTab = "settings";
            
            prefs.edit().putString("current_tab", currentTab).apply();
            updateUIForTab();
            return true;
        });
        
        // Set initial selection
        if (currentTab.equals("tasks")) binding.bottomNav.setSelectedItemId(R.id.nav_tasks);
        else if (currentTab.equals("finance")) binding.bottomNav.setSelectedItemId(R.id.nav_finance);
        else if (currentTab.equals("kanban")) binding.bottomNav.setSelectedItemId(R.id.nav_kanban);
        else if (currentTab.equals("settings")) binding.bottomNav.setSelectedItemId(R.id.nav_settings);
    }

    private void updateUIForTab() {
        binding.taskRecycler.setVisibility(currentTab.equals("settings") ? View.GONE : View.VISIBLE);
        binding.settingsContainer.setVisibility(currentTab.equals("settings") ? View.VISIBLE : View.GONE);
        binding.financeSummaryCard.setVisibility(currentTab.equals("finance") ? View.VISIBLE : View.GONE);
        binding.fabAdd.setVisibility(currentTab.equals("settings") ? View.GONE : View.VISIBLE);
        
        if (currentTab.equals("finance")) {
            binding.workspaceTitle.setText(R.string.neural_ledger);
        } else if (currentTab.equals("kanban")) {
            binding.workspaceTitle.setText(R.string.neural_flow);
        } else if (currentTab.equals("settings")) {
            binding.workspaceTitle.setText(R.string.core_config);
        } else {
            binding.workspaceTitle.setText(R.string.axon_hub);
        }
        
        filterBlocks();
        calculateFinanceTotal();
    }

    private void filterBlocks() {
        List<SynapseBlockEntity> oldList = new ArrayList<>(filteredBlocks);

        filteredBlocks.clear();
        for (SynapseBlockEntity b : allBlocks) {
            if (currentTab.equals("tasks")) {
                if (b.getType().equals("TODO") || b.getType().equals("TEXT") || b.getType().equals("HEADER")){
                    filteredBlocks.add(b);
                }
            } else if (currentTab.equals("finance") && b.getType().equals("FINANCE")) {
                filteredBlocks.add(b);
            } else if (currentTab.equals("kanban") && b.getType().equals("TODO")) {
                filteredBlocks.add(b);
            }
        }

        Collections.sort(filteredBlocks, (b1, b2) -> {
            /*if(b1.getType().equals("HEADER") && !b2.getType().equals("HEADER")) return -1;
            if(!b1.getType().equals("HEADER") && b2.getType().equals("HEADER")) return 1;*/
            return Long.compare(b1.getTimestamp(), b2.getTimestamp());
        });

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return oldList.size();
            }
            @Override
            public int getNewListSize() {
                return filteredBlocks.size();
            }
            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return oldList.get(oldItemPosition).getId().equals(filteredBlocks.get(newItemPosition).getId());
            }
            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                SynapseBlockEntity oldBlock = oldList.get(oldItemPosition);
                SynapseBlockEntity newBlock = filteredBlocks.get(newItemPosition);

                boolean contentSame = java.util.Objects.equals(oldBlock.getContent(), newBlock.getContent());
                boolean completedSame = oldBlock.isCompleted() == newBlock.isCompleted();
                return contentSame && completedSame;
            }
        });
        diffResult.dispatchUpdatesTo(adapter);
    }

    private void loadBlocks() {
        if(adapter == null) return;

        synapseDao.getAllBlocks(currentUserId).observe(this, blocks -> {
            if(blocks == null) return;
            allBlocks = blocks;
            filterBlocks();
            calculateFinanceTotal();
            checkBurnout();
        });
    }

    private void calculateFinanceTotal() {
        double total = 0;
        for (SynapseBlockEntity b : allBlocks) {
            if (b.getType().equals("FINANCE")) {
                total += b.getAmount();
            }
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
            binding.txtAffirmation.setText(String.format(Locale.getDefault(), "Neural Load High: %d tasks pending. Remember to breathe.", pendingTasks));
        } else {
            binding.burnoutAlert.setVisibility(View.GONE);
        }
    }

    private Dialog showAddBlockDialog() {
        // Force hide the overlay to ensure it's not blocking touches
        binding.blurOverlay.setVisibility(View.GONE);

        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheet =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);

        // Use a simple layout for the bottom sheet
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 60, 60, 80);
        layout.setBackgroundColor(ContextCompat.getColor(this, R.color.card_bg));

        String[] types = {"Header", "Note", "To-Do Task", "Finance Entry", "Template (COMING SOON)"};
        int[] icons = {android.R.drawable.checkbox_on_background,
                android.R.drawable.ic_menu_edit,
                android.R.drawable.ic_menu_sort_by_size,
                android.R.drawable.ic_menu_agenda,
                android.R.drawable.ic_menu_agenda};

        for (int i = 0; i < types.length; i++) {
            final int index = i;
            TextView itemView = new TextView(this);
            itemView.setText(types[i]);
            itemView.setTextSize(18);
            itemView.setPadding(20, 40, 20, 40);
            itemView.setCompoundDrawablesWithIntrinsicBounds(icons[i], 0, 0, 0);
            itemView.setCompoundDrawablePadding(40);
            itemView.setClickable(true);
            itemView.setFocusable(true);

            // This is the selection logic
            itemView.setOnClickListener(v -> {
                String type;
                if (index == 0) type = "HEADER";
                else if (index == 1) type = "TEXT";
                else if (index == 2) type = "TODO";
                else type = "FINANCE";

                final String finalType = type;

                insertNewBlock(type);

                runOnUiThread(() -> {
                    bottomSheet.dismiss();
                    Toast.makeText(this, type + " Added to Neural Matrix", Toast.LENGTH_SHORT).show();

                    if (onboardingStep == 3){
                        onboardingStep = 4;
                        showOnboardingStep();
                    }

                    if (finalType.equals("FINANCE")){
                        if(!currentTab.equals("finance")) {
                            currentTab = getString(R.string.finance);
                            binding.bottomNav.setSelectedItemId(R.id.nav_finance);
                            updateUIForTab();
                        }
                    } else {
                        if(currentTab.equals("settings")) {
                            currentTab = "tasks";
                            binding.bottomNav.setSelectedItemId(R.id.nav_tasks);
                            updateUIForTab();
                        }
                    }
                    filterBlocks();
                });
            });
            layout.addView(itemView);
        }

        bottomSheet.setContentView(layout);
        bottomSheet.show();

        bottomSheet.setOnDismissListener(dialog -> {
            if (onboardingStep == 3){
                onboardingStep = 4;
                showOnboardingStep();
            }
        });

        return bottomSheet;
    }

    private void insertNewBlock(String type){
        Executors.newSingleThreadExecutor().execute(() -> {
            String newId = UUID.randomUUID().toString();
            SynapseBlockEntity block = new SynapseBlockEntity(newId, currentUserId, type, "");
            block.setTimestamp(System.currentTimeMillis());

            Log.d("debug", "content="+type);

            if(type.equals("TODO") || type.equals("TEXT")){
                SynapseBlockEntity newestHeader = null;
                for(SynapseBlockEntity b : allBlocks){
                    if(b.getType().equals("HEADER")){
                        if(newestHeader == null || b.getTimestamp() > newestHeader.getTimestamp()){
                            newestHeader = b;

                        }
                    }
                }
                if(newestHeader != null){
                    block.setParentId(newestHeader.getId());
                    Log.d("debug", "content="+newestHeader.getContent());
                } else {
                    block.setParentId(null);
                }
            }
            synapseDao.insert(block);
            if (type.equals("FINANCE")) {
                runOnUiThread(() -> showFinanceEditDialog(block, null));
            }
        });
    }

    private void showFinanceEditDialog(SynapseBlockEntity block, MainAdapter.FinanceViewHolder view) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        View v = LayoutInflater.from(this).inflate(R.layout.dialog_finance_edit, null);
        EditText etDesc = v.findViewById(R.id.et_finance_desc);
        EditText etAmount = v.findViewById(R.id.et_finance_amount);
        RadioGroup rdoType = v.findViewById(R.id.rdo_finance_type);
        RadioButton rdoIncome = v.findViewById(R.id.rdo_income);
        NumberPicker spnMonth = v.findViewById(R.id.spn_month);
        NumberPicker spnDay = v.findViewById(R.id.spn_day);
        NumberPicker spnYear = v.findViewById(R.id.spn_year);

        etDesc.setText(block.getContent());
        etAmount.setText(String.valueOf(Math.abs(block.getAmount())));
        spnMonth.setMinValue(1);
        spnMonth.setMaxValue(12);
        spnMonth.setValue(month+1);

        spnDay.setMinValue(1);
        spnDay.setMaxValue(calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        spnDay.setValue(day);
        spnYear.setMinValue(1900);
        spnYear.setMaxValue(2030);
        spnYear.setValue(year);
        
        new AlertDialog.Builder(this)
                .setTitle("Finance Matrix")
                .setView(v)
                .setPositiveButton("Sync", (dialog, which) -> {
                    int sign = rdoType.getCheckedRadioButtonId() == rdoIncome.getId() ? 1 : -1;
                    double netFinanceChange = Double.parseDouble(etAmount.getText().toString()) * sign;

                    block.setContent(etDesc.getText().toString());
                    block.setDay(spnDay.getValue());
                    block.setMonth(spnMonth.getValue());
                    block.setYear(spnYear.getValue());

                    try {
                        block.setAmount(netFinanceChange);
                    } catch (Exception ignored) {}

                    Executors.newSingleThreadExecutor().execute(() -> {
                        synapseDao.update(block);

                        if (view != null) {
                            runOnUiThread(() -> {
                                view.cat.setText(block.getContent());
                                view.amt.setText(String.format("$%.2f", block.getAmount()));
                                view.date.setText(String.format("%02d", block.getMonth()) + "/" + String.format("%02d", block.getDay()) + "/" + String.format("%02d", block.getYear()));

                                if (block.getAmount() < 0) {
                                    view.amt.setTextColor(Color.RED);
                                } else {
                                    view.amt.setTextColor(Color.GREEN);
                                }
                            });
                        }

                    });
                })
                .show();
    }

    private void setupTouchHelper() {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public int getSwipeDirs(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder viewHolder) {
                int pos = viewHolder.getBindingAdapterPosition();

                if(pos == RecyclerView.NO_POSITION || filteredBlocks == null ||pos >= filteredBlocks.size()) {
                    return 0;
                }

                SynapseBlockEntity block = filteredBlocks.get(pos);
                if(block.getType().equals("TODO")) {
                    return ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
                } else {
                    return ItemTouchHelper.LEFT;
                }
            }

            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                final int pos = viewHolder.getBindingAdapterPosition();
                if(pos == RecyclerView.NO_POSITION || pos >= filteredBlocks.size()) return;

                final SynapseBlockEntity block = filteredBlocks.get(pos);

                if(direction == ItemTouchHelper.LEFT){
                    filteredBlocks.remove(pos);
                    adapter.notifyItemRemoved(pos);

                    Executors.newSingleThreadExecutor().execute(() -> {
                        synapseDao.delete(block);
                    });
                }
                else if(direction == ItemTouchHelper.RIGHT) {
                    if (!block.isCompleted()){
                        block.setCompleted(true);

                        Executors.newSingleThreadExecutor().execute(() -> {
                            synapseDao.update(block);

                            for(SynapseBlockEntity b : allBlocks){
                               if(b.getId().equals(block.getId())) {
                                   b.setCompleted(true);
                                   break;
                               }
                            }
                            runOnUiThread(() -> {
                                addExp(50);
                                updateInsights();
                                filterBlocks();
                            });
                        });
                    } else {
                        adapter.notifyItemChanged(pos);
                    }
                }
            }
        };
        new ItemTouchHelper(callback).attachToRecyclerView(binding.taskRecycler);
    }

    private void setupSettingsLogic() {
        binding.themeToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnLightTheme) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    prefs.edit().putInt("theme_mode", AppCompatDelegate.MODE_NIGHT_NO).apply();
                } else if (checkedId == R.id.btnDarkTheme) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    prefs.edit().putInt("theme_mode", AppCompatDelegate.MODE_NIGHT_YES).apply();
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                    prefs.edit().putInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM).apply();
                }
            }
        });

        binding.rgStaleness.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_2days) stalenessThreshold = 2;
            else if (checkedId == R.id.rb_5days) stalenessThreshold = 5;
            else if (checkedId == R.id.rb_7days) stalenessThreshold = 7;
            prefs.edit().putInt("staleness_threshold", stalenessThreshold).apply();
        });
        
        binding.btnResetOnboarding.setOnClickListener(v -> {
            prefs.edit().putBoolean("is_new_user", true).apply();
            startOnboarding();
        });
    }

    private void applySavedTheme() {
        int theme = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(theme);
    }

    private void addExp(int amount) {
        if(currentUser == null) return;

        int newExp = currentUser.getExp() + amount;
        currentUser.setExp(newExp);

        int newLevel = 1 + (newExp / 1000);
        currentUser.setLevel(newLevel);

        Executors.newSingleThreadExecutor().execute(() -> {
            userDao.update(currentUser);
            runOnUiThread(this::updateLevelUI);
        });
    }

    private void updateInsights() {
        if (currentUser == null) return;

        int exp = currentUser.getExp();
        int level = currentUser.getLevel();
        int progress = (exp % 1000) / 10;

        binding.levelText.setText(String.format(Locale.getDefault(), "Lvl %d", level));
        binding.expProgress.setProgress(progress);

        if (level >= 5) {
            binding.txtRewardStatus.setText("Templates: UNLOCKED");
        } else {
            binding.txtRewardStatus.setText("Template Builder at Lvl. 5");
        }
    }

    private void setupTTS() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) tts.setLanguage(Locale.US);
        });
    }

    private void showAxonPromptDialog() {
        EditText input = new EditText(this);
        input.setHint("Query Axon...");
        new AlertDialog.Builder(this)
                .setTitle("Neural Interface")
                .setView(input)
                .setPositiveButton("Transmit", (d, w) -> askAxon(input.getText().toString()))
                .show();
    }

    private void askAxon(String prompt) {
        Content content = new Content.Builder().addText(prompt).build();
        ListenableFuture<GenerateContentResponse> response = axonModel.generateContent(content);
        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String text = result.getText();
                runOnUiThread(() -> {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Axon Response")
                            .setMessage(text)
                            .setPositiveButton("Speak", (d, w) -> tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null))
                            .setNegativeButton("Close", null)
                            .show();
                });
            }

            @Override
            public void onFailure(Throwable t) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Neural Link Error", Toast.LENGTH_SHORT).show());
            }
        }, Executors.newSingleThreadExecutor());
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

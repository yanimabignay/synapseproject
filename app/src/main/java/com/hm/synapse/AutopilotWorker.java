package com.hm.synapse;

import android.content.Context;
import android.content.SharedPreferences;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AutopilotWorker extends Worker {
    private TextToSpeech tts;
    private final SynapseDatabase database;
    private static final String TAG = "SynapseAutopilot";

    public AutopilotWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        database = SynapseDatabase.getDatabase(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Neural vitals check initiated...");

        try {
            // 1. Fetch current workspace state from SQLite
            List<SynapseBlockEntity> blocks = database.synapseDao().getAllBlocksSync("local_user");

            // 2. Perform Predictive Analytics (Burnout & Staleness)
            int activeTasks = 0;
            int staleTasks = 0;

            SharedPreferences prefs = getApplicationContext().getSharedPreferences("SynapsePrefs", Context.MODE_PRIVATE);
            int thresholdDays = prefs.getInt("staleness_threshold", 5);
            long thresholdMs = TimeUnit.DAYS.toMillis(thresholdDays);
            long now = System.currentTimeMillis();

            for (SynapseBlockEntity block : blocks) {
                if (block.getType().equals("TODO") && !block.isCompleted()) {
                    activeTasks++;
                    if (now - block.getLastAccessed() > thresholdMs) {
                        staleTasks++;
                    }
                }
            }

            // 3. Proactive Decision Engine
            String message = "";
            if (activeTasks > 10) {
                message = "Critical workspace density detected. I suggest prioritizing three core blocks and archiving the rest.";
            } else if (staleTasks > 0) {
                message = "You have " + staleTasks + " neglected tasks. Would you like me to reschedule them for your peak focus hours?";
            } else if (activeTasks == 0) {
                message = "Workspace is clear. Your cognitive load is low. It's a great time for deep creative work.";
            }

            if (!message.isEmpty()) {
                triggerVoiceReminder(message);
            }

            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Autopilot cycle failed", e);
            return Result.retry();
        }
    }

    private void triggerVoiceReminder(String message) {
        final CountDownLatch latch = new CountDownLatch(1);

        tts = new TextToSpeech(getApplicationContext(), status -> {
            if (status != TextToSpeech.ERROR) {
                tts.setLanguage(Locale.US);
                tts.setPitch(0.9f); // Neutral, professional pitch
                tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "AutopilotID");

                // Wait for TTS to finish or timeout
                new Thread(() -> {
                    while (tts != null && tts.isSpeaking()) {
                        try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                    }
                    latch.countDown();
                }).start();
            } else {
                latch.countDown();
            }
        });

        try {
            latch.await(10, TimeUnit.SECONDS); // Max wait time
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (tts != null) {
                tts.stop();
                tts.shutdown();
            }
        }
    }
}

package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ProgressAnalyticsActivity extends AppCompatActivity {

    private TextView tvDaysCompleted, tvCompletionRate, tvMostProductive;
    private ProgressBar progressBar;
    private ImageButton btnBack;
    private ConstraintLayout rootLayout;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private DailyProgressChart dailyChart;

    private boolean isGuest;

    private static final String PREFS_NAME = "AppPreferences";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_BACKGROUND_COLOR = "background_color";

    private boolean currentDarkMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress_analytics);

        isGuest = getIntent().getBooleanExtra("isGuest", false);

        // Initialize views
        tvDaysCompleted = findViewById(R.id.tvDaysCompleted);
        tvCompletionRate = findViewById(R.id.tvCompletionRate);
        tvMostProductive = findViewById(R.id.tvMostProductive);
        progressBar = findViewById(R.id.progressBar);
        btnBack = findViewById(R.id.btnBack);
        rootLayout = findViewById(R.id.rootLayout);
        dailyChart = findViewById(R.id.dailyChart);


        applyCustomizations();

        // Back button
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(ProgressAnalyticsActivity.this, DashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish(); // Optional: finish current activity
        });

        if (isGuest) {
            Toast.makeText(this, "Guest users cannot view analytics.", Toast.LENGTH_LONG).show();
            tvDaysCompleted.setText("Guest Mode");
            tvCompletionRate.setText("Analytics unavailable");
            tvMostProductive.setText("");
            progressBar.setProgress(0);
            return;
        }

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            finish();
            return;
        }

        // Placeholder values
        tvDaysCompleted.setText("Tasks completed: N/A");
        tvCompletionRate.setText("Completion rate: N/A");
        tvMostProductive.setText("Most productive day: N/A");
        progressBar.setProgress(0);

        // Load data
        fetchTaskData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean(KEY_DARK_MODE, false);
        if (currentDarkMode != isDarkMode) {
            recreate();
        }
        applyCustomizations();
    }

    private void applyTheme() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        currentDarkMode = prefs.getBoolean(KEY_DARK_MODE, false);
        if (currentDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void applyCustomizations() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean(KEY_DARK_MODE, false);

        if (rootLayout != null) {
            if (isDarkMode) {
                rootLayout.setBackgroundColor(Color.BLACK);
                btnBack.setColorFilter(Color.WHITE);
            } else {
                int backgroundColor = prefs.getInt(KEY_BACKGROUND_COLOR, Color.WHITE);
                rootLayout.setBackgroundColor(backgroundColor);
                btnBack.clearColorFilter();
            }
        }

        // Pass dark mode status to custom charts
        if (dailyChart != null) {
            dailyChart.setDarkMode(isDarkMode);
        }
    }

    private void fetchTaskData() {
        db.collection("tasks")
                .whereEqualTo("userId", currentUser.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalTasks = 0;
                    int completedTasks = 0;
                    Map<String, Integer> dailyCompletion = new HashMap<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Boolean isCompleted = doc.getBoolean("completed");
                        Date taskDate = doc.getDate("date");
                        totalTasks++;

                        if (isCompleted != null && isCompleted && taskDate != null) {
                            completedTasks++;
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                            String day = sdf.format(taskDate);
                            dailyCompletion.put(day, dailyCompletion.getOrDefault(day, 0) + 1);
                        }
                    }

                    // Update UI
                    tvDaysCompleted.setText("Tasks completed: " + completedTasks);
                    int completionRate = totalTasks > 0 ? (completedTasks * 100) / totalTasks : 0;
                    tvCompletionRate.setText("Completion rate: " + completionRate + "%");
                    progressBar.setProgress(completionRate);

                    String mostProductiveDay = "N/A";
                    int maxTasks = 0;
                    for (String day : dailyCompletion.keySet()) {
                        if (dailyCompletion.get(day) > maxTasks) {
                            maxTasks = dailyCompletion.get(day);
                            mostProductiveDay = day;
                        }
                    }
                    tvMostProductive.setText("Most productive day: " + mostProductiveDay);

                    // Update charts
                    dailyChart.setData(dailyCompletion);
                })
                .addOnFailureListener(e -> { /* handle errors if needed */ });
    }
}

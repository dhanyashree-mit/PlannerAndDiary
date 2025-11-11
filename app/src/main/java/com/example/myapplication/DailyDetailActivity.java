package com.example.myapplication;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class DailyDetailActivity extends AppCompatActivity {

    TextView txtDiaryTitle, txtDiaryContent, txtDiaryDate;
    ImageButton btnBack;
    LinearLayout rootLayout;

    private static final String PREFS_NAME = "AppPreferences";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_BACKGROUND_COLOR = "background_color";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dailydairyactivity);

        rootLayout = findViewById(R.id.rootLayout); // Assuming root is LinearLayout

        // Initialize views
        txtDiaryTitle = findViewById(R.id.txtDiaryTitle);
        txtDiaryContent = findViewById(R.id.txtDiaryContent);
        txtDiaryDate = findViewById(R.id.txtDiaryDate);
        btnBack = findViewById(R.id.btnBack);

        // Load and apply preferences
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean(KEY_DARK_MODE, false);
        int backgroundColor = prefs.getInt(KEY_BACKGROUND_COLOR, Color.WHITE);
        applyDarkModeAndBackground(isDarkMode, backgroundColor);

        // Back button click → finish activity and return to MainActivity
        btnBack.setOnClickListener(v -> finish());

        // Get diary data passed from MainActivity
        String title = getIntent().getStringExtra("diaryTitle");
        String content = getIntent().getStringExtra("diaryContent");
        String timestamp = getIntent().getStringExtra("diaryTimestamp");

        // Check if data exists
        if (title == null || content == null || timestamp == null) {
            Toast.makeText(this, "Diary not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Display diary data
        txtDiaryTitle.setText(title);
        txtDiaryContent.setText(content);
        txtDiaryDate.setText(timestamp);
    }

    private void applyDarkModeAndBackground(boolean darkMode, int backgroundColor) {
        if (darkMode) {
            rootLayout.setBackgroundColor(Color.BLACK);
        } else {
            rootLayout.setBackgroundColor(backgroundColor);
        }
    }
}

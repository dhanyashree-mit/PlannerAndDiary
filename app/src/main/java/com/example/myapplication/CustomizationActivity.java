
package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

public class CustomizationActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "AppPreferences";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_BACKGROUND_COLOR = "background_color";

    private ConstraintLayout rootLayout;
    private LinearLayout colorSelectionLayout;
    private Button backgroundSection;
    private Switch darkModeSwitch;
    private ImageButton btnBack;
    private TextView titleText;

    private int currentBackgroundColor = Color.WHITE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customization );

        // find views
        rootLayout = findViewById(R.id.rootLayout);
        colorSelectionLayout = findViewById(R.id.colorSelectionLayout);
        backgroundSection = findViewById(R.id.backgroundSection);
        darkModeSwitch = findViewById(R.id.darkModeSwitch);
        btnBack = findViewById(R.id.btnBack);
        titleText = findViewById(R.id.titleText);

        // Safety check (basic)
        if (rootLayout == null || colorSelectionLayout == null || backgroundSection == null
                || darkModeSwitch == null || btnBack == null || titleText == null) {
            Toast.makeText(this, "Missing views in layout!", Toast.LENGTH_LONG).show();
            return;
        }

        // Load saved preferences
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean(KEY_DARK_MODE, false);
        currentBackgroundColor = prefs.getInt(KEY_BACKGROUND_COLOR, Color.WHITE);

        // Apply saved state
        applyDarkMode(isDarkMode);
        rootLayout.setBackgroundColor(isDarkMode ? Color.BLACK : currentBackgroundColor);
        darkModeSwitch.setChecked(isDarkMode);

        // Back button: default behavior (go back)
        btnBack.setOnClickListener(v -> onBackPressed());

        // Toggle color selection visibility
        backgroundSection.setOnClickListener(v -> {
            if (colorSelectionLayout.getVisibility() == View.GONE) {
                colorSelectionLayout.setVisibility(View.VISIBLE);
            } else {
                colorSelectionLayout.setVisibility(View.GONE);
            }
        });

        // Dark mode toggle
        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save pref
            prefs.edit().putBoolean(KEY_DARK_MODE, isChecked).apply();

            // Apply
            applyDarkMode(isChecked);

            // If switching to light mode, re-apply saved background color
            if (!isChecked) {
                rootLayout.setBackgroundColor(currentBackgroundColor);
            } else {
                rootLayout.setBackgroundColor(Color.BLACK);
            }

            Toast.makeText(this, "Dark Mode: " + (isChecked ? "On" : "Off"), Toast.LENGTH_SHORT).show();
        });

        // Populate color selection with same pastel colors as before
        String[] colorNames = {"Pink", "Green", "Blue", "Yellow", "Purple", "White"};
        int[] pastelColors = {
                Color.parseColor("#FFB3BA"), // Light pink
                Color.parseColor("#BAFFC9"), // Light green
                Color.parseColor("#BAE1FF"), // Light blue
                Color.parseColor("#FFFFBA"), // Light yellow
                Color.parseColor("#E0BBE4"), // Light purple
                Color.WHITE  // White
        };

        // Create color swatches
        colorSelectionLayout.removeAllViews();
        for (int i = 0; i < pastelColors.length; i++) {
            final int colorIndex = i;
            TextView colorView = new TextView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 8, 0, 8);
            colorView.setLayoutParams(params);
            colorView.setPadding(20, 24, 20, 24);
            colorView.setBackgroundColor(pastelColors[i]);
            colorView.setText(colorNames[i]);
            colorView.setTextColor(Color.BLACK);
            colorView.setGravity(Gravity.CENTER);
            colorView.setTextSize(16f);

            // click -> apply color (only when not in dark mode)
            colorView.setOnClickListener(v -> {
                boolean currentlyDark = darkModeSwitch.isChecked();
                if (currentlyDark) {
                    Toast.makeText(this, "Disable dark mode to change background color.", Toast.LENGTH_SHORT).show();
                    return;
                }

                currentBackgroundColor = pastelColors[colorIndex];
                rootLayout.setBackgroundColor(currentBackgroundColor);
                // persist selection
                prefs.edit().putInt(KEY_BACKGROUND_COLOR, currentBackgroundColor).apply();
                Toast.makeText(this, "Background color changed to " + colorNames[colorIndex], Toast.LENGTH_SHORT).show();
            });

            colorSelectionLayout.addView(colorView);
        }
    }

    private void applyDarkMode(boolean darkMode) {
        if (darkMode) {
            // Dark mode styling
            titleText.setTextColor(Color.WHITE);
            // If you have other UI elements to change, do it here
            // Tint the back arrow white
            btnBack.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        } else {
            // Light mode styling
            titleText.setTextColor(Color.BLACK);
            // restore back arrow to dark (black). If your drawable has multiple colors,
            // you can clear the filter instead: btnBack.clearColorFilter();
            btnBack.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP);
        }
    }
}

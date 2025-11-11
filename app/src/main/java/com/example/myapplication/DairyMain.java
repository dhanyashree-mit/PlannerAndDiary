package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class DairyMain extends AppCompatActivity {

    private ListView diaryListView;
    private List<String> diaryTitles = new ArrayList<>();
    private List<String> diaryIds = new ArrayList<>(); // To store document IDs
    private ArrayAdapter<String> adapter;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private EditText searchEditText;
    private ImageButton searchButton;
    private ImageButton backButton;
    private ImageButton editButton;
    private RelativeLayout rootLayout;

    private boolean isGuest;

    private static final String PREFS_NAME = "AppPreferences";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_BACKGROUND_COLOR = "background_color";

    private boolean currentDarkMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme(); // Apply theme before creating the view
        super.onCreate(savedInstanceState);
        setContentView(R.layout.diary_main);

        isGuest = getIntent().getBooleanExtra("isGuest", false);
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        diaryListView = findViewById(R.id.listDiaries);
        searchEditText = findViewById(R.id.editSearch);
        searchButton = findViewById(R.id.btnSearch);
        backButton = findViewById(R.id.btnBack);
        editButton = findViewById(R.id.btnEdit);
        rootLayout = findViewById(R.id.rootLayout);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, diaryTitles);
        diaryListView.setAdapter(adapter);

        applyCustomizations(); // Apply background color

        if (isGuest) {
            editButton.setVisibility(View.GONE);
            searchEditText.setHint("Guest mode - Search disabled");
            searchEditText.setEnabled(false);
            searchButton.setEnabled(false);
        } else {
            loadDiaryEntries("");
        }

        diaryListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedDiaryId = diaryIds.get(position);
            Intent intent = new Intent(DairyMain.this, DiaryEditorActivity.class);
            intent.putExtra("diaryId", selectedDiaryId);
            intent.putExtra("isGuest", isGuest);
            startActivity(intent);
        });

        searchButton.setOnClickListener(v -> {
            String query = searchEditText.getText().toString().trim();
            loadDiaryEntries(query);
        });

        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(DairyMain.this, DashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish(); // Optional: finish current activity
        });

        editButton.setOnClickListener(v -> {
             if (isGuest) {
                Toast.makeText(this, "Guest users cannot add new entries.", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(DairyMain.this, DiaryEditorActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check if the theme has changed and recreate if necessary
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean(KEY_DARK_MODE, false);
        if (currentDarkMode != isDarkMode) {
            recreate();
        }

        applyCustomizations(); // Re-apply background in case it changed
        if (!isGuest) {
            loadDiaryEntries("");
        }
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
                backButton.setColorFilter(Color.WHITE);
            } else {
                int backgroundColor = prefs.getInt(KEY_BACKGROUND_COLOR, Color.WHITE);
                rootLayout.setBackgroundColor(backgroundColor);
                backButton.clearColorFilter();
            }
        }
    }

    private void loadDiaryEntries(String titleQuery) {
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("diaries")
                .whereEqualTo("userId", currentUser.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        diaryTitles.clear();
                        diaryIds.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String title = document.getString("title");
                            if (title != null && (titleQuery.isEmpty() || title.toLowerCase().contains(titleQuery.toLowerCase()))) {
                                diaryTitles.add(title);
                                diaryIds.add(document.getId());
                            }
                        }
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(DairyMain.this, "Error loading diaries.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}

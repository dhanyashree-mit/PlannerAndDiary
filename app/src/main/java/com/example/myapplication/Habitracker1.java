package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class Habitracker1 extends AppCompatActivity {

    private RecyclerView recyclerViewHabits;
    private Button buttonAddHabit;
    private ImageButton btnBack;
    private View rootLayout;

    private HabitAdapter habitAdapter;
    private List<Habit> habitList;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;

    private boolean isDarkMode;
    private int backgroundColor;
    private boolean isGuest;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        applyTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.habit_main);

        isGuest = getIntent().getBooleanExtra("isGuest", false);

        initViews();
        loadPreferences();
        applyCustomizations();
        setupFirebase();
        setupRecyclerView();
        if (!isGuest) {
            loadHabits();
        }

        buttonAddHabit.setOnClickListener(v -> {
            if (isGuest) {
                Toast.makeText(this, "Guest users cannot add habits.", Toast.LENGTH_SHORT).show();
                return;
            }
            showAddHabitDialog();
        });

        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(Habitracker1.this, DashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish(); // Optional: finish current activity
        });
    }

    private void initViews() {
        recyclerViewHabits = findViewById(R.id.recyclerViewHabits);
        buttonAddHabit = findViewById(R.id.buttonAddHabit);
        btnBack = findViewById(R.id.btnBack);
        rootLayout = findViewById(R.id.habitRootLayout);

        if (isGuest) {
            buttonAddHabit.setVisibility(View.GONE);
        }
    }

    private void setupFirebase() {
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
    }

    private void setupRecyclerView() {
        habitList = new ArrayList<>();
        habitAdapter = new HabitAdapter(habitList, habit -> {
            // Optional: handle toggle updates
        });
        habitAdapter.setDarkMode(isDarkMode);
        recyclerViewHabits.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewHabits.setAdapter(habitAdapter);
    }

    private void loadHabits() {
        if (currentUser == null) return;

        db.collection("habits")
                .whereEqualTo("userId", currentUser.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        habitList.clear();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            Habit habit = doc.toObject(Habit.class);
                            habit.setHabitId(doc.getId());
                            habit.setCompleted(false); // Always start with checkbox unchecked
                            habitList.add(habit);
                        }
                        habitAdapter.notifyDataSetChanged();
                    }
                });
    }

    private void showAddHabitDialog() {
        AddHabitDialogFragment dialog = new AddHabitDialogFragment();
        dialog.setOnHabitAddedListener(title -> {
            if (currentUser == null) return;

            Habit habit = new Habit(title, currentUser.getUid());
            db.collection("habits")
                    .add(habit)
                    .addOnSuccessListener(ref -> {
                        habit.setHabitId(ref.getId());
                        habitList.add(habit);
                        habitAdapter.notifyItemInserted(habitList.size() - 1);
                    });
        });
        dialog.show(getSupportFragmentManager(), "AddHabitDialog");
    }

    private void applyTheme() {
        SharedPreferences prefs = getSharedPreferences("AppPreferences", MODE_PRIVATE);
        isDarkMode = prefs.getBoolean("dark_mode", false);
        AppCompatDelegate.setDefaultNightMode(isDarkMode ?
                AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    private void loadPreferences() {
        SharedPreferences prefs = getSharedPreferences("AppPreferences", MODE_PRIVATE);
        backgroundColor = prefs.getInt("background_color", Color.WHITE);
    }

    private void applyCustomizations() {
        if (rootLayout != null) {
            if (isDarkMode) {
                rootLayout.setBackgroundColor(Color.BLACK);
                btnBack.setColorFilter(Color.WHITE);
            } else {
                rootLayout.setBackgroundColor(backgroundColor);
                btnBack.clearColorFilter();
            }
        }

        buttonAddHabit.setBackgroundColor(isDarkMode ? Color.DKGRAY : Color.parseColor("#2710F7"));
        buttonAddHabit.setTextColor(Color.WHITE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPreferences();
        applyCustomizations();
        habitAdapter.setDarkMode(isDarkMode);
        habitAdapter.notifyDataSetChanged();
    }
}

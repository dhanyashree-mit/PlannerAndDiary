package com.example.myapplication;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private TextView tvDateHeader;
    private ImageButton btnMenu, btnClearSearch, btnBack;
    private EditText etSearch;
    private LinearLayout layoutTasksContainer;
    private LinearLayout rootLayout;

    private List<Task> allTodayTaskList;
    private boolean isGuest;
    private Map<String, Class<?>> moduleMap;
    private PopupMenu searchPopupMenu;

    private static final String PREFS_NAME = "AppPreferences";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_BACKGROUND_COLOR = "background_color";

    private boolean currentDarkMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme(); // Apply theme before creating the view

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();
        isGuest = getIntent().getBooleanExtra("isGuest", false);

        initializeViews();
        applyCustomizations(); // Apply background color
        initializeModuleMap();
        setupClickListeners();

        allTodayTaskList = new ArrayList<>();
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

        if (isGuest) {
            refreshTaskListUI(new ArrayList<>());
        } else if (currentUser != null) {
            loadTodayTasks();
        }
    }

    private void initializeViews() {
        tvDateHeader = findViewById(R.id.tvDateHeader);
        btnMenu = findViewById(R.id.btnMenu);
        etSearch = findViewById(R.id.etSearch);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        layoutTasksContainer = findViewById(R.id.layoutTasksContainer);
        rootLayout = findViewById(R.id.dashboardRoot);
        btnBack = findViewById(R.id.btnBack);

        Date today = Calendar.getInstance().getTime();
        String dateString = DateFormat.format("dd-MM-yy", today).toString();
        tvDateHeader.setText("Today's tasks - " + dateString);
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

        if (isDarkMode) {
            rootLayout.setBackgroundColor(Color.BLACK);
            btnBack.setColorFilter(Color.WHITE);

            // ✅ Make search box gray with white text
            etSearch.setBackgroundColor(Color.parseColor("#424242")); // Dark grey
            etSearch.setTextColor(Color.WHITE);
            etSearch.setHintTextColor(Color.LTGRAY);
            etSearch.setCompoundDrawableTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));

        } else {
            int backgroundColor = prefs.getInt(KEY_BACKGROUND_COLOR, Color.WHITE);
            rootLayout.setBackgroundColor(backgroundColor);
            btnBack.clearColorFilter();

            // ✅ Restore normal search box appearance
            etSearch.setBackgroundResource(R.drawable.rounded_search_background);
            etSearch.setTextColor(Color.BLACK);
            etSearch.setHintTextColor(Color.DKGRAY);
            etSearch.setCompoundDrawableTintList(null);
        }
    }


    private void initializeModuleMap() {
        moduleMap = new HashMap<>();
        moduleMap.put("daily diary", DairyMain.class);
        moduleMap.put("task planner", TaskPlanner1.class);
        moduleMap.put("habit tracker", com.example.myapplication.Habitracker1.class);
        moduleMap.put("progress analytics", com.example.myapplication.ProgressAnalyticsActivity.class);
        moduleMap.put("audio diary", com.example.myapplication.DiaryAudioActivity.class);
        moduleMap.put("reminders", com.example.myapplication.RemindersActivity.class);
        moduleMap.put("personalization", CustomizationActivity.class);
    }

    private void setupClickListeners() {
        btnMenu.setOnClickListener(v -> showDashboardMenu());
        btnClearSearch.setOnClickListener(v -> etSearch.setText(""));
        btnBack.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(DashboardActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterTasks(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    private void filterTasks(String query) {
        List<Task> filteredList = new ArrayList<>();
        if (query.isEmpty()) {
            filteredList.addAll(allTodayTaskList);
        } else {
            for (Task task : allTodayTaskList) {
                if (task.getTitle().toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(task);
                }
            }
        }
        refreshTaskListUI(filteredList);
    }

    private void loadTodayTasks() {
        if (currentUser == null) {
            Log.e("DashboardActivity", "User is null");
            return;
        }

        Calendar todayCal = Calendar.getInstance();
        String todayDateString = DateFormat.format("dd/MM/yy", todayCal).toString();

        db.collection("tasks")
                .whereEqualTo("userId", currentUser.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        allTodayTaskList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Task t = document.toObject(Task.class);
                            t.setTaskId(document.getId());
                            Date taskDate = t.getDate();
                            if (taskDate != null) {
                                String taskDateString = DateFormat.format("dd/MM/yy", taskDate).toString();
                                if (taskDateString.equals(todayDateString)) {
                                    allTodayTaskList.add(t);
                                }
                            }
                        }
                        refreshTaskListUI(allTodayTaskList);
                    } else {
                        Toast.makeText(this, "Error fetching tasks", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void refreshTaskListUI(List<Task> tasksToDisplay) {
        layoutTasksContainer.removeAllViews();

        if (tasksToDisplay.isEmpty()) {
            TextView noTasks = new TextView(this);
            noTasks.setText(isGuest ? "Guest mode: No tasks to display." : "No tasks for today.");
            noTasks.setPadding(16, 32, 16, 16);
            noTasks.setGravity(Gravity.CENTER_HORIZONTAL);
            layoutTasksContainer.addView(noTasks);
            return;
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean(KEY_DARK_MODE, false);

        TableLayout tableLayout = new TableLayout(this);
        tableLayout.setStretchAllColumns(true);
        tableLayout.setBackgroundResource(R.drawable.table_border);

        // Header Row
        TableRow headerRow = new TableRow(this);
        headerRow.setBackgroundColor(Color.parseColor("#B39DDB")); // darker purple
        headerRow.addView(createHeaderCell("Time"));
        headerRow.addView(createHeaderCell("Task"));
        headerRow.addView(createHeaderCell("Status"));
        tableLayout.addView(headerRow);

        // Data Rows
        for (Task t : tasksToDisplay) {
            TableRow row = new TableRow(this);
            row.setBackgroundColor(Color.parseColor("#D1C4E9")); // lighter purple

            // Time Cell
            String displayTime = (t.getReminder() != null && !t.getReminder().isEmpty()) ? t.getReminder() : "-";
            TextView tvTime = new TextView(this);
            tvTime.setText(displayTime);
            tvTime.setTextColor(getResources().getColor(android.R.color.black));

            // Task Cell
            TextView tvTask = new TextView(this);
            tvTask.setText(t.getTitle());
            tvTask.setTextColor(getResources().getColor(android.R.color.black));

            // Status Checkbox
            CheckBox cb = new CheckBox(this);
            cb.setChecked(t.isCompleted());
            cb.setEnabled(!isGuest);

            // 🔧 Make checkbox clearly visible in dark mode
            if (isDarkMode) {
                cb.setButtonTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
            } else {
                cb.setButtonTintList(android.content.res.ColorStateList.valueOf(Color.BLACK));
            }

            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                t.setCompleted(isChecked);
                updateTaskCompletionStatus(t, isChecked);
            });

            row.addView(createTableCell(tvTime, false));
            row.addView(createTableCell(tvTask, false));
            row.addView(createTableCell(cb, true));
            tableLayout.addView(row);
        }

        layoutTasksContainer.addView(tableLayout);
    }


    private TextView createHeaderCell(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(16f);
        tv.setPadding(40, 40, 40, 40);
        tv.setTextColor(getResources().getColor(android.R.color.black));
        tv.setGravity(Gravity.CENTER);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setBackgroundResource(R.drawable.table_border);
        return tv;
    }

    private View createTableCell(View content, boolean isCheckbox) {
        LinearLayout cellContainer = new LinearLayout(this);
        cellContainer.setOrientation(LinearLayout.HORIZONTAL); // horizontal alignment
        cellContainer.setGravity(Gravity.CENTER_VERTICAL); // align in single row

        if (isCheckbox) {
            cellContainer.setPadding(10, 10, 10, 10);
        } else {
            cellContainer.setPadding(27, 27, 27, 27);
        }

        cellContainer.setBackgroundResource(R.drawable.table_border);
        cellContainer.addView(content);
        return cellContainer;
    }

    private void updateTaskCompletionStatus(Task task, boolean isCompleted) {
        if (isGuest) return;
        db.collection("tasks").document(task.getTaskId())
                .update("completed", isCompleted)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Updated!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Update failed.", Toast.LENGTH_SHORT).show());
    }

    // ----------- MENU ----------- 
    private void showDashboardMenu() {
        PopupMenu popupMenu = new PopupMenu(this, btnMenu);
        for (String moduleName : moduleMap.keySet()) {
            String[] words = moduleName.split(" ");
            StringBuilder capitalizedModuleName = new StringBuilder();
            for (String word : words) {
                if (!word.isEmpty()) {
                    capitalizedModuleName.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
                }
            }
            popupMenu.getMenu().add(capitalizedModuleName.toString().trim());
        }

        popupMenu.setOnMenuItemClickListener(item -> {
            Class<?> activityClass = moduleMap.get(item.getTitle().toString().toLowerCase());
            if (activityClass != null) {
                Intent intent = new Intent(this, activityClass);
                intent.putExtra("isGuest", isGuest);
                startActivity(intent);
                return true;
            }
            return false;
        });

        popupMenu.show();
    }

}

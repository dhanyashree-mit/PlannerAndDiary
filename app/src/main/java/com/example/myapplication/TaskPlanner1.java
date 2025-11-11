package com.example.myapplication;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskPlanner1 extends AppCompatActivity {

    private TextView tvSelectedDate;
    private ImageButton btnCalendar, btnPlus, btnBack;

    private LinearLayout layoutTaskInput, layoutTaskList, rootLayout;
    private EditText etTaskTitle;
    private Button btnSave, btnCancel;

    private Date selectedDate;
    private List<Task> taskList;
    private Task currentEditingTask;
    private boolean isEditingMode = false;
    private boolean isGuest;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private static final String PREFS_NAME = "AppPreferences";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_BACKGROUND_COLOR = "background_color";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme(); // Apply dark mode before creating view
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_planner1);

        isGuest = getIntent().getBooleanExtra("isGuest", false);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null && !isGuest) {
            Toast.makeText(this, "Please log in to use the task planner.", Toast.LENGTH_LONG).show();
            finish(); // Close activity if not logged in
            return;
        }

        initializeViews();         // Initialize all UI elements
        applyCustomizations();     // Apply background color / dark mode
        setupClickListeners();     // Setup buttons
        taskList = new ArrayList<>();
        if (!isGuest) {
            loadTasksFromFirestore();  // Load tasks
        }
        updateDateDisplay();       // Show current selected date (if any)
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyCustomizations(); // Re-apply background in case user changed settings
    }

    private void initializeViews() {
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        btnCalendar = findViewById(R.id.btnCalendar);
        btnPlus = findViewById(R.id.btnPlus);
        btnBack = findViewById(R.id.btnBack);
        layoutTaskInput = findViewById(R.id.layoutTaskInput);
        layoutTaskList = findViewById(R.id.layoutTaskList);
        etTaskTitle = findViewById(R.id.etTaskTitle);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
        rootLayout = findViewById(R.id.rootLayout);

        if (isGuest) {
            btnPlus.setVisibility(View.GONE);
            btnCalendar.setEnabled(false);
        }
    }

    private void setupClickListeners() {
        btnCalendar.setOnClickListener(v -> {
            if (isGuest) {
                Toast.makeText(TaskPlanner1.this, "Guest users cannot add tasks.", Toast.LENGTH_SHORT).show();
                return;
            }
            showDatePicker();
        });
        btnPlus.setOnClickListener(v -> {
            if (isGuest) {
                Toast.makeText(TaskPlanner1.this, "Guest users cannot add tasks.", Toast.LENGTH_SHORT).show();
                return;
            }
            showPlusMenu();
        });
        btnSave.setOnClickListener(v -> saveTask());
        btnCancel.setOnClickListener(v -> cancelTask());
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(TaskPlanner1.this, DashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish(); // Optional: finish current activity
        });
    }

    private void applyTheme() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean(KEY_DARK_MODE, false);
        if (isDarkMode) {
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
    }

    private void loadTasksFromFirestore() {
        if (currentUser == null) return;

        db.collection("tasks")
                .whereEqualTo("userId", currentUser.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        taskList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Task taskItem = document.toObject(Task.class);
                            taskItem.setTaskId(document.getId());
                            taskList.add(taskItem);
                        }
                        refreshTaskList();
                        updatePlusButtonVisibility();
                    } else {
                        Toast.makeText(this, "Error loading tasks: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveTask() {
        if (isGuest) {
            Toast.makeText(this, "Guest users cannot save tasks.", Toast.LENGTH_SHORT).show();
            return;
        }

        String taskTitle = etTaskTitle.getText().toString().trim();

        if (TextUtils.isEmpty(taskTitle)) {
            Toast.makeText(this, "Please enter a task title", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedDate == null) {
            Toast.makeText(this, "Please select a date first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isEditingMode && currentEditingTask != null) {
            db.collection("tasks").document(currentEditingTask.getTaskId())
                    .update("title", taskTitle, "date", selectedDate)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Task updated successfully", Toast.LENGTH_SHORT).show();
                        hideTaskInput();
                        loadTasksFromFirestore();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Error updating task.", Toast.LENGTH_SHORT).show());
        } else {
            Task newTask = new Task(currentUser.getUid(), taskTitle, selectedDate);
            db.collection("tasks")
                    .add(newTask)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Task saved successfully", Toast.LENGTH_SHORT).show();
                        hideTaskInput();
                        loadTasksFromFirestore();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Error saving task.", Toast.LENGTH_SHORT).show());
        }
    }

    private void showDeleteConfirmationDialog(Task task) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Task")
                .setMessage("Are you sure you want to delete '" + task.getTitle() + "'?")
                .setPositiveButton("Delete", (dialog, id) -> {
                    db.collection("tasks").document(task.getTaskId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Task deleted successfully", Toast.LENGTH_SHORT).show();
                                loadTasksFromFirestore();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Error deleting task.", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showModifyTaskDialog() {
        if (taskList.isEmpty()) {
            Toast.makeText(this, "No tasks available to modify", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] taskTitles = new String[taskList.size()];
        for (int i = 0; i < taskList.size(); i++) {
            taskTitles[i] = taskList.get(i).getTitle() + " (" + taskList.get(i).getFormattedDate() + ")";
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Task to Modify")
                .setItems(taskTitles, (dialog, which) -> {
                    currentEditingTask = taskList.get(which);
                    isEditingMode = true;
                    selectedDate = currentEditingTask.getDate();
                    updateDateDisplay();
                    layoutTaskInput.setVisibility(View.VISIBLE);
                    etTaskTitle.setText(currentEditingTask.getTitle());
                    etTaskTitle.requestFocus();
                }).show();
    }

    private void showReminderDialog() {
        if (taskList.isEmpty()) {
            Toast.makeText(this, "No tasks available to add reminder", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] taskTitles = new String[taskList.size()];
        for (int i = 0; i < taskList.size(); i++) {
            taskTitles[i] = taskList.get(i).getTitle() + " (" + taskList.get(i).getFormattedDate() + ")";
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Task for Reminder")
                .setItems(taskTitles, (dialog, which) -> {
                    Task selectedTask = taskList.get(which);
                    showTimePickerForReminder(selectedTask);
                }).show();
    }

    private void showTimePickerForReminder(Task task) {
        Calendar calendar = Calendar.getInstance();
        if (task.getDate() != null) {
            calendar.setTime(task.getDate());
        }

        new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    String reminderTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                    db.collection("tasks").document(task.getTaskId())
                            .update("reminder", reminderTime, "hasReminder", true)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Reminder set for " + reminderTime, Toast.LENGTH_SHORT).show();
                                loadTasksFromFirestore();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Error setting reminder.", Toast.LENGTH_SHORT).show());
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
        ).show();
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        if (selectedDate != null) {
            calendar.setTime(selectedDate);
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(year, month, dayOfMonth);
                    selectedDate = selectedCalendar.getTime();
                    updateDateDisplay();
                    showTaskInput();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void updateDateDisplay() {
        if (selectedDate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
            tvSelectedDate.setText(sdf.format(selectedDate));
        } else {
            tvSelectedDate.setText("No Date Selected");
        }
    }

    private void refreshTaskList() {
        layoutTaskList.removeAllViews();
        for (Task task : taskList) {
            View taskView = createTaskView(task);
            layoutTaskList.addView(taskView);
        }
    }

    private View createTaskView(Task task) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View taskView = inflater.inflate(R.layout.task_item, layoutTaskList, false);

        TextView tvTaskTitle = taskView.findViewById(R.id.tvTaskTitle);
        TextView tvTaskDate = taskView.findViewById(R.id.tvTaskDate);
        TextView tvTaskReminder = taskView.findViewById(R.id.tvTaskReminder);

        tvTaskTitle.setText(task.getTitle());
        tvTaskDate.setText(task.getFormattedDate());

        if (task.hasReminder()) {
            tvTaskReminder.setText("Reminder: " + task.getReminder());
            tvTaskReminder.setVisibility(View.VISIBLE);
        } else {
            tvTaskReminder.setVisibility(View.GONE);
        }

        return taskView;
    }

    private void showTaskInput() {
        layoutTaskInput.setVisibility(View.VISIBLE);
        etTaskTitle.setText("");
        etTaskTitle.requestFocus();
        isEditingMode = false;
        currentEditingTask = null;
    }

    private void hideTaskInput() {
        layoutTaskInput.setVisibility(View.GONE);
        etTaskTitle.setText("");
        isEditingMode = false;
        currentEditingTask = null;
    }

    private void cancelTask() {
        hideTaskInput();
    }

    private void showPlusMenu() {
        if (isGuest) {
            Toast.makeText(this, "Guest users cannot add tasks.", Toast.LENGTH_SHORT).show();
            return;
        }
        PopupMenu popupMenu = new PopupMenu(this, btnPlus);
        popupMenu.getMenuInflater().inflate(R.menu.plus_menu, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_add_task) {
                showDatePicker();
                return true;
            } else if (itemId == R.id.menu_modify_task) {
                showModifyTaskDialog();
                return true;
            } else if (itemId == R.id.menu_add_reminder) {
                showReminderDialog();
                return true;
            } else if (itemId == R.id.menu_delete_task) {
                if (taskList.isEmpty()) {
                    Toast.makeText(this, "No tasks to delete", Toast.LENGTH_SHORT).show();
                    return true;
                }
                String[] taskTitles = new String[taskList.size()];
                for (int i = 0; i < taskList.size(); i++) {
                    taskTitles[i] = taskList.get(i).getTitle();
                }
                new AlertDialog.Builder(this)
                        .setTitle("Select task to delete")
                        .setItems(taskTitles, (dialog, which) -> showDeleteConfirmationDialog(taskList.get(which)))
                        .show();
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    private void updatePlusButtonVisibility() {
        if(isGuest) {
            btnPlus.setVisibility(View.GONE);
        } else {
            btnPlus.setVisibility(taskList.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }
}
package com.example.myapplication;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RemindersActivity extends AppCompatActivity implements ReminderAdapter.OnToggleListener {

    private RecyclerView rvReminders;
    private ReminderAdapter adapter;
    private List<Reminder> reminderList;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private ConstraintLayout rootLayout;
    private ImageView btnBack;

    private boolean isGuest;

    private static final String PREFS_NAME = "AppPreferences";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_BACKGROUND_COLOR = "background_color";
    private static final int NOTIFICATION_PERMISSION_CODE = 101;

    private boolean currentDarkMode;

    private BroadcastReceiver refreshReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ReminderReciever.ACTION_REFRESH_REMINDERS.equals(intent.getAction())) {
                if (!isGuest) {
                    loadRemindersFromFirestore();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminders);

        isGuest = getIntent().getBooleanExtra("isGuest", false);
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null && !isGuest) {
            Toast.makeText(this, "Please log in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        createNotificationChannel();
        requestNotificationPermission();

        rootLayout = findViewById(R.id.rootLayout);
        rvReminders = findViewById(R.id.rv_reminders);
        rvReminders.setLayoutManager(new LinearLayoutManager(this));
        reminderList = new ArrayList<>();
        adapter = new ReminderAdapter(reminderList, this, isGuest);
        rvReminders.setAdapter(adapter);
        btnBack = findViewById(R.id.iv_back);

        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(RemindersActivity.this, DashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish(); // Optional: finish current activity
        });


        applyCustomizations();
        if (!isGuest) {
            loadRemindersFromFirestore();
        }

        // Register the broadcast receiver
        IntentFilter filter = new IntentFilter(ReminderReciever.ACTION_REFRESH_REMINDERS);
        ContextCompat.registerReceiver(this, refreshReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister the broadcast receiver
        unregisterReceiver(refreshReceiver);
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
        if (!isGuest) {
            loadRemindersFromFirestore(); // Also refresh on resume
        }
    }

    private void applyTheme() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        currentDarkMode = prefs.getBoolean(KEY_DARK_MODE, false);
        if (currentDarkMode) {
            setTheme(R.style.Theme_MyApplication_Dark);
        } else {
            setTheme(R.style.Theme_MyApplication);
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

        if (adapter != null) {
            adapter.setDarkMode(isDarkMode);
            adapter.notifyDataSetChanged();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "reminder_channel",
                    "Task Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Channel for task reminder notifications");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Notification permission denied. You may not receive reminders.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void loadRemindersFromFirestore() {
        Map<String, Reminder> uniqueReminders = new HashMap<>();

        db.collection("tasks")
                .whereEqualTo("userId", currentUser.getUid())
                .whereEqualTo("hasReminder", true)
                .get()
                .addOnSuccessListener(query -> {
                    for (QueryDocumentSnapshot document : query) {
                        String id = document.getId();
                        String title = document.getString("title");
                        String time = document.getString("reminder"); // HH:mm
                        boolean hasReminder = document.getBoolean("hasReminder") != null &&
                                document.getBoolean("hasReminder");
                        uniqueReminders.put(id, new Reminder(
                                id,
                                title != null ? title : "Untitled",
                                time != null ? time : "",
                                hasReminder
                        ));
                    }
                    reminderList.clear();
                    reminderList.addAll(uniqueReminders.values());
                    adapter.notifyDataSetChanged();

                    for (Reminder r : reminderList) {
                        if (r.isHasReminder()) {
                            scheduleAlarmForReminder(r);
                        }
                    }

                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load reminders", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onToggle(Reminder reminder, boolean enabled) {
        if (isGuest) {
            Toast.makeText(this, "Guest users cannot modify reminders.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (enabled) {
            db.collection("tasks").document(reminder.getTaskId())
                    .update("hasReminder", true)
                    .addOnSuccessListener(unused -> {
                        scheduleAlarmForReminder(reminder);
                        Toast.makeText(this, "Reminder enabled", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to update reminder", Toast.LENGTH_SHORT).show());
        } else {
            cancelAlarmForReminder(reminder);
            scheduleDeletionAlarm(reminder);
            Toast.makeText(this, "Reminder will be removed at the scheduled time", Toast.LENGTH_SHORT).show();
        }
    }

    private void scheduleDeletionAlarm(Reminder reminder) {
        if (reminder.getTime() == null || !reminder.getTime().contains(":")) return;

        String[] parts = reminder.getTime().split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);

        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        Intent intent = new Intent(this, ReminderReciever.class);
        intent.setAction(ReminderReciever.ACTION_DELETE_REMINDER);
        intent.putExtra("taskId", reminder.getTaskId());

        int requestCode = reminder.getTaskId().hashCode();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (am != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                Intent permissionIntent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(permissionIntent);
                Toast.makeText(this, "Please grant permission to schedule exact alarms.", Toast.LENGTH_LONG).show();
                return;
            }
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }
    }

    private void scheduleAlarmForReminder(Reminder reminder) {
        if (reminder.getTime() == null || !reminder.getTime().contains(":")) return;

        String[] parts = reminder.getTime().split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);

        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        Intent intent = new Intent(this, ReminderReciever.class);
        intent.putExtra("taskTitle", reminder.getTitle());
        intent.putExtra("taskId", reminder.getTaskId());

        int requestCode = reminder.getTaskId().hashCode();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (am != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                Intent permissionIntent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(permissionIntent);
                Toast.makeText(this, "Please grant permission to schedule exact alarms.", Toast.LENGTH_LONG).show();
                return;
            }
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }
    }

    private void cancelAlarmForReminder(Reminder reminder) {
        Intent intent = new Intent(this, ReminderReciever.class);
        int requestCode = reminder.getTaskId().hashCode();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (am != null) {
            am.cancel(pendingIntent);
        }
    }
}
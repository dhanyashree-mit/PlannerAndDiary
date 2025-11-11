package com.example.myapplication;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.firestore.FirebaseFirestore;

public class ReminderReciever extends BroadcastReceiver {

    public static final String ACTION_REFRESH_REMINDERS = "com.example.myapplication.ACTION_REFRESH_REMINDERS";
    public static final String ACTION_DELETE_REMINDER = "com.example.myapplication.ACTION_DELETE_REMINDER";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String taskId = intent.getStringExtra("taskId");

        // This handles the delayed deletion when a user toggles a reminder off.
        if (ACTION_DELETE_REMINDER.equals(action)) {
            if (taskId != null) {
                FirebaseFirestore.getInstance().collection("tasks").document(taskId)
                        .update("hasReminder", false)
                        .addOnSuccessListener(aVoid -> {
                            Log.d("ReminderReceiver", "Task reminder disabled via toggle for: " + taskId);
                            Intent refreshIntent = new Intent(ACTION_REFRESH_REMINDERS);
                            context.sendBroadcast(refreshIntent);
                        });
            }
        } else {
            // This handles the standard reminder notification.
            String taskTitle = intent.getStringExtra("taskTitle");

            // 1. Show the notification.
            Intent activityIntent = new Intent(context, RemindersActivity.class);
            PendingIntent contentIntent = PendingIntent.getActivity(
                    context, 0, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "reminder_channel")
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("Task Reminder")
                    .setContentText(taskTitle != null ? taskTitle : "Reminder")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(contentIntent);

            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.notify(taskId != null ? taskId.hashCode() : 0, builder.build());
            }

            // 2. Update the task in Firestore to remove it from the list.
            if (taskId != null) {
                FirebaseFirestore.getInstance().collection("tasks").document(taskId)
                        .update("hasReminder", false)
                        .addOnSuccessListener(aVoid -> {
                            Log.d("ReminderReceiver", "Task reminder processed and disabled for: " + taskId);
                            Intent refreshIntent = new Intent(ACTION_REFRESH_REMINDERS);
                            context.sendBroadcast(refreshIntent);
                        });
            }
        }
    }
}
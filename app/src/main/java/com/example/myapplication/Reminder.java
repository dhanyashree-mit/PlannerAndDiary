package com.example.myapplication;

public class Reminder {
    private String taskId;
    private String title;
    private String time; // "HH:mm" format
    private boolean hasReminder;

    public Reminder() {
        // empty constructor for Firestore
    }

    public Reminder(String taskId, String title, String time, boolean hasReminder) {
        this.taskId = taskId;
        this.title = title;
        this.time = time;
        this.hasReminder = hasReminder;
    }

    public String getTaskId() { return taskId; }
    public String getTitle() { return title; }
    public String getTime() { return time; }
    public boolean isHasReminder() { return hasReminder; }

    public void setTaskId(String taskId) { this.taskId = taskId; }
    public void setTitle(String title) { this.title = title; }
    public void setTime(String time) { this.time = time; }
    public void setHasReminder(boolean hasReminder) { this.hasReminder = hasReminder; }
}

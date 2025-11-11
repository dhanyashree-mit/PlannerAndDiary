package com.example.myapplication;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Task {

    private String taskId;
    private String userId;
    private String title;
    private Date date;
    private String reminder; // contains reminder time like "6:00 am"
    private boolean hasReminder;
    private String time;
    private boolean isCompleted;

    public Task() {}

    public Task(String userId, String title, Date date) {
        this.userId = userId;
        this.title = title;
        this.date = date;
        this.hasReminder = false;
        this.reminder = null;
        this.isCompleted = false;
        this.time = "";
    }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getUserId() { return userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }

    public String getReminder() { return reminder; }
    public void setReminder(String reminder) { this.reminder = reminder; }

    public boolean hasReminder() { return hasReminder; }
    public void setHasReminder(boolean hasReminder) { this.hasReminder = hasReminder; }

    public String getTime() { return time.isEmpty() ? "-" : time; }
    public void setTime(String time) { this.time = time; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }

    public String getFormattedDate() {
        if (this.date != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
            return sdf.format(this.date);
        }
        return "N/A";
    }
}
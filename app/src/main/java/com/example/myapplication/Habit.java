package com.example.myapplication;

import com.google.firebase.firestore.Exclude;

public class Habit {
    private String habitId;
    private String name;
    private boolean isCompleted;
    private int streak;
    private String userId;

    public Habit() {}  // Firestore requires no-arg constructor

    public Habit(String name, String userId) {
        this.name = name;
        this.userId = userId;
        this.isCompleted = false;
        this.streak = 0;
    }

    public String getHabitId() { return habitId; }
    public void setHabitId(String habitId) { this.habitId = habitId; }

    public String getName() { return name; }
    public boolean isCompleted() { return isCompleted; }
    public int getStreak() { return streak; }
    public String getUserId() { return userId; }

    // Simple setter for Firestore and initial setup
    public void setCompleted(boolean completed) {
        this.isCompleted = completed;
    }

    public void setStreak(int streak) { this.streak = streak; }

    // Handles the logic when a checkbox is toggled
    public void handleCheck(boolean isChecked) {
        if (isChecked && !this.isCompleted) { // If checking an unchecked box
            this.streak++;
        } else if (!isChecked && this.isCompleted) { // If unchecking a checked box
            this.streak--;
            if (this.streak < 0) {
                this.streak = 0;
            }
        }
        this.isCompleted = isChecked;
    }

    @Exclude
    public boolean isEmpty() { return name == null || name.trim().isEmpty(); }
}

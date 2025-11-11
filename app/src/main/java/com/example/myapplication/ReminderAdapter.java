package com.example.myapplication;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ReminderAdapter extends RecyclerView.Adapter<ReminderAdapter.ViewHolder> {

    public interface OnToggleListener {
        void onToggle(Reminder reminder, boolean enabled);
    }

    private List<Reminder> reminders;
    private OnToggleListener listener;
    private boolean isDarkMode = false;
    private boolean isGuest;

    public ReminderAdapter(List<Reminder> reminders, OnToggleListener listener, boolean isGuest) {
        this.reminders = reminders;
        this.listener = listener;
        this.isGuest = isGuest;
    }

    public void setDarkMode(boolean darkMode) {
        isDarkMode = darkMode;
    }

    @NonNull
    @Override
    public ReminderAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reminder, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReminderAdapter.ViewHolder holder, int position) {
        Reminder reminder = reminders.get(position);
        holder.tvTaskName.setText(reminder.getTitle());
        holder.tvTime.setText(reminder.getTime());

        int textColor = isDarkMode ? Color.WHITE : Color.BLACK;
        holder.tvTaskName.setTextColor(textColor);
        holder.tvTime.setTextColor(textColor);

        holder.toggle.setOnCheckedChangeListener(null); // reset listener to avoid recycling issues
        holder.toggle.setChecked(reminder.isHasReminder());

        if (isGuest) {
            holder.toggle.setEnabled(false);
            holder.toggle.setOnClickListener(v -> Toast.makeText(holder.itemView.getContext(), "Guest users cannot modify reminders.", Toast.LENGTH_SHORT).show());
        } else {
            holder.toggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
                reminder.setHasReminder(isChecked);
                if (listener != null) listener.onToggle(reminder, isChecked);
            });
        }
    }

    @Override
    public int getItemCount() { return reminders.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTaskName, tvTime;
        SwitchCompat toggle;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTaskName = itemView.findViewById(R.id.tv_task_name);
            tvTime = itemView.findViewById(R.id.tv_time);
            toggle = itemView.findViewById(R.id.switch_reminder);
        }
    }
}

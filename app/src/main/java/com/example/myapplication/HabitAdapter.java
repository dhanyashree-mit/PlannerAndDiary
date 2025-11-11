package com.example.myapplication;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class HabitAdapter extends RecyclerView.Adapter<HabitAdapter.HabitViewHolder> {

    private List<Habit> habitList;
    private OnHabitToggleListener listener;
    private FirebaseFirestore db;
    private boolean isDarkMode = false;

    public interface OnHabitToggleListener {
        void onHabitToggle(Habit habit);
    }

    public HabitAdapter(List<Habit> habitList, OnHabitToggleListener listener) {
        this.habitList = habitList;
        this.listener = listener;
        db = FirebaseFirestore.getInstance();
    }

    public void setDarkMode(boolean darkMode) {
        isDarkMode = darkMode;
    }

    @NonNull
    @Override
    public HabitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_habit, parent, false);
        return new HabitViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HabitViewHolder holder, int position) {
        Habit habit = habitList.get(position);

        holder.textViewHabitName.setText(habit.getName());
        holder.textViewStreak.setText(String.valueOf(habit.getStreak()));

        if (isDarkMode) {
            holder.itemView.setBackgroundColor(Color.parseColor("#121212"));
            holder.textViewHabitName.setTextColor(Color.WHITE);
            holder.textViewStreak.setTextColor(Color.LTGRAY);
        } else {
            holder.itemView.setBackgroundColor(Color.WHITE);
            holder.textViewHabitName.setTextColor(Color.BLACK);
            holder.textViewStreak.setTextColor(Color.DKGRAY);
        }

        holder.checkBoxHabit.setOnCheckedChangeListener(null);
        holder.checkBoxHabit.setChecked(habit.isCompleted());
        holder.checkBoxHabit.setOnCheckedChangeListener((buttonView, isChecked) -> {
            habit.handleCheck(isChecked);
            if (habit.getHabitId() != null && !habit.getHabitId().isEmpty()) {
                db.collection("habits")
                        .document(habit.getHabitId())
                        .update("isCompleted", habit.isCompleted(), "streak", habit.getStreak());
            }
            listener.onHabitToggle(habit);
            holder.textViewStreak.setText(String.valueOf(habit.getStreak()));
        });
    }

    @Override
    public int getItemCount() {
        return habitList.size();
    }

    static class HabitViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBoxHabit;
        TextView textViewHabitName;
        TextView textViewStreak;

        public HabitViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBoxHabit = itemView.findViewById(R.id.checkBoxHabit);
            textViewHabitName = itemView.findViewById(R.id.textViewHabitName);
            textViewStreak = itemView.findViewById(R.id.textViewStreak);
        }
    }
}

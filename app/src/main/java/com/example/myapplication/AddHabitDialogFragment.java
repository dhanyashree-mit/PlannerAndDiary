package com.example.myapplication;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

public class AddHabitDialogFragment extends BottomSheetDialogFragment {

    private EditText editTextHabitTitle;
    private OnHabitAddedListener listener;

    public interface OnHabitAddedListener {
        void onHabitAdded(String habitTitle);
    }

    public void setOnHabitAddedListener(OnHabitAddedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.dialog_add_habit, container, false);

        editTextHabitTitle = view.findViewById(R.id.editTextHabitTitle);
        MaterialButton buttonSave = view.findViewById(R.id.buttonSave);
        MaterialButton buttonCancel = view.findViewById(R.id.buttonCancel);
        View rootLayout = view.findViewById(R.id.dialogRootLayout);

        // Check dark mode preference
        boolean isDarkMode = false;
        if (getActivity() != null) {
            SharedPreferences prefs = getActivity().getSharedPreferences("AppPreferences", FragmentActivity.MODE_PRIVATE);
            isDarkMode = prefs.getBoolean("dark_mode", false);
        }

        // Apply dark/light theme to dialog
        if (isDarkMode) {
            rootLayout.setBackgroundColor(Color.parseColor("#121212")); // dark background
            editTextHabitTitle.setBackgroundColor(Color.parseColor("#1E1E1E"));
            editTextHabitTitle.setTextColor(Color.WHITE);
            editTextHabitTitle.setHintTextColor(Color.LTGRAY);
            buttonSave.setBackgroundColor(Color.DKGRAY);
            buttonCancel.setBackgroundColor(Color.DKGRAY);
        } else {
            rootLayout.setBackgroundColor(Color.parseColor("#E1D5E7")); // light purple background
            editTextHabitTitle.setBackgroundColor(Color.WHITE);
            editTextHabitTitle.setTextColor(Color.BLACK);
            editTextHabitTitle.setHintTextColor(Color.DKGRAY);
            buttonSave.setBackgroundColor(Color.parseColor("#2710F7"));
            buttonCancel.setBackgroundColor(Color.parseColor("#2710F7"));
        }

        buttonSave.setOnClickListener(v -> {
            String title = editTextHabitTitle.getText().toString().trim();
            if (!title.isEmpty() && listener != null) {
                listener.onHabitAdded(title);
                dismiss();
            }
        });

        buttonCancel.setOnClickListener(v -> dismiss());

        return view;
    }
}

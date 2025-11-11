package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class Onboarding extends AppCompatActivity {

    private static final String PREF_NAME = "onboarding_pref";
    private static final String IS_FIRST_TIME = "isFirstTime";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        Button getStartedButton = findViewById(R.id.getStartedButton);
        getStartedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Mark onboarding as completed
                SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean(IS_FIRST_TIME, false);
                editor.apply();

                // Go to login screen
                Intent intent = new Intent(Onboarding.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}

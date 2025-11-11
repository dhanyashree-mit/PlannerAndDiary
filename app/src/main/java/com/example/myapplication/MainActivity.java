package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final int SPLASH_DISPLAY_LENGTH = 3000; // 3 seconds
    private static final String PREF_NAME = "onboarding_pref";
    private static final String IS_FIRST_TIME = "isFirstTime";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new Handler().postDelayed(() -> {
            SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
            boolean isFirstTime = prefs.getBoolean(IS_FIRST_TIME, true);

            Intent intent;
            if (isFirstTime) {
                intent = new Intent(MainActivity.this, Onboarding.class);
            } else {
                intent = new Intent(MainActivity.this, LoginActivity.class);
            }

            startActivity(intent);
            finish();
        }, SPLASH_DISPLAY_LENGTH);
    }
}

package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private EditText emailInput, passwordInput;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Check if already logged in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            navigateToDashboard(false);
            return;
        }

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        Button loginButton = findViewById(R.id.loginButton);
        Button registerButton = findViewById(R.id.registerButton);
        TextView forgotPasswordText = findViewById(R.id.forgotPasswordText);
        TextView guestModeText = findViewById(R.id.guestModeText);

        loginButton.setOnClickListener(v -> loginUser());
        registerButton.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, Register.class)));
        forgotPasswordText.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class)));
        guestModeText.setOnClickListener(v -> {
            navigateToDashboard(true);
        });
    }

    private void loginUser() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Email is required");
            emailInput.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("Password is required");
            passwordInput.requestFocus();
            return;
        }

        Toast.makeText(this, "Logging in...", Toast.LENGTH_SHORT).show();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Login success
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Check if user details already exist in Firestore
                            db.collection("users").document(user.getUid())
                                    .get()
                                    .addOnSuccessListener(documentSnapshot -> {
                                        if (!documentSnapshot.exists()) {
                                            // User's details are not in Firestore, so create a new document
                                            Map<String, Object> userDetails = new HashMap<>();
                                            userDetails.put("email", user.getEmail());
                                            // You can add more fields here if available, e.g., user name from a previous screen

                                            db.collection("users").document(user.getUid())
                                                    .set(userDetails)
                                                    .addOnSuccessListener(aVoid -> {
                                                        Toast.makeText(LoginActivity.this, "Welcome back!", Toast.LENGTH_SHORT).show();
                                                        navigateToDashboard(false);
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Toast.makeText(LoginActivity.this, "Error storing user details.", Toast.LENGTH_LONG).show();
                                                        navigateToDashboard(false); // Still navigate to dashboard
                                                    });
                                        } else {
                                            // User details already exist
                                            Toast.makeText(LoginActivity.this, "Welcome back!", Toast.LENGTH_SHORT).show();
                                            navigateToDashboard(false);
                                        }
                                    });
                        } else {
                            // This case should ideally not happen after a successful login
                            Toast.makeText(LoginActivity.this, "Login successful, but user data is null.", Toast.LENGTH_LONG).show();
                            navigateToDashboard(false);
                        }
                    } else {
                        // Login failed
                        Toast.makeText(this, "Login failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void navigateToDashboard(boolean isGuest) {
        Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
        intent.putExtra("isGuest", isGuest);
        startActivity(intent);
        finish();
    }
}
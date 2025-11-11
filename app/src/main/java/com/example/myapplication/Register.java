package com.example.myapplication;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Register extends AppCompatActivity {

    private EditText usernameInput, passwordInput, emailInput, phoneInput;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize input fields
        usernameInput = findViewById(R.id.usernameInput);
        passwordInput = findViewById(R.id.passwordInput);
        emailInput = findViewById(R.id.emailInput);
        phoneInput = findViewById(R.id.phoneInput);

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish(); // Go back to previous activity (LoginActivity)
            }
        });

        Button registerButton = findViewById(R.id.registerButton);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Validate input fields before proceeding
                if (validateInputs()) {
                    registerUser();
                }
            }
        });
    }

    private void registerUser() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String username = usernameInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();

        // Check if email is already registered
        mAuth.fetchSignInMethodsForEmail(email)
                .addOnSuccessListener(result -> {
                    if (!result.getSignInMethods().isEmpty()) {
                        Toast.makeText(Register.this, "Email address is already registered. Please login or use forgot password.", Toast.LENGTH_LONG).show();
                    } else {
                        // Show loading indicator
                        Toast.makeText(Register.this, "Creating account...", Toast.LENGTH_SHORT).show();

                        mAuth.createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener(this, task -> {
                                    if (task.isSuccessful()) {
                                        // Registration success
                                        FirebaseUser user = mAuth.getCurrentUser();
                                        saveUserProfile(user.getUid(), username, email, phone, password);
                                    } else {
                                        // Registration failed
                                        Toast.makeText(Register.this, "Registration failed: " + task.getException().getMessage(),
                                                Toast.LENGTH_LONG).show();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Register.this, "Error checking email: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveUserProfile(String userId, String username, String email, String phone, String password) {
        Map<String, Object> user = new HashMap<>();
        user.put("username", username);
        user.put("email", email);
        user.put("phone", phone);
        user.put("password", password);
        user.put("createdAt", new java.util.Date());

        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(Register.this, "Account created successfully! Please login.", Toast.LENGTH_SHORT).show();
                    FirebaseAuth.getInstance().signOut();
                    // Navigate to login page
                    Intent intent = new Intent(Register.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Register.this, "Failed to save profile: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private boolean validateInputs() {
        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();

        // Validate username
        if (TextUtils.isEmpty(username)) {
            usernameInput.setError("Username is required");
            usernameInput.requestFocus();
            return false;
        }

        if (username.length() < 3) {
            usernameInput.setError("Username must be at least 3 characters long");
            usernameInput.requestFocus();
            return false;
        }

        // Validate email
        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Email is required");
            emailInput.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Please enter a valid email address");
            emailInput.requestFocus();
            return false;
        }

        // Validate phone number
        if (TextUtils.isEmpty(phone)) {
            phoneInput.setError("Phone number is required");
            phoneInput.requestFocus();
            return false;
        }

        if (phone.length() < 10) {
            phoneInput.setError("Please enter a valid 10-digit phone number");
            phoneInput.requestFocus();
            return false;
        }

        if (!Patterns.PHONE.matcher(phone).matches()) {
            phoneInput.setError("Please enter a valid phone number");
            phoneInput.requestFocus();
            return false;
        }

        // Validate password
        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("Password is required");
            passwordInput.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            passwordInput.setError("Password must be at least 6 characters long");
            passwordInput.requestFocus();
            return false;
        }

        // Check for password complexity (at least one letter and one number)
        if (!password.matches(".*[A-Za-z].*") || !password.matches(".*[0-9].*")) {
            passwordInput.setError("Password must contain at least one letter and one number");
            passwordInput.requestFocus();
            return false;
        }

        return true;
    }
}

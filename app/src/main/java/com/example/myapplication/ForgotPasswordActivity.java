package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText emailInput;
    private Button sendResetLinkButton;
    private TextView stepTitle;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews();

        sendResetLinkButton.setOnClickListener(view -> {
            if (validateEmail()) {
                sendPasswordResetLink();
            }
        });
    }

    private void initializeViews() {
        emailInput = findViewById(R.id.emailInput);
        sendResetLinkButton = findViewById(R.id.sendResetLinkButton);
        stepTitle = findViewById(R.id.stepTitle);
        stepTitle.setText("Forgot Password?");

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(view -> finish());
    }

    private boolean validateEmail() {
        String email = emailInput.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Email is required");
            emailInput.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Enter a valid email");
            emailInput.requestFocus();
            return false;
        }

        return true;
    }

    private void sendPasswordResetLink() {
        String userEmail = emailInput.getText().toString().trim();
        sendResetLinkButton.setEnabled(false);
        sendResetLinkButton.setText("Sending...");

        // First, check if the email exists in Firestore
        db.collection("users")
                .whereEqualTo("email", userEmail)
                .get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()) {
                        // Email does not exist in your Firestore 'users' collection
                        Toast.makeText(this, "Email not registered. Please sign up first.", Toast.LENGTH_LONG).show();
                        sendResetLinkButton.setEnabled(true);
                        sendResetLinkButton.setText("Send Reset Link");
                    } else {
                        // Email exists in Firestore, so send the reset link via Firebase Auth
                        mAuth.sendPasswordResetEmail(userEmail)
                                .addOnCompleteListener(task -> {
                                    sendResetLinkButton.setEnabled(true);
                                    sendResetLinkButton.setText("Send Reset Link");
                                    if (task.isSuccessful()) {
                                        Toast.makeText(this, "Password reset link sent to " + userEmail, Toast.LENGTH_LONG).show();
                                        finish(); // Go back to login screen
                                    } else {
                                        // This handles cases where the email might be valid but not associated with a Firebase Auth user
                                        Toast.makeText(this, "Failed to send reset link. Please try again.", Toast.LENGTH_LONG).show();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    sendResetLinkButton.setEnabled(true);
                    sendResetLinkButton.setText("Send Reset Link");
                });
    }
}
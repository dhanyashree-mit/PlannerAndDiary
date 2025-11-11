

        package com.example.myapplication;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DiaryEditorActivity extends AppCompatActivity {

    EditText editTitle, editContent;
    Button btnA, btnH1, btnH2, btnBold, btnItalic, btnUnderline, btnCancel, btnSave;
    ImageButton btnBack, btnMenu;
    TextView txtTimestamp;
    android.widget.HorizontalScrollView formatToolbar;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    private String diaryId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diary_editor);

        // Firebase setup
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            mAuth.signInAnonymously().addOnSuccessListener(a -> currentUser = mAuth.getCurrentUser());
        }

        // Initialize views
        editTitle = findViewById(R.id.editTitle);
        editContent = findViewById(R.id.editContent);
        btnA = findViewById(R.id.btnShowToolbar);
        btnBack = findViewById(R.id.btnBack);
        btnMenu = findViewById(R.id.btnMenu);
        btnSave = findViewById(R.id.btnSave);
        txtTimestamp = findViewById(R.id.txtTimestamp);
        formatToolbar = findViewById(R.id.formatScroll);

        btnH1 = findViewById(R.id.btnH1);
        btnH2 = findViewById(R.id.btnH2);
        btnBold = findViewById(R.id.btnBold);
        btnItalic = findViewById(R.id.btnItalic);
        btnUnderline = findViewById(R.id.btnUnderline);
        btnCancel = findViewById(R.id.btnCancel);

        // Check if editing existing diary
        diaryId = getIntent().getStringExtra("diaryId");
        if (diaryId != null) {
            loadDiaryFromFirebase(diaryId);
        }

        String currentDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        txtTimestamp.setText(currentDate);

        // Button listeners
        btnBack.setOnClickListener(v -> finish());
        btnA.setOnClickListener(v -> formatToolbar.setVisibility(formatToolbar.getVisibility() == View.GONE ? View.VISIBLE : View.GONE));
        btnMenu.setOnClickListener(this::showPopupMenu);
        btnSave.setOnClickListener(v -> saveDiaryToFirebase()); // ✅ Save button now works

        // Formatting buttons
        btnH1.setOnClickListener(v -> editContent.setTextSize(24f));
        btnH2.setOnClickListener(v -> editContent.setTextSize(20f));
        btnBold.setOnClickListener(v -> applyStyle(Typeface.BOLD));
        btnItalic.setOnClickListener(v -> applyStyle(Typeface.ITALIC));
        btnUnderline.setOnClickListener(v -> applyUnderline());
        btnCancel.setOnClickListener(v -> {
            editContent.setTextSize(16f);
            editContent.setTypeface(Typeface.DEFAULT);
        });
    }

    private void applyStyle(int style) {
        int start = editContent.getSelectionStart();
        int end = editContent.getSelectionEnd();
        if (start >= end) {
            Toast.makeText(this, "Please select content", Toast.LENGTH_SHORT).show();
            return;
        }

        SpannableStringBuilder ssb = new SpannableStringBuilder(editContent.getText());
        ssb.setSpan(new StyleSpan(style), start, end, 0);
        editContent.setText(ssb);
        editContent.setSelection(end);
    }

    private void applyUnderline() {
        int start = editContent.getSelectionStart();
        int end = editContent.getSelectionEnd();
        if (start >= end) {
            Toast.makeText(this, "Please select content", Toast.LENGTH_SHORT).show();
            return;
        }

        SpannableStringBuilder ssb = new SpannableStringBuilder(editContent.getText());
        ssb.setSpan(new UnderlineSpan(), start, end, 0);
        editContent.setText(ssb);
        editContent.setSelection(end);
    }


    private void showPopupMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        // ✅ Removed "Save" option
        popup.getMenu().add("Delete");
        popup.getMenu().add("Share");

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getTitle().toString()) {
                case "Delete":
                    deleteDiary();
                    break;
                case "Share":
                    shareDiary();
                    break;
            }
            return true;
        });
        popup.show();
    }

    private void saveDiaryToFirebase() {
        if (currentUser == null) return;

        String title = editTitle.getText().toString().trim();
        String content = editContent.getText().toString().trim();
        String timestamp = txtTimestamp.getText().toString();

        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(this, "Please fill in title and content", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference ref = (diaryId == null)
                ? db.collection("diaries").document()
                : db.collection("diaries").document(diaryId);

        ref.set(new DiaryModel(currentUser.getUid(), title, content, timestamp))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Diary saved successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadDiaryFromFirebase(String id) {
        db.collection("diaries").document(id)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        editTitle.setText(doc.getString("title"));
                        editContent.setText(doc.getString("content"));
                        txtTimestamp.setText(doc.getString("timestamp"));
                    }
                });
    }

    private void deleteDiary() {
        if (diaryId == null) {
            Toast.makeText(this, "Nothing to delete", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("diaries").document(diaryId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Deleted successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show());
    }

    private void shareDiary() {
        String title = editTitle.getText().toString();
        String content = editContent.getText().toString();
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, title);
        shareIntent.putExtra(Intent.EXTRA_TEXT, content);
        startActivity(Intent.createChooser(shareIntent, "Share diary via"));
    }
}
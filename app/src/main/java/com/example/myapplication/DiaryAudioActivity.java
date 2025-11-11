package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateFormat;
import android.util.Base64;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DiaryAudioActivity extends AppCompatActivity {

    private ImageButton btnRecord, btnPlay, btnSave, btnBack;
    private TextView tvTimer, tvDate;
    private RecyclerView recyclerView;
    private ConstraintLayout rootLayout;

    private AudioRecord audioRecord;
    private MediaPlayer mediaPlayer;
    private boolean isRecording = false;
    private boolean isPlaying = false;
    private boolean hasRecording = false;
    private boolean isGuest;

    private ByteArrayOutputStream audioData;
    private byte[] audioBytes;
    private Handler timerHandler = new Handler();
    private long startTime = 0;

    private FirebaseFirestore firestore;
    private ArrayList<AudioModel> audioList = new ArrayList<>();
    private AudioAdapter audioAdapter;

    private static final int REQUEST_PERMISSION_CODE = 1000;
    private static final int SAMPLE_RATE = 44100;

    private static final String PREFS_NAME = "AppPreferences";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_BACKGROUND_COLOR = "background_color";

    private boolean currentDarkMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diary_audio);

        isGuest = getIntent().getBooleanExtra("isGuest", false);

        FirebaseApp.initializeApp(this);
        firestore = FirebaseFirestore.getInstance();

        rootLayout = findViewById(R.id.root);
        btnRecord = findViewById(R.id.btn_record);
        btnPlay = findViewById(R.id.btn_play);
        btnSave = findViewById(R.id.btn_save);
        btnBack = findViewById(R.id.btn_back);
        tvTimer = findViewById(R.id.tv_timer);
        tvDate = findViewById(R.id.tv_date);
        recyclerView = findViewById(R.id.recycler_audio);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        audioAdapter = new AudioAdapter(audioList, firestore, isGuest);
        recyclerView.setAdapter(audioAdapter);

        btnSave.setEnabled(false);

        if (!checkPermission()) requestPermission();

        // Set the date
        String dateString = DateFormat.format("dd/MM/yy", new Date()).toString();
        tvDate.setText("Date: " + dateString);

        btnRecord.setOnClickListener(v -> {
            if (isGuest) {
                Toast.makeText(this, "Guest users cannot record audio.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (isRecording) stopRecording();
            else startRecording();
        });

        btnPlay.setOnClickListener(v -> {
            if (isGuest) {
                Toast.makeText(this, "Guest users cannot play audio.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (isPlaying) stopPlaying();
            else startPlaying();
        });

        btnSave.setOnClickListener(v -> {
            if (isGuest) {
                Toast.makeText(this, "Guest users cannot save audio.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (hasRecording) showSaveDialog();
            else Toast.makeText(this, "Record first!", Toast.LENGTH_SHORT).show();
        });

        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(DiaryAudioActivity.this, DashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish(); // Optional: finish current activity
        });

        applyCustomizations();
        if (!isGuest) {
            fetchRecordings();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean(KEY_DARK_MODE, false);
        if (currentDarkMode != isDarkMode) {
            recreate();
        }
        applyCustomizations();
    }

    private void applyTheme() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        currentDarkMode = prefs.getBoolean(KEY_DARK_MODE, false);
        if (currentDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void applyCustomizations() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean(KEY_DARK_MODE, false);

        if (rootLayout != null) {
            if (isDarkMode) {
                rootLayout.setBackgroundColor(Color.BLACK);
                btnBack.setColorFilter(Color.WHITE);
            } else {
                int backgroundColor = prefs.getInt(KEY_BACKGROUND_COLOR, Color.WHITE);
                rootLayout.setBackgroundColor(backgroundColor);
                btnBack.clearColorFilter();
            }
        }

        if (audioAdapter != null) {
            audioAdapter.setDarkMode(isDarkMode);
            audioAdapter.notifyDataSetChanged();
        }
    }

    // ---------------- RECORDING ----------------
    private Thread recordingThread;

    private void startRecording() {
        if (!checkPermission()) {
            requestPermission();
            return;
        }

        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize);

        audioData = new ByteArrayOutputStream();
        isRecording = true;
        btnRecord.setImageResource(R.drawable.ic_stop);
        startTime = System.currentTimeMillis();
        timerHandler.post(updateTimerRunnable);

        recordingThread = new Thread(() -> {
            byte[] buffer = new byte[bufferSize];
            try {
                audioRecord.startRecording();
                while (isRecording) {
                    int read = audioRecord.read(buffer, 0, buffer.length);
                    if (read > 0) audioData.write(buffer, 0, read);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                audioRecord.stop();
                audioRecord.release();
            }

            byte[] pcmData = audioData.toByteArray();
            if (pcmData.length < 100) { // extremely short recording
                pcmData = null;
            }

            if (pcmData != null && pcmData.length > 0) {
                audioBytes = convertToWav(pcmData);
            } else {
                audioBytes = null;
            }

            runOnUiThread(() -> {
                if (audioBytes != null && audioBytes.length > 0) {
                    hasRecording = true;
                    btnSave.setEnabled(true);
                    Toast.makeText(this, "Recording ready to save!", Toast.LENGTH_SHORT).show();
                } else {
                    hasRecording = false;
                    btnSave.setEnabled(false);
                    Toast.makeText(this, "Recording too short or failed!", Toast.LENGTH_LONG).show();
                }
            });
        });
        recordingThread.start();

        Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show();
    }

    private void stopRecording() {
        if (!isRecording) return;
        isRecording = false;
        btnRecord.setImageResource(R.drawable.ic_mic);
        timerHandler.removeCallbacks(updateTimerRunnable);
        tvTimer.setText("00:00:00");
    }

    private Runnable updateTimerRunnable = new Runnable() {
        @Override
        public void run() {
            long elapsed = System.currentTimeMillis() - startTime;
            int seconds = (int) (elapsed / 1000) % 60;
            int minutes = (int) ((elapsed / (1000 * 60)) % 60);
            int hours = (int) ((elapsed / (1000 * 60 * 60)) % 24);
            tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds));
            timerHandler.postDelayed(this, 500);
        }
    };

    // ---------------- PLAYBACK ----------------
    private void startPlaying() {
        if (audioBytes == null || audioBytes.length == 0) {
            Toast.makeText(this, "No recording to play!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            File tempFile = File.createTempFile("temp_audio", ".wav", getCacheDir());
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(audioBytes);
            }

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(tempFile.getAbsolutePath());
            mediaPlayer.prepare();
            mediaPlayer.start();

            isPlaying = true;
            btnPlay.setImageResource(R.drawable.ic_pause);

            mediaPlayer.setOnCompletionListener(mp -> stopPlaying());
            Toast.makeText(this, "Playing recording...", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Playback failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void stopPlaying() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        isPlaying = false;
        btnPlay.setImageResource(R.drawable.ic_play_arrow);
    }

    // ---------------- SAVE TO FIRESTORE ----------------
    private void showSaveDialog() {
        EditText input = new EditText(this);
        input.setHint("Enter recording name");

        new AlertDialog.Builder(this)
                .setTitle("Save Recording")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) saveRecording(name);
                    else Toast.makeText(this, "Enter a valid name", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveRecording(String name) {
        if (isRecording) {
            Toast.makeText(this, "Stop recording first!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (audioBytes == null || audioBytes.length == 0) {
            Toast.makeText(this, "No audio to save!", Toast.LENGTH_SHORT).show();
            return;
        }

        String base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP);

        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("audioData", base64Audio);
        data.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));

        firestore.collection("AudioRecords")
                .add(data)
                .addOnSuccessListener(docRef -> {
                    hasRecording = false;
                    btnSave.setEnabled(false);
                    Toast.makeText(this, "Saved to Firestore!", Toast.LENGTH_SHORT).show();
                    fetchRecordings(); // Refresh the list
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void fetchRecordings() {
        firestore.collection("AudioRecords")
                .orderBy("timestamp")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        audioList.clear();
                        for (DocumentSnapshot doc : task.getResult()) {
                            String name = doc.getString("name");
                            String base64Audio = doc.getString("audioData");
                            String docId = doc.getId();
                            audioList.add(new AudioModel(name, base64Audio, docId));
                        }
                        audioAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(this, "Failed to load recordings", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ---------------- PERMISSIONS ----------------
    private boolean checkPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
            else Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
        }
    }

    // ---------------- WAV CONVERSION ----------------
    private byte[] convertToWav(byte[] pcm) {
        int totalDataLen = pcm.length + 36;
        int byteRate = 16 * SAMPLE_RATE * 1 / 8;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write("RIFF".getBytes());
            out.write(intToByteArray(totalDataLen));
            out.write("WAVE".getBytes());
            out.write("fmt ".getBytes());
            out.write(intToByteArray(16));
            out.write(shortToByteArray((short) 1));
            out.write(shortToByteArray((short) 1));
            out.write(intToByteArray(SAMPLE_RATE));
            out.write(intToByteArray(byteRate));
            out.write(shortToByteArray((short) 2));
            out.write(shortToByteArray((short) 16));
            out.write("data".getBytes());
            out.write(intToByteArray(pcm.length));
            out.write(pcm);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }

    private byte[] intToByteArray(int value) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array();
    }

    private byte[] shortToByteArray(short value) {
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array();
    }
}

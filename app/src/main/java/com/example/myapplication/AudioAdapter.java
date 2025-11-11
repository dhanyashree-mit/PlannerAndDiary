package com.example.myapplication;

import android.app.AlertDialog;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class AudioAdapter extends RecyclerView.Adapter<AudioAdapter.ViewHolder> {

    private ArrayList<AudioModel> list;
    private FirebaseFirestore firestore;
    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;
    private int playingPosition = -1;
    private boolean isDarkMode = false;
    private boolean isGuest;

    public AudioAdapter(ArrayList<AudioModel> list, FirebaseFirestore firestore, boolean isGuest) {
        this.list = list;
        this.firestore = firestore;
        this.isGuest = isGuest;
    }

    public void setDarkMode(boolean darkMode) {
        isDarkMode = darkMode;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_audio, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AudioModel model = list.get(position);
        holder.tvName.setText(model.getName());

        if (isDarkMode) {
            holder.tvName.setTextColor(Color.WHITE);
        } else {
            holder.tvName.setTextColor(Color.BLACK);
        }

        holder.btnPlay.setOnClickListener(v -> {
            if (isGuest) {
                Toast.makeText(holder.itemView.getContext(), "Guest users cannot play audio.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (isPlaying && playingPosition == position) {
                stopAudio(holder);
            } else {
                playAudio(holder, model.getAudioData(), position);
            }
        });

        if (isGuest) {
            holder.btnDelete.setVisibility(View.GONE);
        } else {
            holder.btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(holder.itemView.getContext())
                        .setTitle("Delete Recording")
                        .setMessage("Are you sure you want to delete this recording?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            deleteRecording(model, position, holder);
                        })
                        .setNegativeButton("No", null)
                        .show();
            });
        }
    }

    private void playAudio(ViewHolder holder, String base64Audio, int position) {
        stopAudio(holder); // stop previous if any
        try {
            byte[] audioBytes = Base64.decode(base64Audio, Base64.NO_WRAP);
            File tempFile = File.createTempFile("temp_audio", ".wav", holder.itemView.getContext().getCacheDir());
            FileOutputStream fos = new FileOutputStream(tempFile);
            fos.write(audioBytes);
            fos.close();

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(tempFile.getAbsolutePath());
            mediaPlayer.prepare();
            mediaPlayer.start();
            isPlaying = true;
            playingPosition = position;
            holder.btnPlay.setImageResource(android.R.drawable.ic_media_pause);

            mediaPlayer.setOnCompletionListener(mp -> stopAudio(holder));

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(holder.itemView.getContext(), "Playback failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void stopAudio(ViewHolder holder) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        isPlaying = false;
        playingPosition = -1;
        holder.btnPlay.setImageResource(android.R.drawable.ic_media_play);
    }

    private void deleteRecording(AudioModel model, int position, ViewHolder holder) {
        // Delete from Firestore if docId exists
        if (model.getDocId() != null) {
            firestore.collection("AudioRecords")
                    .document(model.getDocId())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        list.remove(position);
                        notifyItemRemoved(position);
                        Toast.makeText(holder.itemView.getContext(), "Deleted successfully", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(holder.itemView.getContext(), "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Just remove locally
            list.remove(position);
            notifyItemRemoved(position);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        ImageButton btnPlay, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
            btnPlay = itemView.findViewById(R.id.btn_play_item);
            btnDelete = itemView.findViewById(R.id.btn_delete_item);
        }
    }
}

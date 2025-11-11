package com.example.myapplication;

public class AudioModel {
    private String name;
    private String audioData;
    private String docId; // Firestore document ID

    public AudioModel(String name, String audioData) {
        this.name = name;
        this.audioData = audioData;
    }

    public AudioModel(String name, String audioData, String docId) {
        this.name = name;
        this.audioData = audioData;
        this.docId = docId;
    }

    public String getName() { return name; }
    public String getAudioData() { return audioData; }
    public String getDocId() { return docId; }

    public void setDocId(String docId) { this.docId = docId; }
}

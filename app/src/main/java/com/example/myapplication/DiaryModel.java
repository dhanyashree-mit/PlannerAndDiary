package com.example.myapplication;

public class DiaryModel {
    private String id;
    private String userId;
    private String title;
    private String content;
    private String timestamp;

    public DiaryModel() {}

    public DiaryModel(String userId, String title, String content, String timestamp) {
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}

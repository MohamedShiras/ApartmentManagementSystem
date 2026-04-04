package com.example.apartmentmanagementsystem;

public class Notice {
    private String id;
    private String title;
    private String body;
    private String sender;
    private String timeAgo;
    private String badge;

    public Notice(String id, String title, String body, String sender,
                  String timeAgo, String badge, int likes, int comments) {
        this.id = id;
        this.title = title;
        this.body = body;
        this.sender = sender;
        this.timeAgo = timeAgo;
        this.badge = badge;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getSender() { return sender; }
    public String getTimeAgo() { return timeAgo; }
    public String getBadge() { return badge; }
}
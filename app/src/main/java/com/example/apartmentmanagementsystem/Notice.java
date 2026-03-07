package com.example.apartmentmanagementsystem;

public class Notice {
    private String id;
    private String title;
    private String body;
    private String sender;
    private String timeAgo;
    private String badge;
    private int likes;
    private int comments;
    private boolean isLiked;

    public Notice(String id, String title, String body, String sender,
                  String timeAgo, String badge, int likes, int comments) {
        this.id = id;
        this.title = title;
        this.body = body;
        this.sender = sender;
        this.timeAgo = timeAgo;
        this.badge = badge;
        this.likes = likes;
        this.comments = comments;
        this.isLiked = false;
    }

    // Getters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getSender() { return sender; }
    public String getTimeAgo() { return timeAgo; }
    public String getBadge() { return badge; }
    public int getLikes() { return likes; }
    public int getComments() { return comments; }
    public boolean isLiked() { return isLiked; }

    // Setters
    public void setLiked(boolean liked) { this.isLiked = liked; }
    public void setLikes(int likes) { this.likes = likes; }
}
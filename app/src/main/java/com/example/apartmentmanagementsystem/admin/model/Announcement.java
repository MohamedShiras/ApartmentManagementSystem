package com.example.apartmentmanagementsystem.admin.model;

public class Announcement {

    public enum AnnouncementType {
        NOTICE,
        ALERT,
        INFO,
        EVENT,
        UPDATE
    }

    public enum AnnouncementStatus {
        DRAFT,
        SCHEDULED,
        PUBLISHED_LIVE,
        NEEDS_REVIEW
    }

    private final String id;
    private final String title;
    private final String body;
    private final String sender;
    private final String timestamp;
    private final AnnouncementType type;
    private AnnouncementStatus status;
    private int likes;
    private int comments;
    private String audience;

    public Announcement(
            String id,
            String title,
            String body,
            String sender,
            String timestamp,
            AnnouncementType type,
            AnnouncementStatus status,
            String audience
    ) {
        this.id = id;
        this.title = title;
        this.body = body;
        this.sender = sender;
        this.timestamp = timestamp;
        this.type = type;
        this.status = status;
        this.audience = audience;
        this.likes = 0;
        this.comments = 0;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getSender() {
        return sender;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public AnnouncementType getType() {
        return type;
    }

    public AnnouncementStatus getStatus() {
        return status;
    }

    public void setStatus(AnnouncementStatus status) {
        this.status = status;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public int getComments() {
        return comments;
    }

    public void setComments(int comments) {
        this.comments = comments;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }
}


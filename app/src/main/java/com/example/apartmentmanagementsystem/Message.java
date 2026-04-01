package com.example.apartmentmanagementsystem;

public class Message {
    private String id;
    private String userId;
    private String content;
    private String replyToId;
    private String replyToContent;   // filled in at load time
    private String createdAt;
    private String senderName;
    private String senderApartment;
    private int    reactionCount;
    private boolean hasReacted;

    public Message(String id, String userId, String content,
                   String replyToId, String createdAt,
                   String senderName, String senderApartment) {
        this.id              = id;
        this.userId          = userId;
        this.content         = content;
        this.replyToId       = replyToId;
        this.createdAt       = createdAt;
        this.senderName      = senderName;
        this.senderApartment = senderApartment;
        this.reactionCount   = 0;
        this.hasReacted      = false;
    }

    // ── Getters ──────────────────────────────────────────────────────
    public String getId()              { return id; }
    public String getUserId()          { return userId; }
    public String getContent()         { return content; }
    public String getReplyToId()       { return replyToId; }
    public String getReplyToContent()  { return replyToContent; }
    public String getCreatedAt()       { return createdAt; }
    public String getSenderName()      { return senderName; }
    public String getSenderApartment() { return senderApartment; }
    public int    getReactionCount()   { return reactionCount; }
    public boolean isHasReacted()      { return hasReacted; }

    // ── Setters ──────────────────────────────────────────────────────
    public void setReplyToContent(String c) { this.replyToContent = c; }
    public void setReactionCount(int n)     { this.reactionCount  = n; }
    public void setHasReacted(boolean b)    { this.hasReacted     = b; }
}
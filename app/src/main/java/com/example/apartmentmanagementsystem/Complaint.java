package com.example.apartmentmanagementsystem;

public class Complaint {
    private String id;
    private String category;
    private String subject;
    private String description;
    private String status;
    private String date;
    private String priority;

    public Complaint(String id, String category, String subject, String description, String status, String date, String priority) {
        this.id = id;
        this.category = category;
        this.subject = subject;
        this.description = description;
        this.status = status;
        this.date = date;
        this.priority = priority;
    }

    public String getId() { return id; }
    public String getCategory() { return category; }
    public String getSubject() { return subject; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public String getDate() { return date; }
    public String getPriority() { return priority; }
}
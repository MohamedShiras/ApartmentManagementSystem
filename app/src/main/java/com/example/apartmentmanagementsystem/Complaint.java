package com.example.apartmentmanagementsystem;

public class Complaint {
    private String id;
    private String category;
    private String subject;
    private String description;
    private String status;
    private String date;

    public Complaint(String id, String category, String subject, String description, String status, String date) {
        this.id = id;
        this.category = category;
        this.subject = subject;
        this.description = description;
        this.status = status;
        this.date = date;
    }

    public String getId() { return id; }
    public String getCategory() { return category; }
    public String getSubject() { return subject; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public String getDate() { return date; }
}

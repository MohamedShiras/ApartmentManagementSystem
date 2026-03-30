package com.example.apartmentmanagementsystem;

public class AdminReservation {
    private final String id;
    private final String serviceName;
    private final String bookedBy;
    private final String description;
    private final String dateTime;
    private final String status;
    private final int imageResId;
    private final String imageUri;

    public AdminReservation(String id, String serviceName, String bookedBy, String description,
                            String dateTime, String status, int imageResId) {
        this(id, serviceName, bookedBy, description, dateTime, status, imageResId, null);
    }

    public AdminReservation(String id, String serviceName, String bookedBy, String description,
                            String dateTime, String status, int imageResId, String imageUri) {
        this.id = id;
        this.serviceName = serviceName;
        this.bookedBy = bookedBy;
        this.description = description;
        this.dateTime = dateTime;
        this.status = status;
        this.imageResId = imageResId;
        this.imageUri = imageUri;
    }

    public String getId() {
        return id;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getBookedBy() {
        return bookedBy;
    }

    public String getDescription() {
        return description;
    }

    public String getDateTime() {
        return dateTime;
    }

    public String getStatus() {
        return status;
    }

    public int getImageResId() {
        return imageResId;
    }

    public String getImageUri() {
        return imageUri;
    }
}

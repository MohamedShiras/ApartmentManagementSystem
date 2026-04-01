package com.example.apartmentmanagementsystem;

public class AdminReservation {

    private final String id;
    private final String serviceName;
    private final String description;
    private final String reservationDate;
    private final String reservationTime;
    private final String duration;
    private final String imageUrl;
    private final String status;
    private final String bookedBy;
    private final String capacity; // Add this line

    public AdminReservation(
            String id,
            String serviceName,
            String description,
            String reservationDate,
            String reservationTime,
            String duration,
            String imageUrl,
            String status,
            String bookedBy,
            String capacity // Add this parameter
    ) {
        this.id = id;
        this.serviceName = serviceName;
        this.description = description;
        this.reservationDate = reservationDate;
        this.reservationTime = reservationTime;
        this.duration = duration;
        this.imageUrl = imageUrl;
        this.status = status;
        this.bookedBy = bookedBy;
        this.capacity = capacity; // Add this assignment
    }

    public String getId() {
        return id;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getDescription() {
        return description;
    }

    public String getReservationDate() {
        return reservationDate;
    }

    public String getReservationTime() {
        return reservationTime;
    }

    public String getDuration() {
        return duration;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getStatus() {
        return status;
    }

    public String getBookedBy() {
        return bookedBy;
    }

    public String getCapacity() {
        return capacity == null ? "" : capacity;
    }
}

package com.example.apartmentmanagementsystem;

public class AdminReservation {

    private final String id;
    private final String serviceName;
    private final String description;
    private final String timePeriod;
    private final String maxGuests;

    public AdminReservation(
            String id,
            String serviceName,
            String description,
            String timePeriod,
            String maxGuests
    ) {
        this.id = id;
        this.serviceName = serviceName;
        this.description = description;
        this.timePeriod = timePeriod;
        this.maxGuests = maxGuests;
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

    public String getTimePeriod() {
        return timePeriod == null ? "" : timePeriod;
    }

    public String getMaxGuests() {
        return maxGuests == null ? "" : maxGuests;
    }
}

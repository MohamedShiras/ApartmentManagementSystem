package com.example.apartmentmanagementsystem.admin.model;

public class UserMaintenanceRecord {

    private final String recordId;
    private final String residentName;
    private final String unit;
    private final String maintenanceType;
    private final String submittedDate;
    private final String issueSummary;
    private final String description;
    private final MaintenancePriority priority;
    private MaintenanceStatus status;

    public UserMaintenanceRecord(
            String recordId,
            String residentName,
            String unit,
            String maintenanceType,
            String submittedDate,
            String issueSummary,
            String description,
            MaintenancePriority priority,
            MaintenanceStatus status
    ) {
        this.recordId = recordId;
        this.residentName = residentName;
        this.unit = unit;
        this.maintenanceType = maintenanceType;
        this.submittedDate = submittedDate;
        this.issueSummary = issueSummary;
        this.description = description;
        this.priority = priority;
        this.status = status;
    }

    public String getRecordId() {
        return recordId;
    }

    public String getResidentName() {
        return residentName;
    }

    public String getUnit() {
        return unit;
    }

    public String getMaintenanceType() {
        return maintenanceType;
    }

    public String getSubmittedDate() {
        return submittedDate;
    }

    public String getIssueSummary() {
        return issueSummary;
    }

    public String getDescription() {
        return description;
    }

    public MaintenancePriority getPriority() {
        return priority;
    }

    public MaintenanceStatus getStatus() {
        return status;
    }

    public void setStatus(MaintenanceStatus status) {
        this.status = status;
    }
}


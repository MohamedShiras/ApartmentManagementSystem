package com.example.apartmentmanagementsystem.admin.data;

import com.example.apartmentmanagementsystem.admin.model.MaintenancePriority;
import com.example.apartmentmanagementsystem.admin.model.MaintenanceStatus;
import com.example.apartmentmanagementsystem.admin.model.UserMaintenanceRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class AdminRecordRepository {

    private static AdminRecordRepository instance;
    private final List<UserMaintenanceRecord> records = new ArrayList<>();

    private AdminRecordRepository() {
        seedRecords();
    }

    public static synchronized AdminRecordRepository getInstance() {
        if (instance == null) {
            instance = new AdminRecordRepository();
        }
        return instance;
    }

    public List<UserMaintenanceRecord> getAllRecords() {
        return new ArrayList<>(records);
    }

    public List<UserMaintenanceRecord> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllRecords();
        }

        String normalized = query.trim().toLowerCase(Locale.US);
        List<UserMaintenanceRecord> filtered = new ArrayList<>();

        for (UserMaintenanceRecord record : records) {
            if (contains(record.getRecordId(), normalized)
                    || contains(record.getResidentName(), normalized)
                    || contains(record.getUnit(), normalized)
                    || contains(record.getMaintenanceType(), normalized)
                    || contains(record.getIssueSummary(), normalized)) {
                filtered.add(record);
            }
        }

        return filtered;
    }

    public UserMaintenanceRecord getById(String recordId) {
        for (UserMaintenanceRecord record : records) {
            if (record.getRecordId().equals(recordId)) {
                return record;
            }
        }
        return null;
    }

    public void updateStatus(String recordId, MaintenanceStatus newStatus) {
        UserMaintenanceRecord record = getById(recordId);
        if (record != null) {
            record.setStatus(newStatus);
        }
    }

    public int getStatusCount(MaintenanceStatus status) {
        int count = 0;
        for (UserMaintenanceRecord record : records) {
            if (record.getStatus() == status) {
                count++;
            }
        }
        return count;
    }

    public int getTotalCount() {
        return records.size();
    }

    private boolean contains(String value, String normalizedQuery) {
        return value != null && value.toLowerCase(Locale.US).contains(normalizedQuery);
    }

    private void seedRecords() {
        if (!records.isEmpty()) {
            return;
        }

        Collections.addAll(
                records,
                new UserMaintenanceRecord(
                        "MR-2026-DB1",
                        "Alex Lawson",
                        "B-402",
                        "Generator",
                        "25 Feb 2026",
                        "Generator Room - Fuel Leak",
                        "Resident reported strong fuel smell and leak near backup generator line.",
                        MaintenancePriority.HIGH,
                        MaintenanceStatus.OVERDUE
                ),
                new UserMaintenanceRecord(
                        "MR-2026-D91",
                        "Raj Sharma",
                        "A-307",
                        "Elevator",
                        "06 Mar 2026",
                        "Lift door sensor issue",
                        "Door reopens unexpectedly on floor 3 and floor 5.",
                        MaintenancePriority.MEDIUM,
                        MaintenanceStatus.PENDING
                ),
                new UserMaintenanceRecord(
                        "MR-2026-E33",
                        "Noah Miller",
                        "C-110",
                        "Electric",
                        "11 Mar 2026",
                        "Corridor light fluctuation",
                        "Light panel in west corridor keeps flickering at peak hours.",
                        MaintenancePriority.MEDIUM,
                        MaintenanceStatus.IN_PROGRESS
                ),
                new UserMaintenanceRecord(
                        "MR-2026-F04",
                        "Emma White",
                        "D-215",
                        "Electric",
                        "13 Mar 2026",
                        "Main breaker trip",
                        "Breaker trips when AC and microwave run together.",
                        MaintenancePriority.HIGH,
                        MaintenanceStatus.PENDING
                ),
                new UserMaintenanceRecord(
                        "MR-2026-F62",
                        "Ibrahim Khan",
                        "E-009",
                        "Elevator",
                        "19 Mar 2026",
                        "Lift panel beep issue",
                        "Control panel gives repeated warning beep though functioning normally.",
                        MaintenancePriority.LOW,
                        MaintenanceStatus.RESOLVED
                )
        );
    }
}


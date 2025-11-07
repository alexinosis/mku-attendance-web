package com.mku.attendance.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AttendanceRecord {
    private String studentId;
    private String unitCode;
    private boolean present;
    private String date;
    private String timestamp;

    // Default constructor for JSON
    public AttendanceRecord() {
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    // Constructor for manual creation
    public AttendanceRecord(String studentId, String unitCode, boolean present, String date) {
        this.studentId = studentId;
        this.unitCode = unitCode;
        this.present = present;
        this.date = date;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    // Constructor with auto date generation
    public AttendanceRecord(String studentId, String unitCode, boolean present) {
        this.studentId = studentId;
        this.unitCode = unitCode;
        this.present = present;
        this.date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    // Getters and Setters
    public String getStudentId() {
        return studentId != null ? studentId : "";
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId != null ? studentId : "";
    }

    public String getUnitCode() {
        return unitCode != null ? unitCode : "";
    }

    public void setUnitCode(String unitCode) {
        this.unitCode = unitCode != null ? unitCode : "";
    }

    public boolean isPresent() {
        return present;
    }

    public void setPresent(boolean present) {
        this.present = present;
    }

    public String getDate() {
        if (date == null || date.isEmpty()) {
            this.date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
        return date;
    }

    public void setDate(String date) {
        this.date = date != null ? date : LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    public String getTimestamp() {
        if (timestamp == null || timestamp.isEmpty()) {
            this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    // Helper methods
    public String getStatus() {
        return present ? "PRESENT" : "ABSENT";
    }

    public String getFormattedTime() {
        if (timestamp != null && timestamp.length() > 11) {
            return timestamp.substring(11, 16); // Extract HH:mm
        }
        return "--:--";
    }

    public String getFormattedDate() {
        if (date != null && date.length() >= 10) {
            return date; // Already in yyyy-MM-dd format
        }
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    // ADD THIS METHOD: Extract time from timestamp
    public String getTime() {
        return getFormattedTime(); // Use the existing method
    }

    @Override
    public String toString() {
        return "AttendanceRecord{" +
                "studentId='" + studentId + '\'' +
                ", unitCode='" + unitCode + '\'' +
                ", present=" + present +
                ", date='" + date + '\'' +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }
}
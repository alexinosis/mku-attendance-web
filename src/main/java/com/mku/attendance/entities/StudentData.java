package com.mku.attendance.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import java.util.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StudentData {
    private String id; // UUID from database
    private String studentId; // Student ID
    private String name; // Full name (matches React frontend)
    private String email;
    private String passwordHash; // Matches database column name
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Legacy fields for backward compatibility
    private String course;
    private Set<String> registeredUnits;
    private Map<String, List<AttendanceRecord>> attendanceRecords;
    private boolean emailVerified = false;

    // Constructor for React frontend compatibility
    public StudentData(String studentId, String name, String email, String passwordHash) {
        this.id = UUID.randomUUID().toString();
        this.studentId = studentId;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        // Initialize legacy fields
        this.course = "";
        this.registeredUnits = new HashSet<>();
        this.attendanceRecords = new HashMap<>();
        this.emailVerified = false;
    }

    // Constructor for database mapping
    public StudentData(String id, String studentId, String name, String email,
                       String passwordHash, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.studentId = studentId;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;

        // Initialize legacy fields
        this.course = "";
        this.registeredUnits = new HashSet<>();
        this.attendanceRecords = new HashMap<>();
        this.emailVerified = false;
    }

    public StudentData() {
        this.id = UUID.randomUUID().toString();
        this.studentId = "";
        this.name = "";
        this.email = "";
        this.passwordHash = "";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.course = "";
        this.registeredUnits = new HashSet<>();
        this.attendanceRecords = new HashMap<>();
        this.emailVerified = false;
    }

    // Getters and Setters for new fields
    public String getId() { return id; }
    public void setId(String id) { this.id = id != null ? id : UUID.randomUUID().toString(); }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId != null ? studentId : ""; }

    public String getName() {
        if (name != null && !name.trim().isEmpty()) {
            return name;
        }
        // Fallback to legacy firstName/lastName for backward compatibility
        return "Unknown Student";
    }
    public void setName(String name) { this.name = name != null ? name : ""; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email != null ? email : ""; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash != null ? passwordHash : ""; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt != null ? createdAt : LocalDateTime.now(); }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt != null ? updatedAt : LocalDateTime.now(); }

    // Legacy getters for backward compatibility
    public String getFirstName() {
        if (name != null && name.contains(" ")) {
            return name.split(" ")[0];
        }
        return name != null ? name : "";
    }

    public String getLastName() {
        if (name != null && name.contains(" ")) {
            String[] parts = name.split(" ");
            return parts.length > 1 ? parts[parts.length - 1] : "";
        }
        return "";
    }

    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course != null ? course : ""; }

    public String getPassword() { return passwordHash; } // Legacy compatibility
    public void setPassword(String password) { this.passwordHash = password != null ? password : ""; } // Legacy compatibility

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public Set<String> getRegisteredUnits() {
        if (registeredUnits == null) {
            registeredUnits = new HashSet<>();
        }
        return registeredUnits;
    }

    public void setRegisteredUnits(Set<String> registeredUnits) {
        this.registeredUnits = registeredUnits != null ? registeredUnits : new HashSet<>();
    }

    public Map<String, List<AttendanceRecord>> getAttendanceRecords() {
        if (attendanceRecords == null) {
            attendanceRecords = new HashMap<>();
        }
        return attendanceRecords;
    }

    public void setAttendanceRecords(Map<String, List<AttendanceRecord>> attendanceRecords) {
        this.attendanceRecords = attendanceRecords != null ? attendanceRecords : new HashMap<>();
    }

    // Business methods - Updated for React frontend compatibility
    public boolean hasValidEmail() {
        return email != null && !email.trim().isEmpty() &&
                email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    // Method to validate student data for React frontend
    public boolean isValidForRegistration() {
        return studentId != null && !studentId.trim().isEmpty() &&
                name != null && !name.trim().isEmpty() &&
                email != null && !email.trim().isEmpty() &&
                passwordHash != null && !passwordHash.trim().isEmpty() &&
                hasValidEmail();
    }

    // Method to update password (for password reset)
    public void updatePassword(String newPassword) {
        this.passwordHash = newPassword;
        this.updatedAt = LocalDateTime.now();
    }

    // Legacy business methods (for backward compatibility)
    public boolean registerCourse(String courseCode) {
        if (courseCode == null || courseCode.trim().isEmpty()) {
            return false;
        }
        if (this.course == null || this.course.isEmpty()) {
            this.course = courseCode.trim().toUpperCase();
            return true;
        }
        return false;
    }

    public boolean registerUnit(String unitCode) {
        if (unitCode == null || unitCode.trim().isEmpty()) {
            return false;
        }
        if (registeredUnits == null) {
            registeredUnits = new HashSet<>();
        }
        if (registeredUnits.size() < 8) {
            String normalizedUnitCode = unitCode.trim().toUpperCase();
            boolean added = registeredUnits.add(normalizedUnitCode);
            System.out.println("Registering unit " + normalizedUnitCode + " for student " + studentId + ": " + (added ? "SUCCESS" : "ALREADY_EXISTS"));
            return added;
        }
        System.out.println("Cannot register unit " + unitCode + " for student " + studentId + ": MAX_UNITS_REACHED");
        return false;
    }

    public boolean removeUnit(String unitCode) {
        if (unitCode == null || registeredUnits == null) {
            return false;
        }
        boolean removed = registeredUnits.remove(unitCode.toUpperCase());
        System.out.println("Removing unit " + unitCode + " for student " + studentId + ": " + (removed ? "SUCCESS" : "NOT_FOUND"));
        return removed;
    }

    public void addAttendanceRecord(String unitCode, AttendanceRecord record) {
        if (unitCode == null || record == null) {
            return;
        }
        if (attendanceRecords == null) {
            attendanceRecords = new HashMap<>();
        }
        String normalizedUnitCode = unitCode.toUpperCase();
        attendanceRecords.computeIfAbsent(normalizedUnitCode, k -> new ArrayList<>()).add(record);
        System.out.println("Added attendance record for student " + studentId + " in unit " + normalizedUnitCode + ": " + record);
    }

    public void markAttendance(String unitCode, boolean present) {
        if (unitCode == null) {
            return;
        }
        AttendanceRecord record = new AttendanceRecord(studentId, unitCode, present);
        addAttendanceRecord(unitCode, record);
    }

    public boolean isRegisteredForUnit(String unitCode) {
        if (unitCode == null || registeredUnits == null) {
            return false;
        }
        return registeredUnits.contains(unitCode.toUpperCase());
    }

    // ========== ATTENDANCE CALCULATION METHODS ==========

    public int getTotalSessions(String unitCode) {
        if (unitCode == null) return 0;
        List<AttendanceRecord> records = getAttendanceRecordsForUnit(unitCode);
        return records.size();
    }

    public int getPresentCount(String unitCode) {
        if (unitCode == null) return 0;
        List<AttendanceRecord> records = getAttendanceRecordsForUnit(unitCode);
        return (int) records.stream()
                .filter(record -> record != null && record.isPresent())
                .count();
    }

    // MISSING METHOD: Added back
    public int getAbsentCount(String unitCode) {
        if (unitCode == null) return 0;
        List<AttendanceRecord> records = getAttendanceRecordsForUnit(unitCode);
        return (int) records.stream()
                .filter(record -> record != null && !record.isPresent())
                .count();
    }

    public double getAttendancePercentage(String unitCode) {
        int total = getTotalSessions(unitCode);
        if (total == 0) return 0.0;
        int present = getPresentCount(unitCode);
        return Math.round((present * 100.0) / total * 10.0) / 10.0;
    }

    // MISSING METHOD: Added back
    public String getTodaysAttendanceStatus(String unitCode) {
        if (unitCode == null) return "NOT_MARKED";

        List<AttendanceRecord> records = getAttendanceRecordsForUnit(unitCode);
        if (records.isEmpty()) return "NOT_MARKED";

        String today = java.time.LocalDate.now().toString();

        for (AttendanceRecord record : records) {
            if (record != null && record.getDate() != null && record.getDate().contains(today)) {
                return record.isPresent() ? "PRESENT" : "ABSENT";
            }
        }

        return "NOT_MARKED";
    }

    // MISSING METHOD: Added back
    public String getLastAttendanceDate(String unitCode) {
        if (unitCode == null) return null;

        List<AttendanceRecord> records = getAttendanceRecordsForUnit(unitCode);
        if (records.isEmpty()) return null;

        return records.stream()
                .filter(record -> record != null && record.getDate() != null)
                .max(Comparator.comparing(AttendanceRecord::getDate))
                .map(AttendanceRecord::getDate)
                .orElse(null);
    }

    // MISSING METHOD: Added back
    public String getLastAttendanceTime(String unitCode) {
        if (unitCode == null) return null;

        List<AttendanceRecord> records = getAttendanceRecordsForUnit(unitCode);
        if (records.isEmpty()) return null;

        return records.stream()
                .filter(record -> record != null && record.getTimestamp() != null)
                .max(Comparator.comparing(AttendanceRecord::getTimestamp))
                .map(AttendanceRecord::getFormattedTime)
                .orElse(null);
    }

    // MISSING METHOD: Added back
    public int getUnitsWithAttendanceCount() {
        if (registeredUnits == null) return 0;
        return (int) registeredUnits.stream()
                .filter(unitCode -> getTotalSessions(unitCode) > 0)
                .count();
    }

    // MISSING METHOD: Added back
    public int getUnitsWithGoodAttendanceCount() {
        if (registeredUnits == null) return 0;
        return (int) registeredUnits.stream()
                .filter(unitCode -> getAttendancePercentage(unitCode) >= 75)
                .count();
    }

    // MISSING METHOD: Added back
    public int getUnitsNeedingImprovementCount() {
        if (registeredUnits == null) return 0;
        return (int) registeredUnits.stream()
                .filter(unitCode -> getAttendancePercentage(unitCode) < 75)
                .count();
    }

    // MISSING METHOD: Added back
    public double getOverallAttendancePercentage() {
        if (registeredUnits == null || registeredUnits.isEmpty()) return 0.0;

        double totalPercentage = 0.0;
        int unitsWithAttendance = 0;

        for (String unitCode : registeredUnits) {
            double unitPercentage = getAttendancePercentage(unitCode);
            if (unitPercentage > 0) {
                totalPercentage += unitPercentage;
                unitsWithAttendance++;
            }
        }

        return unitsWithAttendance > 0 ? Math.round(totalPercentage / unitsWithAttendance * 10.0) / 10.0 : 0.0;
    }

    // MISSING METHOD: Added back
    public boolean hasAnyAttendanceRecords() {
        if (registeredUnits == null) return false;
        return registeredUnits.stream()
                .anyMatch(unitCode -> getTotalSessions(unitCode) > 0);
    }

    // MISSING METHOD: Added back
    public int getTotalSessionsAcrossAllUnits() {
        if (registeredUnits == null) return 0;
        return registeredUnits.stream()
                .mapToInt(this::getTotalSessions)
                .sum();
    }

    // MISSING METHOD: Added back
    public int getTotalPresentSessionsAcrossAllUnits() {
        if (registeredUnits == null) return 0;
        return registeredUnits.stream()
                .mapToInt(this::getPresentCount)
                .sum();
    }

    private List<AttendanceRecord> getAttendanceRecordsForUnit(String unitCode) {
        if (unitCode == null || attendanceRecords == null) {
            return new ArrayList<>();
        }
        List<AttendanceRecord> records = attendanceRecords.get(unitCode.toUpperCase());
        return records != null ? records : new ArrayList<>();
    }

    @Override
    public String toString() {
        return "StudentData{" +
                "id='" + id + '\'' +
                ", studentId='" + studentId + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }

    public String getDebugInfo() {
        return String.format("Student[ID=%s, StudentID=%s, Name=%s, Email=%s, Created=%s]",
                id, studentId, name, email, createdAt);
    }

    // Method to convert to Map for JSON response (React frontend compatibility)
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("student_id", studentId);
        map.put("name", name);
        map.put("email", email);
        map.put("created_at", createdAt.toString());
        map.put("updated_at", updatedAt.toString());
        return map;
    }
}
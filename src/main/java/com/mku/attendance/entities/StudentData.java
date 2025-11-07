package com.mku.attendance.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StudentData {
    private String studentId;
    private String firstName;
    private String lastName;
    private String email;
    private String course; // Changed to track registered course
    private Set<String> registeredUnits; // Track registered units
    private String password;
    private Map<String, List<AttendanceRecord>> attendanceRecords; // Unit code -> attendance records

    public StudentData(String studentId, String firstName, String lastName,
                       String email, String course, String unit, String password) {
        this.studentId = studentId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.course = course;
        this.password = password;
        this.registeredUnits = new HashSet<>();
        this.attendanceRecords = new HashMap<>();

        // Add initial unit if provided
        if (unit != null && !unit.trim().isEmpty()) {
            this.registeredUnits.add(unit.trim().toUpperCase());
        }
    }

    // Default constructor for JSON
    public StudentData() {
        this.studentId = "";
        this.firstName = "";
        this.lastName = "";
        this.email = "";
        this.course = "";
        this.password = "";
        this.registeredUnits = new HashSet<>();
        this.attendanceRecords = new HashMap<>();
    }

    // Getters
    public String getStudentId() { return studentId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getName() {
        if ((firstName == null || firstName.isEmpty()) && (lastName == null || lastName.isEmpty())) {
            return "Unknown Student";
        }
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }
    public String getEmail() { return email; }
    public String getCourse() { return course; }
    public String getPassword() { return password; }
    public Set<String> getRegisteredUnits() {
        if (registeredUnits == null) {
            registeredUnits = new HashSet<>();
        }
        return registeredUnits;
    }
    public Map<String, List<AttendanceRecord>> getAttendanceRecords() {
        if (attendanceRecords == null) {
            attendanceRecords = new HashMap<>();
        }
        return attendanceRecords;
    }

    // Proper Setters for JSON deserialization
    public void setStudentId(String studentId) {
        this.studentId = studentId != null ? studentId : "";
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName != null ? firstName : "";
    }

    public void setLastName(String lastName) {
        this.lastName = lastName != null ? lastName : "";
    }

    public void setEmail(String email) {
        this.email = email != null ? email : "";
    }

    public void setCourse(String course) {
        this.course = course != null ? course : "";
    }

    public void setPassword(String password) {
        this.password = password != null ? password : "";
    }

    public void setRegisteredUnits(Set<String> registeredUnits) {
        this.registeredUnits = registeredUnits != null ? registeredUnits : new HashSet<>();
    }

    public void setAttendanceRecords(Map<String, List<AttendanceRecord>> attendanceRecords) {
        this.attendanceRecords = attendanceRecords != null ? attendanceRecords : new HashMap<>();
    }

    // Business methods
    public boolean registerCourse(String courseCode) {
        if (courseCode == null || courseCode.trim().isEmpty()) {
            return false;
        }
        if (this.course == null || this.course.isEmpty()) {
            this.course = courseCode.trim().toUpperCase();
            return true;
        }
        return false; // Already registered for a course
    }

    public boolean registerUnit(String unitCode) {
        if (unitCode == null || unitCode.trim().isEmpty()) {
            return false;
        }
        if (registeredUnits == null) {
            registeredUnits = new HashSet<>();
        }
        if (registeredUnits.size() < 8) { // Maximum 8 units
            String normalizedUnitCode = unitCode.trim().toUpperCase();
            boolean added = registeredUnits.add(normalizedUnitCode);
            System.out.println("Registering unit " + normalizedUnitCode + " for student " + studentId + ": " + (added ? "SUCCESS" : "ALREADY_EXISTS"));
            return added;
        }
        System.out.println("Cannot register unit " + unitCode + " for student " + studentId + ": MAX_UNITS_REACHED");
        return false; // Maximum units reached
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

    // Add attendance record with auto-creation
    public void markAttendance(String unitCode, boolean present) {
        if (unitCode == null) {
            return;
        }
        AttendanceRecord record = new AttendanceRecord(studentId, unitCode, present);
        addAttendanceRecord(unitCode, record);
    }

    // Check if student is registered for a specific unit
    public boolean isRegisteredForUnit(String unitCode) {
        if (unitCode == null || registeredUnits == null) {
            return false;
        }
        return registeredUnits.contains(unitCode.toUpperCase());
    }

    // Get attendance records for specific unit with null safety
    public List<AttendanceRecord> getAttendanceRecordsForUnit(String unitCode) {
        if (unitCode == null || attendanceRecords == null) {
            return new ArrayList<>();
        }
        List<AttendanceRecord> records = attendanceRecords.get(unitCode.toUpperCase());
        return records != null ? records : new ArrayList<>();
    }

    // Attendance statistics with null safety
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
        return Math.round((present * 100.0) / total * 10.0) / 10.0; // Round to 1 decimal place
    }

    // Get today's attendance status for a unit
    public String getTodaysAttendanceStatus(String unitCode) {
        if (unitCode == null) return "NOT_MARKED";

        List<AttendanceRecord> records = getAttendanceRecordsForUnit(unitCode);
        if (records.isEmpty()) return "NOT_MARKED";

        // Get today's date string (YYYY-MM-DD format)
        String today = java.time.LocalDate.now().toString();

        for (AttendanceRecord record : records) {
            if (record != null && record.getDate() != null && record.getDate().contains(today)) {
                return record.isPresent() ? "PRESENT" : "ABSENT";
            }
        }

        return "NOT_MARKED";
    }

    // Get last attendance date for a unit
    public String getLastAttendanceDate(String unitCode) {
        if (unitCode == null) return null;

        List<AttendanceRecord> records = getAttendanceRecordsForUnit(unitCode);
        if (records.isEmpty()) return null;

        // Sort by date descending and get the latest
        return records.stream()
                .filter(record -> record != null && record.getDate() != null)
                .max(Comparator.comparing(AttendanceRecord::getDate))
                .map(AttendanceRecord::getDate)
                .orElse(null);
    }

    // Get last attendance time for a unit
    public String getLastAttendanceTime(String unitCode) {
        if (unitCode == null) return null;

        List<AttendanceRecord> records = getAttendanceRecordsForUnit(unitCode);
        if (records.isEmpty()) return null;

        // Sort by timestamp descending and get the latest
        return records.stream()
                .filter(record -> record != null && record.getTimestamp() != null)
                .max(Comparator.comparing(AttendanceRecord::getTimestamp))
                .map(AttendanceRecord::getFormattedTime)
                .orElse(null);
    }

    // ========== ADD THESE MISSING HELPER METHODS ==========

    // Helper method for template - count units with attendance records
    public int getUnitsWithAttendanceCount() {
        if (registeredUnits == null) return 0;
        return (int) registeredUnits.stream()
                .filter(unitCode -> getTotalSessions(unitCode) > 0)
                .count();
    }

    // Helper method for template - count units with good attendance (>=75%)
    public int getUnitsWithGoodAttendanceCount() {
        if (registeredUnits == null) return 0;
        return (int) registeredUnits.stream()
                .filter(unitCode -> getAttendancePercentage(unitCode) >= 75)
                .count();
    }

    // Helper method for template - count units needing improvement (<75%)
    public int getUnitsNeedingImprovementCount() {
        if (registeredUnits == null) return 0;
        return (int) registeredUnits.stream()
                .filter(unitCode -> getAttendancePercentage(unitCode) < 75)
                .count();
    }

    // Additional helper method for overall attendance percentage
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

    // Helper method to check if student has any attendance records
    public boolean hasAnyAttendanceRecords() {
        if (registeredUnits == null) return false;
        return registeredUnits.stream()
                .anyMatch(unitCode -> getTotalSessions(unitCode) > 0);
    }

    // Helper method to get total sessions across all units
    public int getTotalSessionsAcrossAllUnits() {
        if (registeredUnits == null) return 0;
        return registeredUnits.stream()
                .mapToInt(this::getTotalSessions)
                .sum();
    }

    // Helper method to get total present sessions across all units
    public int getTotalPresentSessionsAcrossAllUnits() {
        if (registeredUnits == null) return 0;
        return registeredUnits.stream()
                .mapToInt(this::getPresentCount)
                .sum();
    }

    @Override
    public String toString() {
        return "StudentData{" +
                "studentId='" + studentId + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", course='" + course + '\'' +
                ", registeredUnits=" + (registeredUnits != null ? registeredUnits : "null") +
                ", registeredUnitsCount=" + (registeredUnits != null ? registeredUnits.size() : 0) +
                '}';
    }

    // Helper method to get student info for debugging
    public String getDebugInfo() {
        return String.format("Student[ID=%s, Name=%s, Course=%s, Units=%s, UnitsCount=%d]",
                studentId, getName(), course,
                registeredUnits != null ? registeredUnits : "null",
                registeredUnits != null ? registeredUnits.size() : 0);
    }
}
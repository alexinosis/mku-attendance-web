package com.mku.attendance.services;

import com.mku.attendance.entities.StudentData;
import com.mku.attendance.entities.AttendanceRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class StudentManager {
    private Map<String, StudentData> students = new HashMap<>();
    private final FileDataService fileDataService;

    @Autowired
    public StudentManager(FileDataService fileDataService) {
        this.fileDataService = fileDataService;
        System.out.println("✅ StudentManager initialized!");

        // Load students from file
        try {
            students = fileDataService.loadStudents();
            if (students == null) {
                students = new HashMap<>();
                System.out.println("No students data loaded, initializing empty student map");
            } else {
                System.out.println("StudentManager initialized with " + students.size() + " students");
            }
        } catch (Exception e) {
            System.err.println("Error loading students: " + e.getMessage());
            students = new HashMap<>();
        }
    }

    public void addStudent(StudentData student) {
        if (student != null && student.getStudentId() != null) {
            students.put(student.getStudentId().toUpperCase(), student);
            saveStudentsToFile();
            System.out.println("Added student: " + student.getStudentId());
        }
    }

    public StudentData getStudent(String studentId) {
        if (studentId == null) return null;
        StudentData student = students.get(studentId.toUpperCase());
        System.out.println("Student lookup for '" + studentId + "': " + (student != null ? "FOUND" : "NOT FOUND"));
        return student;
    }

    public boolean exists(String studentId) {
        boolean exists = students.containsKey(studentId.toUpperCase());
        System.out.println("Student exists check for '" + studentId + "': " + exists);
        return exists;
    }

    public Map<String, StudentData> getStudents() {
        return new HashMap<>(students);
    }

    // Save students to file
    public void saveStudentsToFile() {
        try {
            fileDataService.saveStudents(students);
            System.out.println("Students data saved successfully");
        } catch (Exception e) {
            System.err.println("Error saving students data: " + e.getMessage());
        }
    }

    // Mark attendance
    public void markAttendance(String studentId, String unitCode, boolean present) {
        if (studentId == null || unitCode == null) return;

        StudentData student = students.get(studentId.toUpperCase());
        if (student != null) {
            Set<String> registeredUnits = student.getRegisteredUnits();
            if (registeredUnits != null && registeredUnits.contains(unitCode.toUpperCase())) {
                AttendanceRecord record = new AttendanceRecord(studentId, unitCode, present);
                student.addAttendanceRecord(unitCode, record);
                saveStudentsToFile();
                System.out.println("Attendance marked for student " + studentId + " in unit " + unitCode + ": " + (present ? "Present" : "Absent"));
            } else {
                System.out.println("Student " + studentId + " is not registered for unit " + unitCode);
            }
        } else {
            System.out.println("Student not found: " + studentId);
        }
    }

    // Get attendance summary for a student
    public Map<String, Map<String, Object>> getAttendanceSummary(String studentId) {
        Map<String, Map<String, Object>> summary = new HashMap<>();
        if (studentId == null) return summary;

        StudentData student = students.get(studentId.toUpperCase());
        if (student != null) {
            Set<String> registeredUnits = student.getRegisteredUnits();
            if (registeredUnits != null) {
                for (String unitCode : registeredUnits) {
                    Map<String, Object> unitSummary = new HashMap<>();
                    unitSummary.put("totalSessions", student.getTotalSessions(unitCode));
                    unitSummary.put("presentCount", student.getPresentCount(unitCode));
                    unitSummary.put("absentCount", student.getAbsentCount(unitCode));
                    unitSummary.put("attendancePercentage", student.getAttendancePercentage(unitCode));
                    summary.put(unitCode, unitSummary);
                }
            }
        }
        return summary;
    }

    // Get overall statistics for dashboard
    public Map<String, Object> getOverallStatistics(String studentId) {
        Map<String, Object> stats = new HashMap<>();
        if (studentId == null) return stats;

        StudentData student = students.get(studentId.toUpperCase());
        if (student != null) {
            int totalSessions = 0;
            int totalPresent = 0;

            Set<String> registeredUnits = student.getRegisteredUnits();
            if (registeredUnits != null) {
                for (String unitCode : registeredUnits) {
                    totalSessions += student.getTotalSessions(unitCode);
                    totalPresent += student.getPresentCount(unitCode);
                }
            }

            double overallPercentage = totalSessions > 0 ? (totalPresent * 100.0) / totalSessions : 0.0;
            stats.put("totalSessions", totalSessions);
            stats.put("totalPresent", totalPresent);
            stats.put("overallPercentage", Math.round(overallPercentage * 100.0) / 100.0);
        }
        return stats;
    }
}
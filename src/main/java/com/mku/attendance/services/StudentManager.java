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
        System.out.println("✅ StudentManager initializing...");

        // Load students from file
        try {
            students = fileDataService.loadStudents();
            if (students == null) {
                students = new HashMap<>();
                System.out.println("ℹ️ No students data loaded, initializing empty student map");
            } else {
                System.out.println("✅ StudentManager initialized with " + students.size() + " students");

                // Debug: Print loaded students
                if (!students.isEmpty()) {
                    System.out.println("=== LOADED STUDENTS ===");
                    students.forEach((id, student) -> {
                        System.out.println("ID: " + id +
                                ", Name: " + student.getName() +
                                ", Course: " + student.getCourse() +
                                ", Units: " + (student.getRegisteredUnits() != null ? student.getRegisteredUnits().size() : 0));
                    });
                    System.out.println("=======================");
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error loading students: " + e.getMessage());
            e.printStackTrace();
            students = new HashMap<>();
        }
    }

    // ========== CRUD OPERATIONS ==========

    public void addStudent(StudentData student) {
        if (student != null && student.getStudentId() != null) {
            String studentId = student.getStudentId().toUpperCase();
            students.put(studentId, student);
            saveStudentsToFile();
            System.out.println("✅ Student added and saved: " + studentId + " - " + student.getName());
        } else {
            System.err.println("❌ Cannot add student: Student or Student ID is null");
        }
    }

    /**
     * UPDATE STUDENT - NEW METHOD FOR REACT FRONTEND
     * Updates an existing student's information
     */
    public boolean updateStudent(StudentData updatedStudent) {
        if (updatedStudent == null || updatedStudent.getStudentId() == null) {
            System.err.println("❌ Cannot update student: Student or Student ID is null");
            return false;
        }

        String studentId = updatedStudent.getStudentId().toUpperCase();
        StudentData existingStudent = students.get(studentId);

        if (existingStudent == null) {
            System.err.println("❌ Cannot update student: Student not found - " + studentId);
            return false;
        }

        try {
            // Update student fields
            existingStudent.setName(updatedStudent.getName());
            existingStudent.setEmail(updatedStudent.getEmail());
            existingStudent.setPasswordHash(updatedStudent.getPasswordHash());
            existingStudent.setCourse(updatedStudent.getCourse());
            existingStudent.setRegisteredUnits(updatedStudent.getRegisteredUnits());
            existingStudent.setAttendanceRecords(updatedStudent.getAttendanceRecords());
            existingStudent.setEmailVerified(updatedStudent.isEmailVerified());

            // Update timestamps
            existingStudent.setUpdatedAt(java.time.LocalDateTime.now());

            // Save to file
            saveStudentsToFile();
            System.out.println("✅ Student updated and saved: " + studentId + " - " + updatedStudent.getName());
            return true;

        } catch (Exception e) {
            System.err.println("❌ Error updating student " + studentId + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * UPDATE STUDENT PASSWORD - NEW METHOD FOR PASSWORD RESET
     */
    public boolean updateStudentPassword(String studentId, String newPassword) {
        if (studentId == null || newPassword == null) {
            System.err.println("❌ Cannot update password: Student ID or new password is null");
            return false;
        }

        StudentData student = students.get(studentId.toUpperCase());
        if (student == null) {
            System.err.println("❌ Cannot update password: Student not found - " + studentId);
            return false;
        }

        try {
            student.updatePassword(newPassword);
            saveStudentsToFile();
            System.out.println("✅ Password updated for student: " + studentId);
            return true;

        } catch (Exception e) {
            System.err.println("❌ Error updating password for student " + studentId + ": " + e.getMessage());
            return false;
        }
    }

    public StudentData getStudent(String studentId) {
        if (studentId == null) {
            System.err.println("❌ Student lookup failed: Student ID is null");
            return null;
        }
        StudentData student = students.get(studentId.toUpperCase());
        System.out.println("🔍 Student lookup for '" + studentId + "': " + (student != null ? "FOUND" : "NOT FOUND"));
        return student;
    }

    public boolean exists(String studentId) {
        if (studentId == null) {
            return false;
        }
        boolean exists = students.containsKey(studentId.toUpperCase());
        System.out.println("🔍 Student exists check for '" + studentId + "': " + exists);
        return exists;
    }

    public Map<String, StudentData> getStudents() {
        return new HashMap<>(students);
    }

    // Save students to file
    public void saveStudentsToFile() {
        try {
            fileDataService.saveStudents(students);
            System.out.println("✅ Students data saved successfully (" + students.size() + " students)");
        } catch (Exception e) {
            System.err.println("❌ Error saving students data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ========== UNIT REGISTRATION METHODS ==========

    public boolean registerUnitForStudent(String studentId, String unitCode) {
        if (studentId == null || unitCode == null) {
            System.err.println("❌ Cannot register unit: Student ID or Unit Code is null");
            return false;
        }

        StudentData student = students.get(studentId.toUpperCase());
        if (student != null) {
            boolean registered = student.registerUnit(unitCode);
            if (registered) {
                saveStudentsToFile();
                System.out.println("✅ Unit registered and saved: " + unitCode + " for student " + studentId);
                return true;
            } else {
                System.err.println("❌ Unit registration failed: " + unitCode + " for student " + studentId);
                return false;
            }
        } else {
            System.err.println("❌ Student not found for unit registration: " + studentId);
            return false;
        }
    }

    public boolean removeUnitFromStudent(String studentId, String unitCode) {
        if (studentId == null || unitCode == null) {
            System.err.println("❌ Cannot remove unit: Student ID or Unit Code is null");
            return false;
        }

        StudentData student = students.get(studentId.toUpperCase());
        if (student != null) {
            boolean removed = student.removeUnit(unitCode);
            if (removed) {
                saveStudentsToFile();
                System.out.println("✅ Unit removed and saved: " + unitCode + " from student " + studentId);
                return true;
            } else {
                System.err.println("❌ Unit removal failed: " + unitCode + " from student " + studentId);
                return false;
            }
        } else {
            System.err.println("❌ Student not found for unit removal: " + studentId);
            return false;
        }
    }

    public boolean updateStudentCourse(String studentId, String courseCode) {
        if (studentId == null || courseCode == null) {
            System.err.println("❌ Cannot update course: Student ID or Course Code is null");
            return false;
        }

        StudentData student = students.get(studentId.toUpperCase());
        if (student != null) {
            boolean updated = student.registerCourse(courseCode);
            if (updated) {
                saveStudentsToFile();
                System.out.println("✅ Course updated and saved: " + courseCode + " for student " + studentId);
                return true;
            } else {
                System.err.println("❌ Course update failed: " + courseCode + " for student " + studentId);
                return false;
            }
        } else {
            System.err.println("❌ Student not found for course update: " + studentId);
            return false;
        }
    }

    // ========== ATTENDANCE METHODS ==========

    public void markAttendance(String studentId, String unitCode, boolean present) {
        if (studentId == null || unitCode == null) {
            System.err.println("❌ Cannot mark attendance: Student ID or Unit Code is null");
            return;
        }

        StudentData student = students.get(studentId.toUpperCase());
        if (student != null) {
            Set<String> registeredUnits = student.getRegisteredUnits();
            if (registeredUnits != null && registeredUnits.contains(unitCode.toUpperCase())) {
                AttendanceRecord record = new AttendanceRecord(studentId, unitCode, present);
                student.addAttendanceRecord(unitCode, record);
                saveStudentsToFile();
                System.out.println("✅ Attendance marked and saved for student " + studentId +
                        " in unit " + unitCode + ": " + (present ? "Present" : "Absent"));
            } else {
                System.err.println("❌ Student " + studentId + " is not registered for unit " + unitCode);
            }
        } else {
            System.err.println("❌ Student not found: " + studentId);
        }
    }

    // Enhanced mark attendance with timestamp
    public void markAttendanceWithTimestamp(String studentId, String unitCode, boolean present, String timestamp) {
        if (studentId == null || unitCode == null || timestamp == null) {
            System.err.println("❌ Cannot mark attendance: Missing required parameters");
            return;
        }

        StudentData student = students.get(studentId.toUpperCase());
        if (student != null) {
            Set<String> registeredUnits = student.getRegisteredUnits();
            if (registeredUnits != null && registeredUnits.contains(unitCode.toUpperCase())) {
                AttendanceRecord record = new AttendanceRecord(studentId, unitCode, present, timestamp);
                student.addAttendanceRecord(unitCode, record);
                saveStudentsToFile();
                System.out.println("✅ Attendance marked with timestamp for student " + studentId +
                        " in unit " + unitCode + ": " + (present ? "Present" : "Absent") + " at " + timestamp);
            } else {
                System.err.println("❌ Student " + studentId + " is not registered for unit " + unitCode);
            }
        } else {
            System.err.println("❌ Student not found: " + studentId);
        }
    }

    // ========== QUERY METHODS ==========

    public List<StudentData> getStudentsByCourse(String courseCode) {
        if (courseCode == null) {
            return new ArrayList<>();
        }

        List<StudentData> courseStudents = new ArrayList<>();
        for (StudentData student : students.values()) {
            if (student != null && courseCode.equalsIgnoreCase(student.getCourse())) {
                courseStudents.add(student);
            }
        }
        System.out.println("📊 Found " + courseStudents.size() + " students in course: " + courseCode);
        return courseStudents;
    }

    public List<StudentData> getStudentsByUnit(String unitCode) {
        if (unitCode == null) {
            return new ArrayList<>();
        }

        List<StudentData> unitStudents = new ArrayList<>();
        for (StudentData student : students.values()) {
            if (student != null && student.getRegisteredUnits() != null &&
                    student.getRegisteredUnits().contains(unitCode.toUpperCase())) {
                unitStudents.add(student);
            }
        }
        System.out.println("📊 Found " + unitStudents.size() + " students registered for unit: " + unitCode);
        return unitStudents;
    }

    public Set<String> getStudentRegisteredUnits(String studentId) {
        if (studentId == null) {
            return new HashSet<>();
        }

        StudentData student = students.get(studentId.toUpperCase());
        if (student != null && student.getRegisteredUnits() != null) {
            return new HashSet<>(student.getRegisteredUnits());
        }
        return new HashSet<>();
    }

    // ========== STATISTICS METHODS ==========

    public Map<String, Map<String, Object>> getAttendanceSummary(String studentId) {
        Map<String, Map<String, Object>> summary = new HashMap<>();
        if (studentId == null) {
            System.err.println("❌ Cannot get attendance summary: Student ID is null");
            return summary;
        }

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
                    unitSummary.put("todaysStatus", student.getTodaysAttendanceStatus(unitCode));
                    unitSummary.put("lastAttendanceDate", student.getLastAttendanceDate(unitCode));
                    unitSummary.put("lastAttendanceTime", student.getLastAttendanceTime(unitCode));
                    summary.put(unitCode, unitSummary);
                }
            }
            System.out.println("📊 Generated attendance summary for student: " + studentId + " (" + summary.size() + " units)");
        } else {
            System.err.println("❌ Student not found for attendance summary: " + studentId);
        }
        return summary;
    }

    public Map<String, Object> getOverallStatistics(String studentId) {
        Map<String, Object> stats = new HashMap<>();
        if (studentId == null) {
            System.err.println("❌ Cannot get overall statistics: Student ID is null");
            return stats;
        }

        StudentData student = students.get(studentId.toUpperCase());
        if (student != null) {
            int totalSessions = 0;
            int totalPresent = 0;
            int unitsWithAttendance = 0;

            Set<String> registeredUnits = student.getRegisteredUnits();
            if (registeredUnits != null) {
                for (String unitCode : registeredUnits) {
                    int unitSessions = student.getTotalSessions(unitCode);
                    if (unitSessions > 0) {
                        totalSessions += unitSessions;
                        totalPresent += student.getPresentCount(unitCode);
                        unitsWithAttendance++;
                    }
                }
            }

            double overallPercentage = totalSessions > 0 ? (totalPresent * 100.0) / totalSessions : 0.0;

            stats.put("totalSessions", totalSessions);
            stats.put("totalPresent", totalPresent);
            stats.put("totalAbsent", totalSessions - totalPresent);
            stats.put("overallPercentage", Math.round(overallPercentage * 100.0) / 100.0);
            stats.put("registeredUnits", registeredUnits != null ? registeredUnits.size() : 0);
            stats.put("unitsWithAttendance", unitsWithAttendance);
            stats.put("unitsWithGoodAttendance", student.getUnitsWithGoodAttendanceCount());
            stats.put("unitsNeedingImprovement", student.getUnitsNeedingImprovementCount());
            stats.put("hasAnyAttendance", student.hasAnyAttendanceRecords());

            System.out.println("📊 Generated overall statistics for student: " + studentId);
        } else {
            System.err.println("❌ Student not found for overall statistics: " + studentId);
        }
        return stats;
    }

    // ========== ADMIN METHODS ==========

    public boolean removeStudent(String studentId) {
        if (studentId == null) {
            System.err.println("❌ Cannot remove student: Student ID is null");
            return false;
        }

        StudentData removedStudent = students.remove(studentId.toUpperCase());
        if (removedStudent != null) {
            saveStudentsToFile();
            System.out.println("✅ Student removed and saved: " + studentId);
            return true;
        } else {
            System.err.println("❌ Student not found for removal: " + studentId);
            return false;
        }
    }

    public int getTotalStudentCount() {
        return students.size();
    }

    public int getStudentsInCourseCount(String courseCode) {
        if (courseCode == null) return 0;
        return (int) students.values().stream()
                .filter(student -> student != null && courseCode.equalsIgnoreCase(student.getCourse()))
                .count();
    }

    public void forceSave() {
        saveStudentsToFile();
        System.out.println("💾 Manual force save completed for students data");
    }

    // ========== VALIDATION METHODS ==========

    public boolean validateStudentCredentials(String studentId, String password) {
        if (studentId == null || password == null) {
            return false;
        }

        StudentData student = students.get(studentId.toUpperCase());
        if (student != null) {
            boolean valid = password.equals(student.getPassword());
            System.out.println("🔐 Student credentials validation for '" + studentId + "': " + (valid ? "VALID" : "INVALID"));
            return valid;
        }
        System.out.println("🔐 Student credentials validation for '" + studentId + "': NOT FOUND");
        return false;
    }

    public boolean isStudentRegisteredForUnit(String studentId, String unitCode) {
        if (studentId == null || unitCode == null) {
            return false;
        }

        StudentData student = students.get(studentId.toUpperCase());
        if (student != null && student.getRegisteredUnits() != null) {
            boolean registered = student.isRegisteredForUnit(unitCode);
            System.out.println("🔍 Student " + studentId + " registered for unit " + unitCode + ": " + registered);
            return registered;
        }
        return false;
    }
}
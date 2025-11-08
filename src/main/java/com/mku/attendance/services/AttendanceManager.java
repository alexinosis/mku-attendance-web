package com.mku.attendance.services;

import com.mku.attendance.entities.Attendance;
import com.mku.attendance.entities.StudentData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class AttendanceManager {
    private List<Attendance> attendanceRecords;
    private Map<String, LectureSession> activeLectures;
    private Map<String, Set<String>> pendingAutoMark;
    private DateTimeFormatter dateFormatter;

    @Autowired
    private StudentManager studentManager;

    @Autowired
    private FileDataService fileDataService; // ADDED: File persistence

    public AttendanceManager(FileDataService fileDataService) {
        this.fileDataService = fileDataService;
        this.attendanceRecords = new ArrayList<>();
        this.activeLectures = new ConcurrentHashMap<>();
        this.pendingAutoMark = new ConcurrentHashMap<>();
        this.dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // Load attendance records from file on startup
        try {
            attendanceRecords = fileDataService.loadAttendance();
            if (attendanceRecords == null) {
                attendanceRecords = new ArrayList<>();
                System.out.println("No attendance data loaded, initializing empty attendance records");
            } else {
                System.out.println("✅ AttendanceManager initialized with " + attendanceRecords.size() + " attendance records");
            }
        } catch (Exception e) {
            System.err.println("❌ Error loading attendance records: " + e.getMessage());
            attendanceRecords = new ArrayList<>();
        }

        System.out.println("✅ AttendanceManager initialized with file persistence");
    }

    // Inner class to track lecture sessions
    private static class LectureSession {
        private String unitCode;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private boolean active;
        private Set<String> markedStudents;

        public LectureSession(String unitCode, LocalDateTime startTime, int durationMinutes) {
            this.unitCode = unitCode;
            this.startTime = startTime;
            this.endTime = startTime.plusMinutes(durationMinutes);
            this.active = true;
            this.markedStudents = new HashSet<>();
        }

        public boolean isActive() {
            return active && LocalDateTime.now().isBefore(endTime);
        }

        public void endLecture() {
            this.active = false;
        }

        public void markStudent(String studentId) {
            markedStudents.add(studentId);
        }

        public Set<String> getMarkedStudents() {
            return new HashSet<>(markedStudents);
        }

        public boolean isLectureTime() {
            LocalDateTime now = LocalDateTime.now();
            return !now.isBefore(startTime) && !now.isAfter(endTime);
        }

        public String getRemainingTime() {
            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(endTime)) {
                return "00:00";
            }
            long secondsLeft = java.time.Duration.between(now, endTime).getSeconds();
            long minutes = secondsLeft / 60;
            long seconds = secondsLeft % 60;
            return String.format("%02d:%02d", minutes, seconds);
        }

        // Helper method to check if session exists and is valid
        public boolean isValidSession() {
            return active && LocalDateTime.now().isBefore(endTime.plusMinutes(5)); // 5-minute grace period
        }
    }

    // ========== LECTURER CONTROLLER METHODS ==========

    /**
     * Check if attendance is active for a unit
     */
    public boolean isAttendanceActive(String unitCode) {
        if (unitCode == null || unitCode.trim().isEmpty()) {
            return false;
        }

        LectureSession session = activeLectures.get(unitCode);
        if (session == null) {
            return false;
        }

        // Check if session is still valid
        if (!session.isValidSession()) {
            // Auto-cleanup expired session
            activeLectures.remove(unitCode);
            return false;
        }

        return session.isActive() && session.isLectureTime();
    }

    /**
     * Start a new lecture session
     */
    public boolean startLecture(String unitCode, int durationMinutes) {
        if (activeLectures.containsKey(unitCode)) {
            LectureSession existingSession = activeLectures.get(unitCode);
            if (existingSession.isValidSession()) {
                return false; // Lecture already active and valid
            } else {
                // Clean up expired session
                activeLectures.remove(unitCode);
            }
        }

        LectureSession session = new LectureSession(unitCode, LocalDateTime.now(), durationMinutes);
        activeLectures.put(unitCode, session);
        System.out.println("✅ Lecture started for unit: " + unitCode + " for " + durationMinutes + " minutes");
        return true;
    }

    /**
     * End a lecture and auto-mark absent students
     */
    public void endLecture(String unitCode) {
        LectureSession session = activeLectures.get(unitCode);
        if (session != null) {
            session.endLecture();
            autoMarkAbsentStudents(unitCode, session.getMarkedStudents());
            activeLectures.remove(unitCode);
            System.out.println("✅ Lecture ended for unit: " + unitCode + ". Auto-marked absent students.");
        }
    }

    /**
     * Record attendance with status and date
     */
    public void recordAttendance(String studentId, String unitCode, String status, String date) {
        boolean present = "PRESENT".equalsIgnoreCase(status);

        // Remove any existing record for the same day
        String datePart = date.contains(" ") ? date.split(" ")[0] : date;
        attendanceRecords.removeIf(a -> a.getStudentId().equals(studentId) &&
                a.getUnitCode().equals(unitCode) &&
                a.getDate().startsWith(datePart));

        Attendance attendance = new Attendance(studentId, unitCode, date, present);
        attendanceRecords.add(attendance);

        // FIXED: Save to file
        saveAttendanceToFile();
    }

    /**
     * Get attendance records for a unit (with date filter)
     */
    public List<Map<String, Object>> getAttendanceRecords(String unitCode, String dateFilter) {
        List<Attendance> records;

        if (dateFilter != null && !dateFilter.isEmpty()) {
            records = getAttendanceRecordsForUnitAndDate(unitCode, dateFilter);
        } else {
            records = getAttendanceRecordsForUnit(unitCode);
        }

        return convertAttendanceToMap(records);
    }

    /**
     * Get attendance records for a unit (without date filter)
     */
    public List<Map<String, Object>> getAttendanceRecords(String unitCode) {
        List<Attendance> records = getAttendanceRecordsForUnit(unitCode);
        return convertAttendanceToMap(records);
    }

    /**
     * Mark student present (simplified version)
     */
    public boolean markStudentPresent(String studentId, String unitCode) {
        Map<String, Object> result = markAttendance(studentId, unitCode);
        return (Boolean) result.get("success");
    }

    // ========== CORE ATTENDANCE METHODS ==========

    /**
     * Mark student attendance (main method) - UPDATED with save
     */
    public Map<String, Object> markAttendance(String studentId, String unitCode) {
        Map<String, Object> result = new HashMap<>();

        // Validate inputs
        if (studentId == null || unitCode == null) {
            result.put("success", false);
            result.put("message", "Invalid student or unit information.");
            return result;
        }

        LectureSession session = activeLectures.get(unitCode);
        if (session == null || !session.isValidSession()) {
            result.put("success", false);
            result.put("message", "No active lecture for this unit.");
            return result;
        }

        if (!session.isActive()) {
            result.put("success", false);
            result.put("message", "Lecture has ended. Attendance marking closed.");
            return result;
        }

        if (!session.isLectureTime()) {
            result.put("success", false);
            result.put("message", "Lecture time has expired. Cannot mark attendance.");
            return result;
        }

        // Check if student is registered for this unit
        StudentData student = studentManager.getStudent(studentId);
        if (student == null) {
            result.put("success", false);
            result.put("message", "Student not found.");
            return result;
        }

        if (student.getRegisteredUnits() == null || !student.getRegisteredUnits().contains(unitCode)) {
            result.put("success", false);
            result.put("message", "You are not registered for this unit.");
            return result;
        }

        // Check if already marked
        String currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        boolean alreadyMarked = attendanceRecords.stream()
                .anyMatch(a -> a.getStudentId().equals(studentId) &&
                        a.getUnitCode().equals(unitCode) &&
                        a.getDate().startsWith(currentDate) &&
                        a.isPresent());

        if (alreadyMarked) {
            result.put("success", false);
            result.put("message", "Attendance already marked for today.");
            return result;
        }

        // Mark student in the session
        session.markStudent(studentId);

        // Create attendance record
        String timestamp = LocalDateTime.now().format(dateFormatter);
        Attendance attendance = new Attendance(studentId, unitCode, timestamp, true);
        attendanceRecords.add(attendance);

        // FIXED: Save attendance records to file
        saveAttendanceToFile();

        System.out.println("✅ Attendance marked and saved for student: " + studentId + " in unit: " + unitCode);

        result.put("success", true);
        result.put("message", "Attendance marked successfully!");
        result.put("remainingTime", session.getRemainingTime());
        return result;
    }

    /**
     * Manual attendance marking for lecturers - UPDATED with save
     */
    public boolean manuallyMarkAttendance(String studentId, String unitCode, boolean present, String date) {
        if (studentId == null || unitCode == null || date == null) {
            return false;
        }

        try {
            // Remove any existing record for the same day
            String datePart = date.contains(" ") ? date.split(" ")[0] : date;
            attendanceRecords.removeIf(a -> a.getStudentId().equals(studentId) &&
                    a.getUnitCode().equals(unitCode) &&
                    a.getDate().startsWith(datePart));

            Attendance attendance = new Attendance(studentId, unitCode, date, present);
            attendanceRecords.add(attendance);

            // FIXED: Save attendance records to file
            saveAttendanceToFile();

            System.out.println("✅ Manual attendance marked and saved: " + studentId + " for unit " + unitCode + " - " + (present ? "PRESENT" : "ABSENT"));
            return true;
        } catch (Exception e) {
            System.err.println("❌ Error in manual attendance marking: " + e.getMessage());
            return false;
        }
    }

    // ========== DATA RETRIEVAL METHODS ==========

    /**
     * Get today's attendance for a unit
     */
    public List<Attendance> getTodaysAttendanceForUnit(String unitCode) {
        if (unitCode == null) return new ArrayList<>();

        String currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return attendanceRecords.stream()
                .filter(a -> a.getUnitCode().equals(unitCode) && a.getDate().startsWith(currentDate))
                .collect(Collectors.toList());
    }

    /**
     * Get attendance records for a unit and specific date
     */
    public List<Attendance> getAttendanceRecordsForUnitAndDate(String unitCode, String date) {
        if (unitCode == null || date == null) return new ArrayList<>();

        return attendanceRecords.stream()
                .filter(a -> a.getUnitCode().equals(unitCode) && a.getDate().startsWith(date))
                .collect(Collectors.toList());
    }

    /**
     * Get attendance records for a unit
     */
    public List<Attendance> getAttendanceRecordsForUnit(String unitCode) {
        if (unitCode == null) return new ArrayList<>();

        return attendanceRecords.stream()
                .filter(a -> a.getUnitCode().equals(unitCode))
                .sorted((a1, a2) -> a2.getDate().compareTo(a1.getDate())) // Most recent first
                .collect(Collectors.toList());
    }

    /**
     * Get all attendance records for a student
     */
    public List<Attendance> getAttendanceRecordsForStudent(String studentId) {
        if (studentId == null) return new ArrayList<>();

        return attendanceRecords.stream()
                .filter(a -> a.getStudentId().equals(studentId))
                .sorted((a1, a2) -> a2.getDate().compareTo(a1.getDate())) // Most recent first
                .collect(Collectors.toList());
    }

    /**
     * Get today's attendance for a student
     */
    public List<Attendance> getTodaysAttendanceForStudent(String studentId) {
        if (studentId == null) return new ArrayList<>();

        String currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return attendanceRecords.stream()
                .filter(a -> a.getStudentId().equals(studentId) && a.getDate().startsWith(currentDate))
                .collect(Collectors.toList());
    }

    /**
     * Get all attendance records
     */
    public List<Attendance> getAttendanceRecords() {
        return new ArrayList<>(attendanceRecords);
    }

    // ========== STATISTICS AND REPORTING METHODS ==========

    /**
     * Get attendance statistics for a unit
     */
    public Map<String, Object> getAttendanceStatistics(String unitCode) {
        Map<String, Object> stats = new HashMap<>();

        if (unitCode == null) {
            stats.put("totalRecords", 0);
            stats.put("presentCount", 0);
            stats.put("absentCount", 0);
            stats.put("attendanceRate", 0.0); // FIXED: Now Double instead of String
            return stats;
        }

        try {
            List<Attendance> unitRecords = getAttendanceRecordsForUnit(unitCode);

            long totalRecords = unitRecords.size();
            long presentCount = unitRecords.stream().filter(Attendance::isPresent).count();
            long absentCount = totalRecords - presentCount;
            double attendanceRate = totalRecords > 0 ? (double) presentCount / totalRecords * 100 : 0;

            stats.put("totalRecords", totalRecords);
            stats.put("presentCount", presentCount);
            stats.put("absentCount", absentCount);
            stats.put("attendanceRate", Math.round(attendanceRate * 10.0) / 10.0); // FIXED: Now Double
        } catch (Exception e) {
            System.err.println("Error getting attendance statistics: " + e.getMessage());
            stats.put("totalRecords", 0);
            stats.put("presentCount", 0);
            stats.put("absentCount", 0);
            stats.put("attendanceRate", 0.0); // FIXED: Now Double
        }

        return stats;
    }

    /**
     * Get dashboard summary for lecturer
     */
    public Map<String, Object> getDashboardSummary(String unitCode) {
        Map<String, Object> summary = new HashMap<>();

        List<Attendance> unitRecords = getAttendanceRecordsForUnit(unitCode);
        List<Attendance> todayRecords = getTodaysAttendanceForUnit(unitCode);

        // Count students registered for this unit
        long totalStudents = studentManager.getStudents().values().stream()
                .filter(student -> student.getRegisteredUnits() != null &&
                        student.getRegisteredUnits().contains(unitCode))
                .count();

        long presentToday = todayRecords.stream().filter(Attendance::isPresent).count();
        long totalPresent = unitRecords.stream().filter(Attendance::isPresent).count();

        double todayRate = totalStudents > 0 ? (presentToday * 100.0) / totalStudents : 0;
        double overallRate = unitRecords.size() > 0 ? (totalPresent * 100.0) / unitRecords.size() : 0;

        summary.put("totalStudents", totalStudents);
        summary.put("presentToday", presentToday);
        summary.put("todayRate", Math.round(todayRate * 10.0) / 10.0); // FIXED: Now Double
        summary.put("overallRate", Math.round(overallRate * 10.0) / 10.0); // FIXED: Now Double
        summary.put("totalRecords", unitRecords.size());
        summary.put("isActive", isAttendanceActive(unitCode));

        return summary;
    }

    /**
     * Get student attendance history
     */
    public List<Map<String, Object>> getStudentAttendanceHistory(String studentId, String unitCode) {
        return attendanceRecords.stream()
                .filter(a -> a.getStudentId().equals(studentId) &&
                        (unitCode == null || a.getUnitCode().equals(unitCode)))
                .sorted((a1, a2) -> a2.getDate().compareTo(a1.getDate()))
                .map(this::convertAttendanceToSingleMap)
                .collect(Collectors.toList());
    }

    // ========== HELPER METHODS ==========

    /**
     * Convert Attendance objects to Map for Thymeleaf
     */
    private List<Map<String, Object>> convertAttendanceToMap(List<Attendance> attendanceList) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (Attendance attendance : attendanceList) {
            Map<String, Object> record = new HashMap<>();
            record.put("studentId", attendance.getStudentId());
            record.put("unitCode", attendance.getUnitCode());
            record.put("date", attendance.getDate());
            record.put("status", attendance.isPresent() ? "PRESENT" : "ABSENT");
            record.put("present", attendance.isPresent());

            // Add student name if available
            StudentData student = studentManager.getStudent(attendance.getStudentId());
            if (student != null) {
                record.put("studentName", student.getFirstName() + " " + student.getLastName());
            } else {
                record.put("studentName", "Unknown Student");
            }

            result.add(record);
        }

        return result;
    }

    /**
     * Convert single attendance record to map
     */
    private Map<String, Object> convertAttendanceToSingleMap(Attendance attendance) {
        Map<String, Object> record = new HashMap<>();
        record.put("studentId", attendance.getStudentId());
        record.put("unitCode", attendance.getUnitCode());
        record.put("date", attendance.getDate());
        record.put("status", attendance.isPresent() ? "PRESENT" : "ABSENT");

        StudentData student = studentManager.getStudent(attendance.getStudentId());
        if (student != null) {
            record.put("studentName", student.getFirstName() + " " + student.getLastName());
        }

        return record;
    }

    /**
     * Auto-mark absent students when lecture ends - UPDATED with save
     */
    private void autoMarkAbsentStudents(String unitCode, Set<String> presentStudents) {
        String currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String timestamp = LocalDateTime.now().format(dateFormatter);

        // Get all students registered for this unit from StudentManager
        List<StudentData> allStudents = studentManager.getStudents().values().stream()
                .filter(student -> student != null &&
                        student.getRegisteredUnits() != null &&
                        student.getRegisteredUnits().contains(unitCode))
                .collect(Collectors.toList());

        System.out.println("Auto-marking absent students for unit: " + unitCode);
        System.out.println("Present students: " + presentStudents.size());
        System.out.println("Total registered students: " + allStudents.size());

        // Mark absent students who didn't attend
        for (StudentData student : allStudents) {
            String studentId = student.getStudentId();
            if (!presentStudents.contains(studentId)) {
                // Check if not already marked for today
                boolean alreadyMarkedToday = attendanceRecords.stream()
                        .anyMatch(a -> a.getStudentId().equals(studentId) &&
                                a.getUnitCode().equals(unitCode) &&
                                a.getDate().startsWith(currentDate));

                if (!alreadyMarkedToday) {
                    Attendance absentRecord = new Attendance(studentId, unitCode, timestamp, false);
                    attendanceRecords.add(absentRecord);
                    System.out.println("Auto-marked absent: " + studentId + " for unit: " + unitCode);
                }
            }
        }

        // FIXED: Save attendance records to file after auto-marking
        saveAttendanceToFile();

        // Store for any future processing
        pendingAutoMark.put(unitCode, presentStudents);
    }

    /**
     * Get attendance status for student display
     */
    public Map<String, Object> getAttendanceStatusForStudent(String studentId, String unitCode) {
        Map<String, Object> status = new HashMap<>();

        // Initialize with default values
        status.put("active", false);
        status.put("canMark", false);
        status.put("alreadyMarked", false);
        status.put("message", "Attendance marking not available");
        status.put("remainingTime", "00:00");

        if (studentId == null || unitCode == null) {
            status.put("message", "Invalid student or unit information");
            return status;
        }

        try {
            boolean isActive = isAttendanceActive(unitCode);
            status.put("active", isActive);
            status.put("canMark", isActive);

            // Check if already marked today
            String currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            boolean alreadyMarked = attendanceRecords.stream()
                    .anyMatch(a -> a.getStudentId().equals(studentId) &&
                            a.getUnitCode().equals(unitCode) &&
                            a.getDate().startsWith(currentDate) &&
                            a.isPresent());

            status.put("alreadyMarked", alreadyMarked);

            if (alreadyMarked) {
                status.put("message", "Attendance already marked for today");
                status.put("canMark", false);
            } else if (isActive) {
                status.put("message", "Attendance marking is active - Click to mark");
                status.put("canMark", true);

                // Add remaining time
                LectureSession session = activeLectures.get(unitCode);
                if (session != null) {
                    status.put("remainingTime", session.getRemainingTime());
                }
            } else {
                status.put("message", "Attendance marking not active");
            }

        } catch (Exception e) {
            System.err.println("Error getting attendance status for student " + studentId + ": " + e.getMessage());
            status.put("message", "Status unavailable - please try again");
        }

        return status;
    }

    /**
     * Get active lecture session
     */
    public LectureSession getActiveLecture(String unitCode) {
        return activeLectures.get(unitCode);
    }

    /**
     * Get all active lectures
     */
    public Map<String, LectureSession> getActiveLectures() {
        // Clean up any expired lectures first
        cleanupExpiredLectures();
        return new HashMap<>(activeLectures);
    }

    /**
     * Get active unit codes for a student
     */
    public List<String> getActiveUnitsForStudent(String studentId) {
        if (studentId == null) return new ArrayList<>();

        return activeLectures.keySet().stream()
                .filter(unitCode -> {
                    try {
                        StudentData student = studentManager.getStudent(studentId);
                        return student != null &&
                                student.getRegisteredUnits() != null &&
                                student.getRegisteredUnits().contains(unitCode) &&
                                isAttendanceActive(unitCode);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * Check if student can mark attendance for any unit
     */
    public boolean canStudentMarkAttendance(String studentId) {
        if (studentId == null) return false;

        StudentData student = studentManager.getStudent(studentId);
        if (student == null || student.getRegisteredUnits() == null) return false;

        return student.getRegisteredUnits().stream()
                .anyMatch(unitCode -> isAttendanceActive(unitCode));
    }

    /**
     * Clean up expired lectures (safety method)
     */
    public void cleanupExpiredLectures() {
        Iterator<Map.Entry<String, LectureSession>> iterator = activeLectures.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, LectureSession> entry = iterator.next();
            LectureSession session = entry.getValue();
            if (!session.isValidSession()) {
                System.out.println("Cleaning up expired lecture for unit: " + entry.getKey());
                autoMarkAbsentStudents(entry.getKey(), session.getMarkedStudents());
                iterator.remove();
            }
        }
    }

    // ========== ADDED PERSISTENCE METHODS ==========

    /**
     * Save attendance records to file
     */
    public void saveAttendanceToFile() {
        try {
            fileDataService.saveAttendance(attendanceRecords);
            System.out.println("✅ Attendance data saved successfully (" + attendanceRecords.size() + " records)");
        } catch (Exception e) {
            System.err.println("❌ Error saving attendance data: " + e.getMessage());
        }
    }

    /**
     * Manual save method that can be called from controllers
     */
    public void saveAllAttendanceData() {
        saveAttendanceToFile();
    }

    /**
     * Get data directory info for debugging
     */
    public String getDataDirectoryInfo() {
        return fileDataService.getDataDirectory();
    }
}
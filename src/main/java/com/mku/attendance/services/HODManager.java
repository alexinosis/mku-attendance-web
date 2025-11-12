package com.mku.attendance.services;

import com.mku.attendance.entities.HOD;
import com.mku.attendance.entities.LecturerData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class HODManager {
    private final CourseManager courseManager;
    private final UnitManager unitManager;
    private final AttendanceManager attendanceManager;
    private final FileDataService fileDataService;
    private Map<String, HOD> hods = new HashMap<>();

    // FIXED: Lecturers now persisted through FileDataService
    private Map<String, LecturerData> lecturers = new HashMap<>();

    @Autowired
    public HODManager(CourseManager courseManager, UnitManager unitManager,
                      AttendanceManager attendanceManager, FileDataService fileDataService) {
        this.courseManager = courseManager;
        this.unitManager = unitManager;
        this.attendanceManager = attendanceManager;
        this.fileDataService = fileDataService;

        // Load HODs data from file on startup
        hods = fileDataService.loadHODs();

        // Load Lecturers data from file on startup
        lecturers = fileDataService.loadLecturers();

        // If no HOD data exists in file, create default HOD
        if (hods.isEmpty()) {
            System.out.println("No HOD data found. Creating default HOD...");
            HOD defaultHod = new HOD("HOD001", "hod", "Dr. Smith");
            defaultHod.addCourse("CS101");
            hods.put("HOD001", defaultHod);
            saveHODsToFile();
            System.out.println("✅ Default HOD created and saved: HOD001");
        } else {
            System.out.println("✅ HOD data loaded from file: " + hods.size() + " HODs");
        }

        System.out.println("✅ Lecturer data loaded from file: " + lecturers.size() + " lecturers");
        System.out.println("✅ HODManager initialized with full persistence");
    }

    public HOD getHOD(String id) {
        return hods.get(id);
    }

    public void addHOD(HOD hod) {
        hods.put(hod.getId(), hod);
        saveHODsToFile();
        System.out.println("✅ HOD added and saved to file: " + hod.getId());
    }

    // NEW: Update HOD method
    public boolean updateHOD(String hodId, String name, String email, String department, String password) {
        HOD hod = hods.get(hodId);
        if (hod == null) {
            System.out.println("❌ HOD not found for update: " + hodId);
            return false;
        }

        // Update fields (only update password if provided)
        if (name != null && !name.trim().isEmpty()) {
            hod.setName(name.trim());
        }
        if (email != null) {
            hod.setEmail(email.trim());
        }
        if (department != null) {
            hod.setDepartment(department.trim());
        }
        if (password != null && !password.trim().isEmpty()) {
            hod.setPassword(password.trim());
        }

        saveHODsToFile();
        System.out.println("✅ HOD updated successfully: " + hodId);
        System.out.println("   Name: " + hod.getName());
        System.out.println("   Email: " + hod.getEmail());
        System.out.println("   Department: " + hod.getDepartment());
        return true;
    }

    public Map<String, HOD> getHODs() {
        return new HashMap<>(hods);
    }

    public Map<String, HOD> getHodDatabase() {
        return new HashMap<>(hods);
    }

    public CourseManager getCourseManager() { return courseManager; }
    public UnitManager getUnitManager() { return unitManager; }
    public AttendanceManager getAttendanceManager() { return attendanceManager; }

    // Method to manually save HODs data
    public void saveHODsToFile() {
        fileDataService.saveHODs(hods);
    }

    // ========== LECTURER MANAGEMENT METHODS WITH PERSISTENCE ==========

    public Map<String, LecturerData> getLecturers() {
        System.out.println("HODManager: Returning " + lecturers.size() + " lecturers");
        return new HashMap<>(lecturers);
    }

    public void addLecturer(LecturerData lecturer) {
        if (lecturer != null && lecturer.getLecturerId() != null) {
            String lecturerId = lecturer.getLecturerId().toUpperCase();
            lecturers.put(lecturerId, lecturer);
            saveLecturersToFile(); // FIXED: Now saves to file
            System.out.println("✅ Lecturer added and saved: " + lecturerId);
            System.out.println("   Name: " + lecturer.getName());
            System.out.println("   Unit: " + lecturer.getUnitCode());
            System.out.println("   Course: " + lecturer.getCourseCode());
            System.out.println("   Total lecturers now: " + lecturers.size());
        }
    }

    public LecturerData getLecturer(String lecturerId) {
        if (lecturerId == null) return null;
        LecturerData lecturer = lecturers.get(lecturerId.toUpperCase());
        System.out.println("HODManager: Looking up lecturer '" + lecturerId + "' -> " +
                (lecturer != null ? "FOUND" : "NOT FOUND"));
        return lecturer;
    }

    public boolean lecturerExists(String lecturerId) {
        if (lecturerId == null) return false;
        boolean exists = lecturers.containsKey(lecturerId.toUpperCase());
        System.out.println("HODManager: Checking if lecturer '" + lecturerId + "' exists -> " + exists);
        return exists;
    }

    public void removeLecturer(String lecturerId) {
        if (lecturerId != null) {
            lecturers.remove(lecturerId.toUpperCase());
            saveLecturersToFile(); // FIXED: Now saves to file
            System.out.println("✅ Lecturer removed and saved: " + lecturerId);
        }
    }

    public int getLecturerCount() {
        return lecturers.size();
    }

    public String getAvailableLecturerIds() {
        return String.join(", ", lecturers.keySet());
    }

    // FIXED: Save lecturers to file
    public void saveLecturersToFile() {
        try {
            fileDataService.saveLecturers(lecturers);
            System.out.println("✅ Lecturers data saved successfully");
        } catch (Exception e) {
            System.err.println("❌ Error saving lecturers data: " + e.getMessage());
        }
    }

    // Comprehensive save all HOD data
    public void saveAllHODData() {
        saveHODsToFile();
        saveLecturersToFile();
        System.out.println("✅ All HOD data saved successfully");
    }
}
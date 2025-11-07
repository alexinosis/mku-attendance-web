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
    private static Map<String, HOD> hods = new HashMap<>();

    // ADDED: Lecturer database in HODManager
    private final Map<String, LecturerData> lecturers = new HashMap<>();

    @Autowired
    public HODManager(CourseManager courseManager, UnitManager unitManager,
                      AttendanceManager attendanceManager, FileDataService fileDataService) {
        this.courseManager = courseManager;
        this.unitManager = unitManager;
        this.attendanceManager = attendanceManager;
        this.fileDataService = fileDataService;

        // Load HODs data from file on startup
        hods = fileDataService.loadHODs();

        // If no data exists in file, create default HOD
        if (hods.isEmpty()) {
            System.out.println("No HOD data found. Creating default HOD...");
            HOD defaultHod = new HOD("HOD001", "hod", "Dr. Smith");
            defaultHod.addCourse("CS101");
            hods.put("HOD001", defaultHod);
            // Save the default data to file
            fileDataService.saveHODs(hods);
            System.out.println("Default HOD created and saved: HOD001");
        } else {
            System.out.println("HOD data loaded from file: " + hods.size() + " HODs");
        }

        System.out.println("HODManager initialized");
    }

    public HOD getHOD(String id) {
        return hods.get(id);
    }

    public void addHOD(HOD hod) {
        hods.put(hod.getId(), hod);
        // Save to file immediately when adding new HOD
        fileDataService.saveHODs(hods);
        System.out.println("HOD added and saved to file: " + hod.getId());
    }

    public Map<String, HOD> getHODs() {
        return hods;
    }

    public static Map<String, HOD> getHodDatabase() {
        return hods;
    }

    public CourseManager getCourseManager() { return courseManager; }
    public UnitManager getUnitManager() { return unitManager; }
    public AttendanceManager getAttendanceManager() { return attendanceManager; }

    // Method to manually save HODs data (can be called from controllers if needed)
    public void saveHODsToFile() {
        fileDataService.saveHODs(hods);
    }

    // ========== LECTURER MANAGEMENT METHODS ==========

    public Map<String, LecturerData> getLecturers() {
        System.out.println("HODManager: Returning " + lecturers.size() + " lecturers");
        return new HashMap<>(lecturers);
    }

    public void addLecturer(LecturerData lecturer) {
        if (lecturer != null && lecturer.getLecturerId() != null) {
            String lecturerId = lecturer.getLecturerId().toUpperCase();
            lecturers.put(lecturerId, lecturer);
            System.out.println("✅ Lecturer added to HODManager: " + lecturerId);
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
            System.out.println("Lecturer removed: " + lecturerId);
        }
    }

    // ADDED: Missing getLecturerCount method
    public int getLecturerCount() {
        return lecturers.size();
    }

    public String getAvailableLecturerIds() {
        return String.join(", ", lecturers.keySet());
    }
}
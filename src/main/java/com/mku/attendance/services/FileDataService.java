package com.mku.attendance.services;

import com.mku.attendance.entities.StudentData;
import com.mku.attendance.entities.HOD;
import com.mku.attendance.entities.Course;
import com.mku.attendance.entities.Unit;
import com.mku.attendance.entities.LecturerData;
import com.mku.attendance.entities.Attendance;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Service;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

@Service
public class FileDataService {

    // Use absolute path for deployment compatibility
    private static final String DATA_DIR;

    static {
        // Use user home directory for permanent storage
        String userHome = System.getProperty("user.home");
        DATA_DIR = Paths.get(userHome, "mku_attendance_data").toString() + File.separator;
    }

    private static final String HODS_FILE = DATA_DIR + "hods.json";
    private static final String STUDENTS_FILE = DATA_DIR + "students.json";
    private static final String COURSES_FILE = DATA_DIR + "courses.json";
    private static final String UNITS_FILE = DATA_DIR + "units.json";
    private static final String LECTURERS_FILE = DATA_DIR + "lecturers.json";
    private static final String ATTENDANCE_FILE = DATA_DIR + "attendance.json";

    private final ObjectMapper objectMapper;

    public FileDataService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
        this.objectMapper.registerModule(new JavaTimeModule());

        createDataDirectory();
        System.out.println("FileDataService initialized with data directory: " + DATA_DIR);
    }

    private void createDataDirectory() {
        File dataDir = new File(DATA_DIR);
        if (!dataDir.exists()) {
            boolean created = dataDir.mkdirs();
            if (created) {
                System.out.println("✅ Permanent data directory created: " + DATA_DIR);
            } else {
                System.err.println("❌ Failed to create data directory: " + DATA_DIR);
            }
        } else {
            System.out.println("✅ Using existing data directory: " + DATA_DIR);
        }
    }

    // Save HODs data
    public void saveHODs(Map<String, HOD> hods) {
        saveToFile(HODS_FILE, hods, "HODs");
    }

    // Load HODs data
    public Map<String, HOD> loadHODs() {
        return loadFromFile(HODS_FILE, HOD.class, "HODs");
    }

    // Save Students data
    public void saveStudents(Map<String, StudentData> students) {
        saveToFile(STUDENTS_FILE, students, "Students");
    }

    // Load Students data
    public Map<String, StudentData> loadStudents() {
        Map<String, StudentData> students = loadFromFile(STUDENTS_FILE, StudentData.class, "Students");

        if (!students.isEmpty()) {
            System.out.println("=== LOADED STUDENTS ===");
            students.forEach((id, student) -> {
                System.out.println("ID: " + id + ", Name: " + student.getName() +
                        ", Course: " + student.getCourse() +
                        ", Units: " + (student.getRegisteredUnits() != null ? student.getRegisteredUnits().size() : 0));
            });
            System.out.println("=======================");
        }

        return students;
    }

    // Save Courses data
    public void saveCourses(Map<String, Course> courses) {
        saveToFile(COURSES_FILE, courses, "Courses");
    }

    // Load Courses data
    public Map<String, Course> loadCourses() {
        return loadFromFile(COURSES_FILE, Course.class, "Courses");
    }

    // Save Units data
    public void saveUnits(Map<String, Unit> units) {
        saveToFile(UNITS_FILE, units, "Units");
    }

    // Load Units data
    public Map<String, Unit> loadUnits() {
        return loadFromFile(UNITS_FILE, Unit.class, "Units");
    }

    // Save Lecturers data
    public void saveLecturers(Map<String, LecturerData> lecturers) {
        saveToFile(LECTURERS_FILE, lecturers, "Lecturers");
    }

    // Load Lecturers data
    public Map<String, LecturerData> loadLecturers() {
        return loadFromFile(LECTURERS_FILE, LecturerData.class, "Lecturers");
    }

    // Save Attendance data
    public void saveAttendance(List<Attendance> attendanceRecords) {
        saveToFile(ATTENDANCE_FILE, attendanceRecords, "Attendance");
    }

    // Load Attendance data
    public List<Attendance> loadAttendance() {
        return loadListFromFile(ATTENDANCE_FILE, Attendance.class, "Attendance");
    }

    // Generic save method for Maps
    private <T> void saveToFile(String filename, Map<String, T> data, String dataType) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(filename), data);
            System.out.println("✅ " + dataType + " data saved successfully to: " + filename);
        } catch (IOException e) {
            System.err.println("❌ Error saving " + dataType + " data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Generic save method for Lists
    private <T> void saveToFile(String filename, List<T> data, String dataType) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(filename), data);
            System.out.println("✅ " + dataType + " data saved successfully to: " + filename);
        } catch (IOException e) {
            System.err.println("❌ Error saving " + dataType + " data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Generic load method for Maps
    private <T> Map<String, T> loadFromFile(String filename, Class<T> valueType, String dataType) {
        try {
            File file = new File(filename);
            if (file.exists()) {
                System.out.println("📁 Loading " + dataType + " from: " + filename);
                Map<String, T> data = objectMapper.readValue(file,
                        objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, valueType));
                System.out.println("✅ " + dataType + " data loaded successfully. Count: " + data.size());
                return data;
            } else {
                System.out.println("ℹ️ No " + dataType + " data file found: " + filename);
            }
        } catch (IOException e) {
            System.err.println("❌ Error loading " + dataType + " data: " + e.getMessage());
            e.printStackTrace();
        }
        return new HashMap<>();
    }

    // Generic load method for Lists
    private <T> List<T> loadListFromFile(String filename, Class<T> valueType, String dataType) {
        try {
            File file = new File(filename);
            if (file.exists()) {
                System.out.println("📁 Loading " + dataType + " from: " + filename);
                List<T> data = objectMapper.readValue(file,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, valueType));
                System.out.println("✅ " + dataType + " data loaded successfully. Count: " + data.size());
                return data;
            } else {
                System.out.println("ℹ️ No " + dataType + " data file found: " + filename);
            }
        } catch (IOException e) {
            System.err.println("❌ Error loading " + dataType + " data: " + e.getMessage());
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    // Comprehensive auto-save
    public void autoSaveAll(Map<String, HOD> hods,
                            Map<String, StudentData> students,
                            Map<String, Course> courses,
                            Map<String, Unit> units,
                            Map<String, LecturerData> lecturers,
                            List<Attendance> attendanceRecords) {
        saveHODs(hods);
        saveStudents(students);
        saveCourses(courses);
        saveUnits(units);
        saveLecturers(lecturers);
        saveAttendance(attendanceRecords);
        System.out.println("✅ All data auto-saved successfully to permanent storage");
    }

    // Get data directory path for info
    public String getDataDirectory() {
        return DATA_DIR;
    }
}
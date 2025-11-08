package com.mku.attendance.services;

import com.mku.attendance.entities.Course;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class CourseManager {
    private Map<String, Course> courses = new HashMap<>();
    private final FileDataService fileDataService;

    @Autowired
    public CourseManager(FileDataService fileDataService) {
        this.fileDataService = fileDataService;

        // Load courses from file on startup
        try {
            courses = fileDataService.loadCourses();
            if (courses == null) {
                courses = new HashMap<>();
                System.out.println("No courses data loaded, initializing empty course map");
            } else {
                System.out.println("CourseManager initialized with " + courses.size() + " courses");
            }
        } catch (Exception e) {
            System.err.println("Error loading courses: " + e.getMessage());
            courses = new HashMap<>();
        }

        System.out.println("✅ CourseManager initialized with file persistence");
    }

    public void addCourse(String code, String name) {
        if (code != null && name != null && !courses.containsKey(code)) {
            courses.put(code, new Course(code, name));
            saveCoursesToFile();
            System.out.println("✅ Course added and saved: code=" + code + ", name=" + name);
        } else {
            System.out.println("❌ Course addition failed: Course " + code + " already exists or invalid data");
        }
    }

    public Map<String, Course> getCourses() {
        return new HashMap<>(courses);
    }

    public Map<String, Course> getCourseDatabase() {
        return new HashMap<>(courses);
    }

    // Save courses to file
    public void saveCoursesToFile() {
        try {
            fileDataService.saveCourses(courses);
            System.out.println("✅ Courses data saved successfully");
        } catch (Exception e) {
            System.err.println("❌ Error saving courses data: " + e.getMessage());
        }
    }

    // Get course by code
    public Course getCourse(String code) {
        return courses.get(code);
    }

    // Check if course exists
    public boolean courseExists(String code) {
        return courses.containsKey(code);
    }

    // Remove course
    public boolean removeCourse(String code) {
        if (courses.containsKey(code)) {
            courses.remove(code);
            saveCoursesToFile();
            System.out.println("✅ Course removed: " + code);
            return true;
        }
        return false;
    }
}
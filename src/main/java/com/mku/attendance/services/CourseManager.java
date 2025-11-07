package com.mku.attendance.services;

import com.mku.attendance.entities.Course;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class CourseManager {
    private static final Map<String, Course> courses = new HashMap<>();

    public CourseManager() {
        System.out.println("CourseManager initialized");
    }

    public void addCourse(String code, String name) {
        courses.put(code, new Course(code, name));
        System.out.println("Course added: code=" + code + ", name=" + name);
    }

    public Map<String, Course> getCourses() {
        return courses;
    }

    public static Map<String, Course> getCourseDatabase() {
        return courses;
    }
}
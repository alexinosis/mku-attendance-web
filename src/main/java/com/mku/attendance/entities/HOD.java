package com.mku.attendance.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HOD {
    private String id;
    private String name;
    private String password;
    private String email;
    private String department;
    private List<String> courses;

    public HOD(String id, String password, String name) {
        this.id = id;
        this.password = password;
        this.name = name;
        this.email = "";
        this.department = "";
        this.courses = new ArrayList<>();
    }

    public HOD(String id, String name, String email, String department, String password) {
        this.id = id;
        this.name = name;
        this.email = email != null ? email.trim() : "";
        this.department = department != null ? department.trim() : "";
        this.password = password;
        this.courses = new ArrayList<>();
    }

    // Default constructor for JSON
    public HOD() {
        this.id = "";
        this.name = "";
        this.password = "";
        this.email = "";
        this.department = "";
        this.courses = new ArrayList<>();
    }

    // Proper setters for JSON deserialization
    public void setId(String id) {
        this.id = id != null ? id : "";
    }

    public void setName(String name) {
        this.name = name != null ? name : "";
    }

    public void setPassword(String password) {
        this.password = password != null ? password : "";
    }

    public void setEmail(String email) {
        this.email = email != null ? email : "";
    }

    public void setDepartment(String department) {
        this.department = department != null ? department : "";
    }

    public void setCourses(List<String> courses) {
        this.courses = courses != null ? courses : new ArrayList<>();
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getPassword() { return password; }
    public String getEmail() { return email; }
    public String getDepartment() { return department; }
    public List<String> getCourses() { return Collections.unmodifiableList(courses); }

    public void addCourse(String courseCode) {
        if (courseCode == null || courseCode.trim().isEmpty()) return;
        String code = courseCode.trim().toUpperCase();
        if (!courses.contains(code)) {
            courses.add(code);
            System.out.println("Course " + code + " assigned to HOD " + id);
        }
    }

    @Override
    public String toString() {
        return "HOD{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
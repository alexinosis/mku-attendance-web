package com.mku.attendance.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LecturerData {
    private String name;
    private String email;
    private String lecturerId;
    private String password;
    private String courseCode;
    private String unitCode;

    public LecturerData() {
    }

    public LecturerData(String name, String email, String lecturerId, String password, String courseCode, String unitCode) {
        this.name = name;
        this.email = email;
        this.lecturerId = lecturerId;
        this.password = password;
        this.courseCode = courseCode;
        this.unitCode = unitCode;
    }

    // Getters
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getLecturerId() { return lecturerId; }
    public String getPassword() { return password; }
    public String getCourseCode() { return courseCode; }
    public String getUnitCode() { return unitCode; }

    // Setters (required for JSON deserialization)
    public void setName(String name) { this.name = name != null ? name : ""; }
    public void setEmail(String email) { this.email = email != null ? email : ""; }
    public void setLecturerId(String lecturerId) { this.lecturerId = lecturerId != null ? lecturerId : ""; }
    public void setPassword(String password) { this.password = password != null ? password : ""; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode != null ? courseCode : ""; }
    public void setUnitCode(String unitCode) { this.unitCode = unitCode != null ? unitCode : ""; }
}
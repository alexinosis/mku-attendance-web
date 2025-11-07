package com.mku.attendance.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Unit {
    private String code;
    private String name;
    private String courseCode;

    // Default constructor (REQUIRED for JSON deserialization)
    public Unit() {
        this.code = "";
        this.name = "";
        this.courseCode = "";
    }

    public Unit(String code, String name, String courseCode) {
        this.code = code;
        this.name = name;
        this.courseCode = courseCode;
    }

    // Getters
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getCourseCode() { return courseCode; }

    // Setters (REQUIRED for JSON deserialization)
    public void setCode(String code) { this.code = code != null ? code : ""; }
    public void setName(String name) { this.name = name != null ? name : ""; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode != null ? courseCode : ""; }

    @Override
    public String toString() {
        return "Unit{code='" + code + "', name='" + name + "', courseCode='" + courseCode + "'}";
    }
}
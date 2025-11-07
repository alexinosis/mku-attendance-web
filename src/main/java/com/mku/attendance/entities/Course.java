package com.mku.attendance.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Course {
    private String code;
    private String name;

    // Constructor with parameters
    public Course(String code, String name) {
        this.code = code;
        this.name = name;
    }

    // Default constructor (REQUIRED for JSON deserialization)
    public Course() {
        this.code = "";
        this.name = "";
    }

    // Getters
    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    // Setters (REQUIRED for JSON deserialization)
    public void setCode(String code) {
        this.code = code != null ? code : "";
    }

    public void setName(String name) {
        this.name = name != null ? name : "";
    }

    @Override
    public String toString() {
        return "Course{code='" + code + "', name='" + name + "'}";
    }
}
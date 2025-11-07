package com.mku.attendance.entities;

public class Attendance {
    private String studentId;
    private String unitCode;
    private String date;
    private boolean present;

    public Attendance(String studentId, String unitCode, String date, boolean present) {
        this.studentId = studentId;
        this.unitCode = unitCode;
        this.date = date;
        this.present = present;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getUnitCode() {
        return unitCode;
    }

    public String getDate() {
        return date;
    }

    public boolean isPresent() {
        return present;
    }
}
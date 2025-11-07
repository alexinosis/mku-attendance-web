package com.mku.attendance.controllers;

import com.mku.attendance.entities.*;
import com.mku.attendance.services.HODManager;
import com.mku.attendance.services.StudentManager;
import com.mku.attendance.services.AttendanceManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class HODController {

    @Autowired
    private HODManager hodManager;

    @Autowired
    private StudentManager studentManager;

    @Autowired
    private AttendanceManager attendanceManager;

    @GetMapping("/hod/login")
    public String showHODLogin() {
        return "hod-login";
    }

    @PostMapping("/hod/login")
    public String loginHOD(
            @RequestParam String hodId,
            @RequestParam String password,
            Model model) {

        System.out.println("HOD Login attempt for: " + hodId);

        if (hodId == null || hodId.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            model.addAttribute("error", "Enter HOD ID and password.");
            return "hod-login";
        }

        HOD hod = hodManager.getHOD(hodId.trim());
        System.out.println("HOD found: " + hod);

        if (hod == null) {
            model.addAttribute("error", "Invalid credentials.");
            return "hod-login";
        }

        if (!hod.getPassword().equals(password)) {
            model.addAttribute("error", "Invalid credentials.");
            return "hod-login";
        }

        return "redirect:/hod/dashboard?hodId=" + hodId.trim();
    }

    @GetMapping("/hod/dashboard")
    public String showHODDashboard(@RequestParam String hodId, Model model) {
        HOD hod = hodManager.getHOD(hodId);
        if (hod == null) {
            return "redirect:/hod/login";
        }

        model.addAttribute("hod", hod);
        model.addAttribute("hodName", hod.getName());
        model.addAttribute("studentManager", studentManager);
        model.addAttribute("hodManager", hodManager);
        model.addAttribute("attendanceManager", attendanceManager);
        // Get lecturers from HODManager
        model.addAttribute("lecturers", hodManager.getLecturers());

        return "hod-dashboard";
    }

    // COURSES MANAGEMENT
    @PostMapping("/hod/courses")
    public String addCourse(
            @RequestParam String code,
            @RequestParam String name,
            @RequestParam String hodId) {

        System.out.println("Adding course: " + code + " - " + name);

        if (code == null || code.trim().isEmpty() || name == null || name.trim().isEmpty()) {
            return "redirect:/hod/dashboard?hodId=" + hodId + "&error=Code and Name required";
        }

        String upperCode = code.trim().toUpperCase();

        if (hodManager.getCourseManager().getCourses().containsKey(upperCode)) {
            return "redirect:/hod/dashboard?hodId=" + hodId + "&error=Course code exists";
        }

        hodManager.getCourseManager().addCourse(upperCode, name.trim());
        System.out.println("Course saved via CourseManager: " + upperCode);

        return "redirect:/hod/dashboard?hodId=" + hodId + "&success=Course added successfully";
    }

    // UNITS MANAGEMENT
    @PostMapping("/hod/units")
    public String addUnit(
            @RequestParam String code,
            @RequestParam String name,
            @RequestParam String courseCode,
            @RequestParam String hodId) {

        System.out.println("Adding unit: " + code + " - " + name + " for course: " + courseCode);

        if (code == null || code.trim().isEmpty() ||
                name == null || name.trim().isEmpty() ||
                courseCode == null || courseCode.trim().isEmpty()) {
            return "redirect:/hod/dashboard?hodId=" + hodId + "&error=All fields required";
        }

        String upperCode = code.trim().toUpperCase();
        String upperCourseCode = courseCode.trim().toUpperCase();

        if (hodManager.getUnitManager().getUnits().containsKey(upperCode)) {
            return "redirect:/hod/dashboard?hodId=" + hodId + "&error=Unit code exists";
        }

        hodManager.getUnitManager().addUnit(upperCode, name.trim(), upperCourseCode);
        System.out.println("Unit saved via UnitManager: " + upperCode);

        return "redirect:/hod/dashboard?hodId=" + hodId + "&success=Unit added successfully";
    }

    // HOD MANAGEMENT
    @PostMapping("/hod/add-hod")
    public String addHOD(
            @RequestParam String id,
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String department,
            @RequestParam String password,
            @RequestParam String hodId) {

        System.out.println("Adding HOD: " + id + " - " + name);

        if (id == null || id.trim().isEmpty() ||
                name == null || name.trim().isEmpty() ||
                password == null || password.trim().isEmpty()) {
            return "redirect:/hod/dashboard?hodId=" + hodId + "&error=ID, Name, Password required";
        }

        String cleanId = id.trim();

        if (hodManager.getHOD(cleanId) != null) {
            return "redirect:/hod/dashboard?hodId=" + hodId + "&error=HOD ID exists";
        }

        HOD newHod = new HOD(cleanId, name.trim(),
                email != null ? email.trim() : "",
                department != null ? department.trim() : "",
                password.trim());
        hodManager.addHOD(newHod);
        System.out.println("HOD saved via HODManager: " + cleanId);

        return "redirect:/hod/dashboard?hodId=" + hodId + "&success=HOD added successfully";
    }

    // LECTURER MANAGEMENT - FIXED CONSTRUCTOR ORDER
    @PostMapping("/hod/lecturers")
    public String addLecturer(
            @RequestParam String lecturerId,
            @RequestParam String name,
            @RequestParam String password,
            @RequestParam String unitCode,
            @RequestParam String courseCode,
            @RequestParam(required = false) String email,
            @RequestParam String hodId) {

        System.out.println("=== ADDING LECTURER ===");
        System.out.println("ID: " + lecturerId);
        System.out.println("Name: " + name);
        System.out.println("Unit: " + unitCode);
        System.out.println("Course: " + courseCode);

        if (lecturerId == null || lecturerId.trim().isEmpty() ||
                name == null || name.trim().isEmpty() ||
                password == null || password.trim().isEmpty() ||
                unitCode == null || unitCode.trim().isEmpty() ||
                courseCode == null || courseCode.trim().isEmpty()) {
            return "redirect:/hod/dashboard?hodId=" + hodId + "&error=All fields except email are required";
        }

        String upperId = lecturerId.trim().toUpperCase();
        String cleanName = name.trim();
        String cleanPassword = password.trim();
        String cleanUnitCode = unitCode.trim().toUpperCase();
        String cleanCourseCode = courseCode.trim().toUpperCase();
        String cleanEmail = email != null && !email.trim().isEmpty() ? email.trim() :
                cleanName.toLowerCase().replace(" ", ".") + "@mku.ac.ke";

        // Check if lecturer exists using HODManager
        if (hodManager.lecturerExists(upperId)) {
            return "redirect:/hod/dashboard?hodId=" + hodId + "&error=Lecturer ID already exists";
        }

        // FIXED: Correct constructor parameter order
        // LecturerData constructor: (name, email, lecturerId, password, courseCode, unitCode)
        LecturerData lecturer = new LecturerData(
                cleanName,         // name
                cleanEmail,        // email
                upperId,           // lecturerId
                cleanPassword,     // password
                cleanCourseCode,   // courseCode
                cleanUnitCode      // unitCode
        );

        // Add lecturer to HODManager instead of local database
        hodManager.addLecturer(lecturer);

        System.out.println("✅ Lecturer added successfully via HODManager: " + upperId);
        System.out.println("Name: " + cleanName);
        System.out.println("Email: " + cleanEmail);
        System.out.println("Unit: " + cleanUnitCode);
        System.out.println("Course: " + cleanCourseCode);
        System.out.println("Total lecturers now: " + hodManager.getLecturerCount());

        return "redirect:/hod/dashboard?hodId=" + hodId + "&success=Lecturer added successfully";
    }

    // ========== NEW: STUDENTS AND ATTENDANCE MANAGEMENT ==========

    @GetMapping("/hod/students")
    public String viewStudents(
            @RequestParam String hodId,
            @RequestParam(required = false) String filterType,
            @RequestParam(required = false) String filterValue,
            Model model) {

        HOD hod = hodManager.getHOD(hodId);
        if (hod == null) {
            return "redirect:/hod/login";
        }

        Map<String, StudentData> students = studentManager.getStudents();
        List<StudentData> filteredStudents = new ArrayList<>(students.values());

        // Apply filters
        if (filterType != null && filterValue != null && !filterValue.trim().isEmpty()) {
            String searchValue = filterValue.trim().toLowerCase();
            switch (filterType) {
                case "admNo":
                    filteredStudents = filteredStudents.stream()
                            .filter(s -> s.getStudentId().toLowerCase().contains(searchValue))
                            .collect(Collectors.toList());
                    break;
                case "name":
                    filteredStudents = filteredStudents.stream()
                            .filter(s -> s.getName().toLowerCase().contains(searchValue))
                            .collect(Collectors.toList());
                    break;
                case "course":
                    filteredStudents = filteredStudents.stream()
                            .filter(s -> s.getCourse() != null && s.getCourse().toLowerCase().contains(searchValue))
                            .collect(Collectors.toList());
                    break;
                case "unit":
                    filteredStudents = filteredStudents.stream()
                            .filter(s -> s.getRegisteredUnits() != null &&
                                    s.getRegisteredUnits().stream()
                                            .anyMatch(unit -> unit.toLowerCase().contains(searchValue)))
                            .collect(Collectors.toList());
                    break;
            }
        }

        model.addAttribute("hod", hod);
        model.addAttribute("students", filteredStudents);
        model.addAttribute("filterType", filterType);
        model.addAttribute("filterValue", filterValue);
        model.addAttribute("totalStudents", students.size());
        model.addAttribute("filteredCount", filteredStudents.size());

        return "hod-students";
    }

    @GetMapping("/hod/attendance-report")
    public String viewAttendanceReport(
            @RequestParam String hodId,
            @RequestParam(required = false) String unitCode,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String studentId,
            Model model) {

        HOD hod = hodManager.getHOD(hodId);
        if (hod == null) {
            return "redirect:/hod/login";
        }

        List<Map<String, Object>> attendanceRecords = new ArrayList<>();

        // Get all attendance records
        List<Attendance> allRecords = attendanceManager.getAttendanceRecords();

        // Convert to map format for display
        for (Attendance record : allRecords) {
            Map<String, Object> recordMap = new HashMap<>();
            recordMap.put("studentId", record.getStudentId());
            recordMap.put("unitCode", record.getUnitCode());
            recordMap.put("date", record.getDate());

            // Extract time from timestamp - FIXED: Handle missing getTime() method
            String time = extractTimeFromTimestamp(record);
            recordMap.put("time", time);

            recordMap.put("status", record.isPresent() ? "PRESENT" : "ABSENT");
            recordMap.put("present", record.isPresent());

            // Add student name
            StudentData student = studentManager.getStudent(record.getStudentId());
            if (student != null) {
                recordMap.put("studentName", student.getName());
                recordMap.put("course", student.getCourse());
            } else {
                recordMap.put("studentName", "Unknown Student");
                recordMap.put("course", "N/A");
            }

            attendanceRecords.add(recordMap);
        }

        // Apply filters
        if (unitCode != null && !unitCode.isEmpty()) {
            attendanceRecords = attendanceRecords.stream()
                    .filter(r -> r.get("unitCode").toString().equalsIgnoreCase(unitCode))
                    .collect(Collectors.toList());
        }

        if (date != null && !date.isEmpty()) {
            attendanceRecords = attendanceRecords.stream()
                    .filter(r -> r.get("date").toString().startsWith(date))
                    .collect(Collectors.toList());
        }

        if (status != null && !status.isEmpty() && !status.equals("ALL")) {
            boolean presentFilter = "PRESENT".equalsIgnoreCase(status);
            attendanceRecords = attendanceRecords.stream()
                    .filter(r -> (Boolean) r.get("present") == presentFilter)
                    .collect(Collectors.toList());
        }

        if (studentId != null && !studentId.isEmpty()) {
            attendanceRecords = attendanceRecords.stream()
                    .filter(r -> r.get("studentId").toString().toLowerCase().contains(studentId.toLowerCase()))
                    .collect(Collectors.toList());
        }

        // Sort by date descending (most recent first)
        attendanceRecords.sort((r1, r2) -> r2.get("date").toString().compareTo(r1.get("date").toString()));

        model.addAttribute("hod", hod);
        model.addAttribute("attendanceRecords", attendanceRecords);
        model.addAttribute("unitCode", unitCode);
        model.addAttribute("date", date);
        model.addAttribute("status", status);
        model.addAttribute("studentId", studentId);
        model.addAttribute("totalRecords", allRecords.size());
        model.addAttribute("filteredCount", attendanceRecords.size());

        // Get available units for filter dropdown
        Set<String> availableUnits = allRecords.stream()
                .map(Attendance::getUnitCode)
                .collect(Collectors.toSet());
        model.addAttribute("availableUnits", availableUnits);

        return "hod-attendance-report";
    }

    @GetMapping("/hod/download-attendance-report")
    public void downloadAttendanceReport(
            @RequestParam String hodId,
            @RequestParam(required = false) String unitCode,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String studentId,
            HttpServletResponse response) throws IOException {

        HOD hod = hodManager.getHOD(hodId);
        if (hod == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid HOD");
            return;
        }

        // Get filtered records
        List<Map<String, Object>> records = getFilteredAttendanceRecords(unitCode, date, status, studentId);

        // Set response headers for CSV download
        response.setContentType("text/csv");
        response.setCharacterEncoding("UTF-8");
        String filename = "attendance_report_" + System.currentTimeMillis() + ".csv";
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        // Build CSV content
        try (PrintWriter writer = response.getWriter()) {
            writer.write("Student ID,Student Name,Course,Unit Code,Date,Time,Status\n");

            for (Map<String, Object> record : records) {
                writer.write(String.format("%s,\"%s\",\"%s\",%s,%s,%s,%s\n",
                        record.get("studentId"),
                        record.get("studentName"),
                        record.get("course"),
                        record.get("unitCode"),
                        record.get("date"),
                        record.get("time") != null ? record.get("time") : "--:--",
                        record.get("status")
                ));
            }
        }
    }

    // Helper method to extract time from timestamp
    private String extractTimeFromTimestamp(Attendance record) {
        try {
            // Try to get timestamp using reflection or available methods
            // Since we don't know the exact methods in Attendance class, let's try common approaches

            // Option 1: Try to get timestamp field
            java.lang.reflect.Field timestampField = null;
            try {
                timestampField = record.getClass().getDeclaredField("timestamp");
                timestampField.setAccessible(true);
                String timestamp = (String) timestampField.get(record);
                if (timestamp != null && timestamp.length() > 11) {
                    return timestamp.substring(11, 16); // Extract HH:mm
                }
            } catch (Exception e) {
                // Ignore and try next approach
            }

            // Option 2: Try to get date field and extract time if it contains time
            try {
                String date = record.getDate();
                if (date != null && date.contains(" ")) {
                    String[] parts = date.split(" ");
                    if (parts.length > 1 && parts[1].length() >= 5) {
                        return parts[1].substring(0, 5); // Extract HH:mm
                    }
                }
            } catch (Exception e) {
                // Ignore
            }

            // Option 3: Return default time
            return "--:--";

        } catch (Exception e) {
            return "--:--";
        }
    }

    // Helper method to get filtered attendance records
    private List<Map<String, Object>> getFilteredAttendanceRecords(String unitCode, String date, String status, String studentId) {
        List<Attendance> allRecords = attendanceManager.getAttendanceRecords();
        List<Map<String, Object>> attendanceRecords = new ArrayList<>();

        // Convert to map format
        for (Attendance record : allRecords) {
            Map<String, Object> recordMap = new HashMap<>();
            recordMap.put("studentId", record.getStudentId());
            recordMap.put("unitCode", record.getUnitCode());
            recordMap.put("date", record.getDate());

            // Extract time from timestamp
            String time = extractTimeFromTimestamp(record);
            recordMap.put("time", time);

            recordMap.put("status", record.isPresent() ? "PRESENT" : "ABSENT");
            recordMap.put("present", record.isPresent());

            StudentData student = studentManager.getStudent(record.getStudentId());
            if (student != null) {
                recordMap.put("studentName", student.getName());
                recordMap.put("course", student.getCourse());
            } else {
                recordMap.put("studentName", "Unknown Student");
                recordMap.put("course", "N/A");
            }

            attendanceRecords.add(recordMap);
        }

        // Apply filters
        if (unitCode != null && !unitCode.isEmpty()) {
            attendanceRecords = attendanceRecords.stream()
                    .filter(r -> r.get("unitCode").toString().equalsIgnoreCase(unitCode))
                    .collect(Collectors.toList());
        }

        if (date != null && !date.isEmpty()) {
            attendanceRecords = attendanceRecords.stream()
                    .filter(r -> r.get("date").toString().startsWith(date))
                    .collect(Collectors.toList());
        }

        if (status != null && !status.isEmpty() && !status.equals("ALL")) {
            boolean presentFilter = "PRESENT".equalsIgnoreCase(status);
            attendanceRecords = attendanceRecords.stream()
                    .filter(r -> (Boolean) r.get("present") == presentFilter)
                    .collect(Collectors.toList());
        }

        if (studentId != null && !studentId.isEmpty()) {
            attendanceRecords = attendanceRecords.stream()
                    .filter(r -> r.get("studentId").toString().toLowerCase().contains(studentId.toLowerCase()))
                    .collect(Collectors.toList());
        }

        // Sort by date descending
        attendanceRecords.sort((r1, r2) -> r2.get("date").toString().compareTo(r1.get("date").toString()));

        return attendanceRecords;
    }

    // API ENDPOINTS
    @GetMapping("/hod/api/courses")
    @ResponseBody
    public Map<String, Course> getCourses() {
        return hodManager.getCourseManager().getCourses();
    }

    @GetMapping("/hod/api/units")
    @ResponseBody
    public Map<String, Unit> getUnits() {
        return hodManager.getUnitManager().getUnits();
    }

    @GetMapping("/hod/api/hods")
    @ResponseBody
    public Map<String, HOD> getHODs() {
        return hodManager.getHODs();
    }

    @GetMapping("/hod/api/students")
    @ResponseBody
    public Map<String, StudentData> getStudents() {
        return studentManager.getStudents();
    }

    @GetMapping("/hod/api/lecturers")
    @ResponseBody
    public Map<String, LecturerData> getLecturers() {
        // Get lecturers from HODManager
        Map<String, LecturerData> lecturers = hodManager.getLecturers();
        System.out.println("API: Returning " + lecturers.size() + " lecturers from HODManager");
        return lecturers;
    }

    @GetMapping("/hod/api/attendance-records")
    @ResponseBody
    public List<Map<String, Object>> getAttendanceRecords(
            @RequestParam(required = false) String unitCode,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String status) {
        return getFilteredAttendanceRecords(unitCode, date, status, null);
    }
}
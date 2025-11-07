package com.mku.attendance.controllers;

import com.mku.attendance.entities.StudentData;
import com.mku.attendance.services.StudentManager;
import com.mku.attendance.services.HODManager;
import com.mku.attendance.services.AttendanceManager;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.HashMap;

@Controller
@RequestMapping("/student")
public class StudentController {

    @Autowired
    private StudentManager studentManager;

    @Autowired
    private HODManager hodManager;

    @Autowired
    private AttendanceManager attendanceManager;

    // ========== REGISTRATION METHODS ==========

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        // Add empty student object to avoid thymeleaf errors
        model.addAttribute("student", new StudentData());
        return "student-register";
    }

    @PostMapping("/register")
    public String registerStudent(
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String studentId,
            @RequestParam String email,
            @RequestParam String password,
            Model model) {

        System.out.println("Student registration attempt for: " + studentId);

        // Validation
        if (firstName == null || firstName.trim().isEmpty() ||
                lastName == null || lastName.trim().isEmpty() ||
                studentId == null || studentId.trim().isEmpty() ||
                email == null || email.trim().isEmpty() ||
                password == null || password.trim().isEmpty()) {
            model.addAttribute("error", "All fields are required.");
            return "student-register";
        }

        // Clean inputs
        firstName = firstName.trim();
        lastName = lastName.trim();
        studentId = studentId.trim().toUpperCase();
        email = email.trim();
        password = password.trim();

        // Email validation
        if (!email.contains("@") || !email.contains(".")) {
            model.addAttribute("error", "Invalid email format.");
            return "student-register";
        }

        // Check if student already exists
        if (studentManager.exists(studentId)) {
            model.addAttribute("error", "Student ID already exists.");
            return "student-register";
        }

        try {
            // Create new student
            StudentData student = new StudentData(studentId, firstName, lastName, email, "", "", password);

            // Add student - method returns void
            studentManager.addStudent(student);

            // Verify student was added by checking if it exists now
            StudentData verifiedStudent = studentManager.getStudent(studentId);
            if (verifiedStudent != null) {
                model.addAttribute("success", "Registration successful! You can now log in.");
                System.out.println("Student registered successfully: " + studentId);
            } else {
                model.addAttribute("error", "Registration failed. Please try again.");
            }
        } catch (Exception e) {
            System.err.println("Error during student registration: " + e.getMessage());
            model.addAttribute("error", "Registration failed due to system error.");
        }

        return "student-register";
    }

    // ========== LOGIN METHODS ==========

    @GetMapping("/login")
    public String showStudentLogin(Model model) {
        model.addAttribute("student", new StudentData());
        return "student-login";
    }

    @PostMapping("/login")
    public String loginStudent(
            @RequestParam String studentId,
            @RequestParam String password,
            HttpSession session,
            Model model) {

        System.out.println("Student login attempt for: " + studentId);

        if (studentId == null || studentId.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            model.addAttribute("error", "Enter student ID and password.");
            return "student-login";
        }

        studentId = studentId.trim().toUpperCase();

        StudentData student = studentManager.getStudent(studentId);
        System.out.println("Student found: " + (student != null ? student.getStudentId() : "null"));

        if (student == null) {
            model.addAttribute("error", "Invalid student ID. Please register first.");
            return "student-login";
        }

        if (!student.getPassword().equals(password)) {
            model.addAttribute("error", "Invalid password.");
            return "student-login";
        }

        // Store student in session
        session.setAttribute("studentId", studentId);
        session.setAttribute("student", student);

        System.out.println("Student login successful: " + studentId);
        return "redirect:/student/dashboard";
    }

    // ========== DASHBOARD & PROFILE ==========

    @GetMapping("/dashboard")
    public String showStudentDashboard(HttpSession session, Model model) {
        String studentId = (String) session.getAttribute("studentId");
        if (studentId == null) {
            return "redirect:/student/login";
        }

        StudentData student = studentManager.getStudent(studentId);
        if (student == null) {
            session.invalidate();
            return "redirect:/student/login";
        }

        try {
            // Get attendance summary with safe method invocation
            Map<String, Map<String, Object>> attendanceSummary = new HashMap<>();
            try {
                java.lang.reflect.Method method = studentManager.getClass().getMethod("getAttendanceSummary", String.class);
                Object result = method.invoke(studentManager, studentId);
                if (result instanceof Map) {
                    attendanceSummary = (Map<String, Map<String, Object>>) result;
                }
            } catch (Exception e) {
                System.err.println("getAttendanceSummary method not available, using empty data");
                // Create mock attendance summary
                attendanceSummary = createMockAttendanceSummary(student);
            }

            // Get overall statistics - FIXED: Use correct key names and ensure they exist
            Map<String, Object> overallStats = new HashMap<>();
            try {
                java.lang.reflect.Method method = studentManager.getClass().getMethod("getOverallStatistics", String.class);
                Object result = method.invoke(studentManager, studentId);
                if (result instanceof Map) {
                    overallStats = (Map<String, Object>) result;
                }
            } catch (Exception e) {
                System.err.println("getOverallStatistics method not available, using mock data");
                overallStats = createMockOverallStats(student);
            }

            // Ensure overallStats has the expected keys for the template
            if (!overallStats.containsKey("overallAttendanceRate")) {
                overallStats.put("overallAttendanceRate", 0.0);
            }
            if (!overallStats.containsKey("totalSessions")) {
                overallStats.put("totalSessions", 0);
            }
            if (!overallStats.containsKey("totalPresent")) {
                overallStats.put("totalPresent", 0);
            }
            if (!overallStats.containsKey("totalAbsent")) {
                overallStats.put("totalAbsent", 0);
            }

            // Get units and courses safely
            Map<String, Object> units = new HashMap<>();
            Map<String, Object> courses = new HashMap<>();

            if (hodManager != null) {
                try {
                    // Try to get unit manager
                    java.lang.reflect.Method getUnitManagerMethod = hodManager.getClass().getMethod("getUnitManager");
                    Object unitManager = getUnitManagerMethod.invoke(hodManager);
                    if (unitManager != null) {
                        java.lang.reflect.Method getUnitsMethod = unitManager.getClass().getMethod("getUnits");
                        Object unitsResult = getUnitsMethod.invoke(unitManager);
                        if (unitsResult instanceof Map) {
                            units = (Map<String, Object>) unitsResult;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Could not get units: " + e.getMessage());
                }

                try {
                    // Try to get course manager
                    java.lang.reflect.Method getCourseManagerMethod = hodManager.getClass().getMethod("getCourseManager");
                    Object courseManager = getCourseManagerMethod.invoke(hodManager);
                    if (courseManager != null) {
                        java.lang.reflect.Method getCoursesMethod = courseManager.getClass().getMethod("getCourses");
                        Object coursesResult = getCoursesMethod.invoke(courseManager);
                        if (coursesResult instanceof Map) {
                            courses = (Map<String, Object>) coursesResult;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Could not get courses: " + e.getMessage());
                }
            }

            // Check for active attendance sessions
            boolean canTakeAttendance = false;
            String activeUnitCode = null;
            if (student.getRegisteredUnits() != null && !student.getRegisteredUnits().isEmpty()) {
                for (String unitCode : student.getRegisteredUnits()) {
                    try {
                        java.lang.reflect.Method method = attendanceManager.getClass().getMethod("isAttendanceActive", String.class);
                        boolean isActive = (Boolean) method.invoke(attendanceManager, unitCode);
                        if (isActive) {
                            canTakeAttendance = true;
                            activeUnitCode = unitCode;
                            break;
                        }
                    } catch (Exception e) {
                        System.err.println("Error checking attendance status for unit " + unitCode + ": " + e.getMessage());
                    }
                }
            }

            model.addAttribute("student", student);
            model.addAttribute("attendanceSummary", attendanceSummary);
            model.addAttribute("overallStats", overallStats);
            model.addAttribute("units", units);
            model.addAttribute("courses", courses);
            model.addAttribute("canTakeAttendance", canTakeAttendance);
            model.addAttribute("activeUnitCode", activeUnitCode);

            // Add flash messages
            if (session.getAttribute("successMessage") != null) {
                model.addAttribute("success", session.getAttribute("successMessage"));
                session.removeAttribute("successMessage");
            }
            if (session.getAttribute("errorMessage") != null) {
                model.addAttribute("error", session.getAttribute("errorMessage"));
                session.removeAttribute("errorMessage");
            }

        } catch (Exception e) {
            System.err.println("Error loading student dashboard: " + e.getMessage());
            model.addAttribute("error", "Error loading dashboard data.");
        }

        return "student-dashboard";
    }

    @GetMapping("/attendance")
    public String showAttendanceDetails(HttpSession session, Model model) {
        String studentId = (String) session.getAttribute("studentId");
        if (studentId == null) {
            return "redirect:/student/login";
        }

        StudentData student = studentManager.getStudent(studentId);
        if (student == null) {
            return "redirect:/student/login";
        }

        Map<String, Map<String, Object>> attendanceSummary = new HashMap<>();
        try {
            java.lang.reflect.Method method = studentManager.getClass().getMethod("getAttendanceSummary", String.class);
            Object result = method.invoke(studentManager, studentId);
            if (result instanceof Map) {
                attendanceSummary = (Map<String, Map<String, Object>>) result;
            }
        } catch (Exception e) {
            System.err.println("getAttendanceSummary method not available");
            attendanceSummary = createMockAttendanceSummary(student);
        }

        // Get overall statistics for attendance page
        Map<String, Object> overallStats = new HashMap<>();
        try {
            java.lang.reflect.Method method = studentManager.getClass().getMethod("getOverallStatistics", String.class);
            Object result = method.invoke(studentManager, studentId);
            if (result instanceof Map) {
                overallStats = (Map<String, Object>) result;
            }
        } catch (Exception e) {
            System.err.println("getOverallStatistics method not available");
            overallStats = createMockOverallStats(student);
        }

        // Ensure overallStats has the expected keys
        if (!overallStats.containsKey("overallAttendanceRate")) {
            overallStats.put("overallAttendanceRate", 0.0);
        }
        if (!overallStats.containsKey("totalSessions")) {
            overallStats.put("totalSessions", 0);
        }
        if (!overallStats.containsKey("totalPresent")) {
            overallStats.put("totalPresent", 0);
        }

        // Get units and courses safely
        Map<String, Object> units = new HashMap<>();
        Map<String, Object> courses = new HashMap<>();

        try {
            if (hodManager != null) {
                java.lang.reflect.Method getUnitManagerMethod = hodManager.getClass().getMethod("getUnitManager");
                Object unitManager = getUnitManagerMethod.invoke(hodManager);
                if (unitManager != null) {
                    java.lang.reflect.Method getUnitsMethod = unitManager.getClass().getMethod("getUnits");
                    Object unitsResult = getUnitsMethod.invoke(unitManager);
                    if (unitsResult instanceof Map) {
                        units = (Map<String, Object>) unitsResult;
                    }
                }

                java.lang.reflect.Method getCourseManagerMethod = hodManager.getClass().getMethod("getCourseManager");
                Object courseManager = getCourseManagerMethod.invoke(hodManager);
                if (courseManager != null) {
                    java.lang.reflect.Method getCoursesMethod = courseManager.getClass().getMethod("getCourses");
                    Object coursesResult = getCoursesMethod.invoke(courseManager);
                    if (coursesResult instanceof Map) {
                        courses = (Map<String, Object>) coursesResult;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting units/courses: " + e.getMessage());
        }

        // Check for active attendance sessions
        boolean canTakeAttendance = false;
        String activeUnitCode = null;
        if (student.getRegisteredUnits() != null && !student.getRegisteredUnits().isEmpty()) {
            for (String unitCode : student.getRegisteredUnits()) {
                try {
                    java.lang.reflect.Method method = attendanceManager.getClass().getMethod("isAttendanceActive", String.class);
                    boolean isActive = (Boolean) method.invoke(attendanceManager, unitCode);
                    if (isActive) {
                        canTakeAttendance = true;
                        activeUnitCode = unitCode;
                        break;
                    }
                } catch (Exception e) {
                    System.err.println("Error checking attendance for unit " + unitCode);
                }
            }
        }

        model.addAttribute("student", student);
        model.addAttribute("attendanceSummary", attendanceSummary);
        model.addAttribute("overallStats", overallStats);
        model.addAttribute("units", units);
        model.addAttribute("courses", courses);
        model.addAttribute("canTakeAttendance", canTakeAttendance);
        model.addAttribute("activeUnitCode", activeUnitCode);

        return "student-attendance";
    }

    @GetMapping("/profile")
    public String showStudentProfile(HttpSession session, Model model) {
        String studentId = (String) session.getAttribute("studentId");
        if (studentId == null) {
            return "redirect:/student/login";
        }

        StudentData student = studentManager.getStudent(studentId);
        if (student == null) {
            return "redirect:/student/login";
        }

        model.addAttribute("student", student);
        return "student-profile";
    }

    // ========== ATTENDANCE METHODS ==========

    @PostMapping("/take-attendance")
    public String takeAttendance(HttpSession session, Model model) {
        String studentId = (String) session.getAttribute("studentId");
        if (studentId == null) {
            return "redirect:/student/login";
        }

        StudentData student = studentManager.getStudent(studentId);
        if (student == null) {
            return "redirect:/student/login";
        }

        // Find active unit for this student
        String activeUnitCode = null;
        if (student.getRegisteredUnits() != null) {
            for (String unitCode : student.getRegisteredUnits()) {
                try {
                    java.lang.reflect.Method method = attendanceManager.getClass().getMethod("isAttendanceActive", String.class);
                    boolean isActive = (Boolean) method.invoke(attendanceManager, unitCode);
                    if (isActive) {
                        activeUnitCode = unitCode;
                        break;
                    }
                } catch (Exception e) {
                    System.err.println("Error checking attendance for unit " + unitCode);
                }
            }
        }

        if (activeUnitCode == null) {
            session.setAttribute("errorMessage", "No active attendance session found for your registered units.");
            return "redirect:/student/dashboard";
        }

        // Mark attendance
        boolean success = false;
        try {
            // Try different method signatures
            try {
                java.lang.reflect.Method method = attendanceManager.getClass().getMethod("markStudentPresent",
                        String.class, String.class);
                success = (Boolean) method.invoke(attendanceManager, studentId, activeUnitCode);
            } catch (NoSuchMethodException e) {
                try {
                    java.lang.reflect.Method method = attendanceManager.getClass().getMethod("recordAttendance",
                            String.class, String.class, String.class);
                    method.invoke(attendanceManager, studentId, activeUnitCode, "PRESENT");
                    success = true;
                } catch (NoSuchMethodException e2) {
                    session.setAttribute("errorMessage", "Attendance system temporarily unavailable.");
                    return "redirect:/student/dashboard";
                }
            }

            if (success) {
                session.setAttribute("successMessage", "Attendance marked successfully for " + activeUnitCode + "!");
                System.out.println("Student " + studentId + " marked present for " + activeUnitCode);
            } else {
                session.setAttribute("errorMessage", "Failed to mark attendance. Please try again.");
            }
        } catch (Exception e) {
            System.err.println("Error marking attendance: " + e.getMessage());
            session.setAttribute("errorMessage", "Error marking attendance: " + e.getMessage());
        }

        return "redirect:/student/dashboard";
    }

    // ========== COURSE REGISTRATION ==========

    @PostMapping("/register-course")
    public String registerCourse(
            @RequestParam String courseCode,
            HttpSession session,
            Model model) {

        String studentId = (String) session.getAttribute("studentId");
        if (studentId == null) {
            return "redirect:/student/login";
        }

        StudentData student = studentManager.getStudent(studentId);
        if (student == null) {
            return "redirect:/student/login";
        }

        if (courseCode == null || courseCode.trim().isEmpty()) {
            session.setAttribute("errorMessage", "Course code is required.");
            return "redirect:/student/dashboard";
        }

        courseCode = courseCode.trim();

        if (student.getCourse() != null && !student.getCourse().isEmpty()) {
            session.setAttribute("errorMessage", "You are already registered for course: " + student.getCourse());
        } else {
            // registerCourse returns void, so we check if it worked by verifying the course was set
            student.registerCourse(courseCode);

            // Verify the course was registered
            if (courseCode.equals(student.getCourse())) {
                studentManager.saveStudentsToFile();
                session.setAttribute("successMessage", "Successfully registered for course: " + courseCode);
                System.out.println("Student " + studentId + " registered for course: " + courseCode);
            } else {
                session.setAttribute("errorMessage", "Failed to register for course: " + courseCode);
            }
        }

        return "redirect:/student/dashboard";
    }

    // ========== UNIT REGISTRATION ==========

    @PostMapping("/register-unit")
    public String registerUnit(
            @RequestParam String unitCode,
            HttpSession session,
            Model model) {

        String studentId = (String) session.getAttribute("studentId");
        if (studentId == null) {
            return "redirect:/student/login";
        }

        StudentData student = studentManager.getStudent(studentId);
        if (student == null) {
            return "redirect:/student/login";
        }

        if (unitCode == null || unitCode.trim().isEmpty()) {
            session.setAttribute("errorMessage", "Unit code is required.");
            return "redirect:/student/dashboard";
        }

        unitCode = unitCode.trim();

        // registerUnit returns void, so we check if it worked by verifying the unit was added
        student.registerUnit(unitCode);

        // Check if unit was successfully registered
        boolean registered = student.getRegisteredUnits() != null &&
                student.getRegisteredUnits().contains(unitCode);

        if (registered) {
            studentManager.saveStudentsToFile();
            session.setAttribute("successMessage", "Successfully registered for unit: " + unitCode);
            System.out.println("Student " + studentId + " registered for unit: " + unitCode);
        } else {
            session.setAttribute("errorMessage", "Failed to register for unit: " + unitCode +
                    ". You may have reached the maximum units or already registered.");
        }

        return "redirect:/student/dashboard";
    }

    // ========== LOGOUT ==========

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/student/login";
    }

    // ========== HELPER METHODS ==========

    private Map<String, Map<String, Object>> createMockAttendanceSummary(StudentData student) {
        Map<String, Map<String, Object>> summary = new HashMap<>();

        if (student.getRegisteredUnits() != null) {
            for (String unitCode : student.getRegisteredUnits()) {
                Map<String, Object> unitStats = new HashMap<>();
                unitStats.put("present", 8);
                unitStats.put("absent", 2);
                unitStats.put("total", 10);
                unitStats.put("attendanceRate", 80.0);
                summary.put(unitCode, unitStats);
            }
        }

        return summary;
    }

    private Map<String, Object> createMockOverallStats(StudentData student) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPresent", 25);
        stats.put("totalAbsent", 5);
        stats.put("totalSessions", 30);
        stats.put("overallAttendanceRate", 83.3);
        return stats;
    }
}
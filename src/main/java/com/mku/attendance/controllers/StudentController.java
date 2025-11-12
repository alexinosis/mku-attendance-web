package com.mku.attendance.controllers;

import com.mku.attendance.entities.StudentData;
import com.mku.attendance.services.StudentManager;
import com.mku.attendance.services.AuthService;
import com.mku.attendance.services.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/api/students")
@CrossOrigin(origins = "*") // Allow React frontend to connect
public class StudentController {

    @Autowired
    private StudentManager studentManager;

    @Autowired
    private AuthService authService;

    @Autowired
    private EmailService emailService;

    @Value("${app.email.enabled:true}")
    private boolean emailEnabled;

    // ========== STUDENT MANAGEMENT ENDPOINTS ==========

    /**
     * GET ALL STUDENTS - For React frontend student management
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllStudents() {
        System.out.println("📋 Fetching all students for React frontend");

        try {
            Map<String, StudentData> students = studentManager.getStudents();
            List<Map<String, Object>> studentList = students.values().stream()
                    .map(StudentData::toMap)
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("students", studentList);
            response.put("count", studentList.size());

            System.out.println("✅ Sent " + studentList.size() + " students to React frontend");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Error fetching students: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to fetch students"
            ));
        }
    }

    /**
     * GET STUDENT BY ID - For React frontend
     */
    @GetMapping("/{studentId}")
    public ResponseEntity<Map<String, Object>> getStudent(@PathVariable String studentId) {
        System.out.println("🔍 Fetching student: " + studentId);

        try {
            StudentData student = studentManager.getStudent(studentId);
            if (student == null) {
                return ResponseEntity.status(404).body(Map.of(
                        "success", false,
                        "message", "Student not found"
                ));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("student", student.toMap());

            System.out.println("✅ Student found: " + studentId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Error fetching student: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to fetch student"
            ));
        }
    }

    /**
     * CREATE NEW STUDENT - For React frontend student registration
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createStudent(@RequestBody Map<String, String> studentData) {
        System.out.println("🆕 Creating new student from React frontend");

        try {
            String studentId = studentData.get("student_id");
            String name = studentData.get("name");
            String email = studentData.get("email");
            String password = studentData.get("password");

            // Validate required fields
            if (studentId == null || studentId.trim().isEmpty() ||
                    name == null || name.trim().isEmpty() ||
                    email == null || email.trim().isEmpty() ||
                    password == null || password.trim().isEmpty()) {

                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "All fields are required: student_id, name, email, password"
                ));
            }

            // Clean inputs
            studentId = studentId.trim().toUpperCase();
            name = name.trim();
            email = email.trim().toLowerCase();
            password = password.trim();

            // Enhanced email validation
            System.out.println("🔍 Validating email: " + email);
            Map<String, Object> emailValidation = emailService.validateEmailWithDetails(email);
            System.out.println("🔍 Email validation details: " + emailValidation);

            boolean isEmailValid = emailService.validateEmailExistence(email);
            System.out.println("🔍 Email validation result: " + isEmailValid);

            if (!isEmailValid) {
                System.out.println("❌ Registration failed: Invalid email - " + email);

                // Provide specific error message based on validation details
                String errorMessage = "Invalid email address";
                if (Boolean.TRUE.equals(emailValidation.get("isDisposable"))) {
                    errorMessage = "Disposable/temporary email addresses are not allowed. Please use your permanent email.";
                } else if (!Boolean.TRUE.equals(emailValidation.get("isValidFormat"))) {
                    errorMessage = "Invalid email format. Please check and try again.";
                } else if (!Boolean.TRUE.equals(emailValidation.get("hasValidTld"))) {
                    errorMessage = "Email domain is not supported. Please use a valid email provider.";
                }

                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", errorMessage
                ));
            }

            // Check if student already exists
            if (studentManager.exists(studentId)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Student ID already exists"
                ));
            }

            // Check if email already registered
            if (authService.findStudentByEmail(email) != null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Email address already registered"
                ));
            }

            // Create new student
            StudentData student = new StudentData(studentId, name, email, password);

            // Validate student data
            if (!student.isValidForRegistration()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Invalid student data"
                ));
            }

            // Add student
            studentManager.addStudent(student);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Student created successfully");
            response.put("student", student.toMap());

            System.out.println("✅ Student created: " + studentId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Error creating student: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to create student: " + e.getMessage()
            ));
        }
    }

    /**
     * UPDATE STUDENT - For React frontend
     */
    @PutMapping("/{studentId}")
    public ResponseEntity<Map<String, Object>> updateStudent(
            @PathVariable String studentId,
            @RequestBody Map<String, String> studentData) {

        System.out.println("✏️ Updating student: " + studentId);

        try {
            StudentData existingStudent = studentManager.getStudent(studentId);
            if (existingStudent == null) {
                return ResponseEntity.status(404).body(Map.of(
                        "success", false,
                        "message", "Student not found"
                ));
            }

            // Update fields if provided
            if (studentData.containsKey("name")) {
                existingStudent.setName(studentData.get("name").trim());
            }
            if (studentData.containsKey("email")) {
                String newEmail = studentData.get("email").trim().toLowerCase();

                // Enhanced email validation for updates
                System.out.println("🔍 Validating email for update: " + newEmail);
                boolean isEmailValid = emailService.validateEmailExistence(newEmail);
                System.out.println("🔍 Email validation result: " + isEmailValid);

                if (!isEmailValid) {
                    System.out.println("❌ Update failed: Invalid email - " + newEmail);
                    return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "message", "Invalid email address. Please use a valid, non-disposable email."
                    ));
                }

                // Check if email is already used by another student
                StudentData studentWithEmail = authService.findStudentByEmail(newEmail);
                if (studentWithEmail != null && !studentWithEmail.getStudentId().equals(studentId)) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "message", "Email address already registered by another student"
                    ));
                }

                existingStudent.setEmail(newEmail);
            }
            if (studentData.containsKey("course")) {
                existingStudent.setCourse(studentData.get("course").trim());
            }

            // Update student
            boolean updated = studentManager.updateStudent(existingStudent);
            if (!updated) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Failed to update student"
                ));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Student updated successfully");
            response.put("student", existingStudent.toMap());

            System.out.println("✅ Student updated: " + studentId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Error updating student: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to update student"
            ));
        }
    }

    /**
     * DELETE STUDENT - For React frontend
     */
    @DeleteMapping("/{studentId}")
    public ResponseEntity<Map<String, Object>> deleteStudent(@PathVariable String studentId) {
        System.out.println("🗑️ Deleting student: " + studentId);

        try {
            boolean deleted = studentManager.removeStudent(studentId);
            if (!deleted) {
                return ResponseEntity.status(404).body(Map.of(
                        "success", false,
                        "message", "Student not found"
                ));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Student deleted successfully");

            System.out.println("✅ Student deleted: " + studentId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Error deleting student: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to delete student"
            ));
        }
    }

    // ========== AUTHENTICATION ENDPOINTS ==========

    /**
     * STUDENT LOGIN - For React frontend (CHANGED ENDPOINT TO AVOID CONFLICT)
     */
    @PostMapping("/auth/login")
    public ResponseEntity<Map<String, Object>> loginStudent(@RequestBody Map<String, String> loginData) {
        String studentId = loginData.get("student_id");
        String password = loginData.get("password");

        System.out.println("🔐 Student login attempt via API: " + studentId);

        Map<String, Object> result = authService.loginStudent(studentId, password);
        return ResponseEntity.ok(result);
    }

    /**
     * INITIATE PASSWORD RESET - For React frontend (CHANGED ENDPOINT TO AVOID CONFLICT)
     */
    @PostMapping("/auth/password-reset/initiate")
    public ResponseEntity<Map<String, Object>> initiatePasswordReset(@RequestBody Map<String, String> resetData) {
        String studentId = resetData.get("student_id");
        String email = resetData.get("email");

        System.out.println("🔐 Password reset initiated via API: " + studentId);

        Map<String, Object> result = authService.initiatePasswordReset(studentId, email);
        return ResponseEntity.ok(result);
    }

    /**
     * VERIFY OTP AND RESET PASSWORD - For React frontend (CHANGED ENDPOINT TO AVOID CONFLICT)
     */
    @PostMapping("/auth/password-reset/verify")
    public ResponseEntity<Map<String, Object>> verifyOTPAndResetPassword(@RequestBody Map<String, String> resetData) {
        String studentId = resetData.get("student_id");
        String otp = resetData.get("otp");
        String newPassword = resetData.get("new_password");

        System.out.println("🔐 OTP verification via API: " + studentId);

        Map<String, Object> result = authService.verifyOTPAndResetPassword(studentId, otp, newPassword);
        return ResponseEntity.ok(result);
    }

    /**
     * RESEND OTP - For React frontend (CHANGED ENDPOINT TO AVOID CONFLICT)
     */
    @PostMapping("/auth/password-reset/resend")
    public ResponseEntity<Map<String, Object>> resendOTP(@RequestBody Map<String, String> resetData) {
        String studentId = resetData.get("student_id");

        System.out.println("🔄 Resending OTP via API: " + studentId);

        Map<String, Object> result = authService.resendOTP(studentId);
        return ResponseEntity.ok(result);
    }

    // ========== ATTENDANCE ENDPOINTS ==========

    /**
     * GET ATTENDANCE SUMMARY - For React frontend
     */
    @GetMapping("/{studentId}/attendance")
    public ResponseEntity<Map<String, Object>> getAttendanceSummary(@PathVariable String studentId) {
        System.out.println("📊 Fetching attendance summary for: " + studentId);

        try {
            Map<String, Map<String, Object>> attendanceSummary = studentManager.getAttendanceSummary(studentId);
            Map<String, Object> overallStats = studentManager.getOverallStatistics(studentId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("attendanceSummary", attendanceSummary);
            response.put("overallStats", overallStats);

            System.out.println("✅ Attendance summary sent for: " + studentId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Error fetching attendance summary: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to fetch attendance summary"
            ));
        }
    }

    /**
     * MARK ATTENDANCE - For React frontend
     */
    @PostMapping("/{studentId}/attendance")
    public ResponseEntity<Map<String, Object>> markAttendance(
            @PathVariable String studentId,
            @RequestBody Map<String, String> attendanceData) {

        String unitCode = attendanceData.get("unit_code");
        boolean present = Boolean.parseBoolean(attendanceData.get("present"));

        System.out.println("📝 Marking attendance for " + studentId + " in " + unitCode + ": " + (present ? "PRESENT" : "ABSENT"));

        try {
            studentManager.markAttendance(studentId, unitCode, present);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Attendance marked successfully");

            System.out.println("✅ Attendance marked for: " + studentId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Error marking attendance: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to mark attendance"
            ));
        }
    }

    // ========== UNIT REGISTRATION ENDPOINTS ==========

    /**
     * REGISTER UNIT - For React frontend
     */
    @PostMapping("/{studentId}/units")
    public ResponseEntity<Map<String, Object>> registerUnit(
            @PathVariable String studentId,
            @RequestBody Map<String, String> unitData) {

        String unitCode = unitData.get("unit_code");

        System.out.println("📚 Registering unit " + unitCode + " for " + studentId);

        try {
            boolean registered = studentManager.registerUnitForStudent(studentId, unitCode);

            Map<String, Object> response = new HashMap<>();
            if (registered) {
                response.put("success", true);
                response.put("message", "Unit registered successfully");
                System.out.println("✅ Unit registered: " + unitCode + " for " + studentId);
            } else {
                response.put("success", false);
                response.put("message", "Failed to register unit");
                System.out.println("❌ Unit registration failed: " + unitCode + " for " + studentId);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Error registering unit: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to register unit"
            ));
        }
    }

    /**
     * REMOVE UNIT - For React frontend
     */
    @DeleteMapping("/{studentId}/units/{unitCode}")
    public ResponseEntity<Map<String, Object>> removeUnit(
            @PathVariable String studentId,
            @PathVariable String unitCode) {

        System.out.println("📚 Removing unit " + unitCode + " from " + studentId);

        try {
            boolean removed = studentManager.removeUnitFromStudent(studentId, unitCode);

            Map<String, Object> response = new HashMap<>();
            if (removed) {
                response.put("success", true);
                response.put("message", "Unit removed successfully");
                System.out.println("✅ Unit removed: " + unitCode + " from " + studentId);
            } else {
                response.put("success", false);
                response.put("message", "Failed to remove unit");
                System.out.println("❌ Unit removal failed: " + unitCode + " from " + studentId);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Error removing unit: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to remove unit"
            ));
        }
    }

    // ========== HEALTH CHECK ==========

    /**
     * HEALTH CHECK - For React frontend
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Student API is running");
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        response.put("studentCount", studentManager.getTotalStudentCount());

        return ResponseEntity.ok(response);
    }

    /**
     * NEW: Email validation endpoint for testing
     */
    @PostMapping("/validate-email")
    public ResponseEntity<Map<String, Object>> validateEmail(@RequestBody Map<String, String> emailData) {
        String email = emailData.get("email");

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Email is required"
            ));
        }

        Map<String, Object> validationResult = emailService.validateEmailWithDetails(email);
        validationResult.put("success", true);

        return ResponseEntity.ok(validationResult);
    }
}
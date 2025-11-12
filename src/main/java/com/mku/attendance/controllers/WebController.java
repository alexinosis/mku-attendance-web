package com.mku.attendance.controllers;

import com.mku.attendance.services.AuthService;
import com.mku.attendance.services.StudentManager;
import com.mku.attendance.services.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Controller
public class WebController {

    @Autowired
    private AuthService authService;

    @Autowired
    private StudentManager studentManager;

    @Autowired
    private EmailService emailService;

    // ========== PAGE MAPPINGS ==========

    @GetMapping("/student/login")
    public String studentLoginPage(@RequestParam(value = "error", required = false) String error,
                                   @RequestParam(value = "success", required = false) String success,
                                   Model model) {
        if (error != null) {
            model.addAttribute("error", error);
        }
        if (success != null) {
            model.addAttribute("success", success);
        }
        return "student-login";
    }

    @GetMapping("/student/forgot-password")
    public String forgotPasswordPage(@RequestParam(value = "error", required = false) String error,
                                     @RequestParam(value = "success", required = false) String success,
                                     @RequestParam(value = "studentId", required = false) String studentId,
                                     @RequestParam(value = "showOTPForm", required = false) Boolean showOTPForm,
                                     @RequestParam(value = "showPasswordForm", required = false) Boolean showPasswordForm,
                                     Model model) {
        if (error != null) {
            model.addAttribute("error", error);
        }
        if (success != null) {
            model.addAttribute("success", success);
        }
        if (studentId != null) {
            model.addAttribute("studentId", studentId);
        }
        if (showOTPForm != null && showOTPForm) {
            model.addAttribute("showOTPForm", true);
        }
        if (showPasswordForm != null && showPasswordForm) {
            model.addAttribute("showPasswordForm", true);
        }
        return "forgot-password";
    }

    @GetMapping("/student/register")
    public String registerPage(@RequestParam(value = "error", required = false) String error,
                               @RequestParam(value = "success", required = false) String success,
                               Model model) {
        if (error != null) {
            model.addAttribute("error", error);
        }
        if (success != null) {
            model.addAttribute("success", success);
        }
        return "student-register";
    }

    @GetMapping("/student/dashboard")
    public String studentDashboard(@RequestParam(value = "studentId", required = false) String studentId,
                                   @RequestParam(value = "name", required = false) String name,
                                   Model model) {
        if (studentId == null) {
            return "redirect:/student/login?error=Please login to access dashboard";
        }

        var student = studentManager.getStudent(studentId);
        if (student == null) {
            return "redirect:/student/login?error=Student not found";
        }

        model.addAttribute("student", student);
        model.addAttribute("attendanceSummary", studentManager.getAttendanceSummary(studentId));
        model.addAttribute("overallStats", studentManager.getOverallStatistics(studentId));

        // ADD MISSING ATTRIBUTES TO PREVENT NULL ERRORS
        model.addAttribute("canTakeAttendance", false);
        model.addAttribute("activeUnitCode", "");
        // Remove the problematic method calls - use empty maps instead
        model.addAttribute("courses", new java.util.HashMap<>());
        model.addAttribute("units", new java.util.HashMap<>());

        return "student-dashboard";
    }

    // ========== FORM HANDLERS ==========

    @PostMapping("/student/login")
    public String handleStudentLogin(@RequestParam String studentId,
                                     @RequestParam String password,
                                     Model model) {

        System.out.println("🌐 Processing web login for student: " + studentId);

        Map<String, Object> result = authService.loginStudentForWeb(studentId, password);

        if (Boolean.TRUE.equals(result.get("success"))) {
            String redirectUrl = "/student/dashboard?studentId=" + studentId;
            // Keep the name parameter but URL encode it to handle spaces
            if (result.containsKey("studentName")) {
                String studentName = (String) result.get("studentName");
                redirectUrl += "&name=" + java.net.URLEncoder.encode(studentName, java.nio.charset.StandardCharsets.UTF_8);
            }
            System.out.println("✅ Login successful, redirecting to: " + redirectUrl);
            return "redirect:" + redirectUrl;
        } else {
            String error = (String) result.get("error");
            System.out.println("❌ Login failed: " + error);
            model.addAttribute("error", error);
            model.addAttribute("studentId", studentId);
            return "student-login";
        }
    }

    @PostMapping("/student/register")
    public String handleStudentRegistration(@RequestParam String studentId,
                                            @RequestParam String name,
                                            @RequestParam String email,
                                            @RequestParam String password,
                                            @RequestParam(required = false) String course,
                                            Model model) {

        System.out.println("\n🌐 ===== WEB REGISTRATION ATTEMPT =====");
        System.out.println("🔍 Student ID: " + studentId);
        System.out.println("🔍 Name: " + name);
        System.out.println("🔍 Email: " + email);

        try {
            // Clean inputs
            studentId = studentId.trim().toUpperCase();
            name = name.trim();
            email = email.trim().toLowerCase();
            password = password.trim();

            // ===== ENHANCED EMAIL VALIDATION =====
            System.out.println("🔍 Validating email: " + email);
            Map<String, Object> emailValidation = emailService.validateEmailWithDetails(email);
            System.out.println("🔍 Email validation details: " + emailValidation);

            boolean isEmailValid = emailService.validateEmailExistence(email);
            System.out.println("🔍 Email validation result: " + isEmailValid);

            if (!isEmailValid) {
                System.out.println("❌ Web registration failed: Invalid email - " + email);

                String errorMessage = "Invalid email address";
                if (Boolean.TRUE.equals(emailValidation.get("isDisposable"))) {
                    errorMessage = "Disposable/temporary email addresses are not allowed. Please use your permanent email.";
                } else if (!Boolean.TRUE.equals(emailValidation.get("isValidFormat"))) {
                    errorMessage = "Invalid email format. Please check and try again.";
                } else if (!Boolean.TRUE.equals(emailValidation.get("hasValidTld"))) {
                    errorMessage = "Email domain is not supported. Please use a valid email provider.";
                }

                model.addAttribute("error", errorMessage);
                model.addAttribute("studentId", studentId);
                model.addAttribute("name", name);
                model.addAttribute("email", email);
                model.addAttribute("course", course);
                return "student-register";
            }

            // Check if student already exists
            if (studentManager.exists(studentId)) {
                System.out.println("❌ Web registration failed: Student ID already exists");
                model.addAttribute("error", "Student ID already exists");
                model.addAttribute("studentId", studentId);
                model.addAttribute("name", name);
                model.addAttribute("email", email);
                model.addAttribute("course", course);
                return "student-register";
            }

            // Check if email already registered
            if (authService.findStudentByEmail(email) != null) {
                System.out.println("❌ Web registration failed: Email already registered");
                model.addAttribute("error", "Email address already registered");
                model.addAttribute("studentId", studentId);
                model.addAttribute("name", name);
                model.addAttribute("email", email);
                model.addAttribute("course", course);
                return "student-register";
            }

            var student = new com.mku.attendance.entities.StudentData(studentId, name, email, password);
            if (course != null && !course.trim().isEmpty()) {
                student.setCourse(course.trim());
            }

            if (!student.isValidForRegistration()) {
                System.out.println("❌ Web registration failed: Invalid student data");
                model.addAttribute("error", "All fields are required and must be valid");
                model.addAttribute("studentId", studentId);
                model.addAttribute("name", name);
                model.addAttribute("email", email);
                model.addAttribute("course", course);
                return "student-register";
            }

            studentManager.addStudent(student);
            System.out.println("✅ Student registered successfully: " + studentId);

            return "redirect:/student/login?success=Registration successful! Please login with your credentials";

        } catch (Exception e) {
            System.err.println("❌ Registration error: " + e.getMessage());
            model.addAttribute("error", "Registration failed: " + e.getMessage());
            model.addAttribute("studentId", studentId);
            model.addAttribute("name", name);
            model.addAttribute("email", email);
            model.addAttribute("course", course);
            return "student-register";
        }
    }

    @PostMapping("/student/forgot-password")
    public String handleForgotPassword(@RequestParam String studentId,
                                       @RequestParam String email,
                                       Model model) {

        System.out.println("🌐 Processing forgot password for: " + studentId);

        // Enhanced email validation for password reset
        System.out.println("🔍 Validating email for password reset: " + email);
        boolean isEmailValid = emailService.validateEmailExistence(email);
        System.out.println("🔍 Email validation result: " + isEmailValid);

        if (!isEmailValid) {
            System.out.println("❌ Password reset failed: Invalid email - " + email);
            Map<String, Object> emailValidation = emailService.validateEmailWithDetails(email);

            String errorMessage = "Invalid email address";
            if (Boolean.TRUE.equals(emailValidation.get("isDisposable"))) {
                errorMessage = "Disposable/temporary email addresses are not allowed. Please use your permanent email.";
            } else if (!Boolean.TRUE.equals(emailValidation.get("isValidFormat"))) {
                errorMessage = "Invalid email format. Please check and try again.";
            }

            model.addAttribute("error", errorMessage);
            model.addAttribute("studentId", studentId);
            model.addAttribute("email", email);
            return "forgot-password";
        }

        Map<String, Object> result = authService.initiatePasswordReset(studentId, email);

        if (Boolean.TRUE.equals(result.get("success"))) {
            String message = (String) result.get("message");
            String maskedEmail = (String) result.get("email");

            // Show OTP form after successful OTP generation
            model.addAttribute("success", message + " (Email: " + maskedEmail + ")");
            model.addAttribute("studentId", studentId);
            model.addAttribute("showOTPForm", true); // Show OTP input form

            return "forgot-password";
        } else {
            String error = (String) result.get("message");
            model.addAttribute("error", error);
            model.addAttribute("studentId", studentId);
            model.addAttribute("email", email);
            return "forgot-password";
        }
    }

    // ========== OTP VERIFICATION & PASSWORD RESET ==========

    @PostMapping("/student/verify-otp")
    public String handleOTPVerification(@RequestParam String studentId,
                                        @RequestParam String otp,
                                        Model model) {

        System.out.println("🔐 OTP verification attempt for: " + studentId + ", OTP: " + otp);

        // Verify OTP without resetting password yet
        Map<String, Object> result = authService.verifyOTP(studentId, otp);

        if (Boolean.TRUE.equals(result.get("success"))) {
            // OTP verified successfully, show password reset form
            model.addAttribute("success", "OTP verified successfully! Please set your new password.");
            model.addAttribute("studentId", studentId);
            model.addAttribute("showPasswordForm", true);
            System.out.println("✅ OTP verified for student: " + studentId);
        } else {
            // OTP verification failed
            String error = (String) result.get("message");
            model.addAttribute("error", error);
            model.addAttribute("studentId", studentId);
            model.addAttribute("showOTPForm", true);
            System.out.println("❌ OTP verification failed for student: " + studentId);
        }

        return "forgot-password";
    }

    @PostMapping("/student/reset-password")
    public String handlePasswordReset(@RequestParam String studentId,
                                      @RequestParam String newPassword,
                                      @RequestParam String confirmPassword,
                                      Model model) {

        System.out.println("🔄 Password reset attempt for: " + studentId);

        // Validate passwords match
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match. Please try again.");
            model.addAttribute("studentId", studentId);
            model.addAttribute("showPasswordForm", true);
            return "forgot-password";
        }

        // Validate password length
        if (newPassword.length() < 6) {
            model.addAttribute("error", "Password must be at least 6 characters long.");
            model.addAttribute("studentId", studentId);
            model.addAttribute("showPasswordForm", true);
            return "forgot-password";
        }

        // Reset password using AuthService
        Map<String, Object> result = authService.resetPasswordWithOTP(studentId, newPassword);

        if (Boolean.TRUE.equals(result.get("success"))) {
            String successMessage = (String) result.get("message");
            System.out.println("✅ Password reset successful for student: " + studentId);
            return "redirect:/student/login?success=" + successMessage;
        } else {
            String error = (String) result.get("message");
            model.addAttribute("error", error);
            model.addAttribute("studentId", studentId);
            model.addAttribute("showPasswordForm", true);
            System.out.println("❌ Password reset failed for student: " + studentId);
            return "forgot-password";
        }
    }

    @PostMapping("/student/resend-otp")
    public String handleResendOTP(@RequestParam String studentId,
                                  Model model) {

        System.out.println("🔄 Resending OTP for: " + studentId);

        Map<String, Object> result = authService.resendOTP(studentId);

        if (Boolean.TRUE.equals(result.get("success"))) {
            String message = (String) result.get("message");
            String maskedEmail = (String) result.get("email");
            model.addAttribute("success", message + " (Email: " + maskedEmail + ")");
            model.addAttribute("studentId", studentId);
            model.addAttribute("showOTPForm", true);
            System.out.println("✅ OTP resent successfully for student: " + studentId);
        } else {
            String error = (String) result.get("message");
            model.addAttribute("error", error);
            model.addAttribute("studentId", studentId);
            model.addAttribute("showOTPForm", true);
            System.out.println("❌ OTP resend failed for student: " + studentId);
        }

        return "forgot-password";
    }

    @GetMapping("/student/logout")
    public String handleLogout() {
        return "redirect:/student/login?success=Logged out successfully";
    }

    // ========== ERROR PAGES ==========

    @GetMapping("/error")
    public String errorPage(@RequestParam(value = "message", required = false) String message,
                            @RequestParam(value = "name", required = false) String name, // ADDED: Handle name parameter
                            Model model) {
        System.out.println("🔧 Error page accessed - message: " + message + ", name: " + name);

        if (message != null) {
            model.addAttribute("error", message);
        }

        // Handle name parameter to prevent the "required parameter 'name' not present" error
        if (name != null) {
            System.out.println("📝 Error page received name parameter: " + name);
            model.addAttribute("name", name);
        }

        return "error";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }

    // ========== HEALTH CHECK ==========

    @GetMapping("/status")
    public String statusPage(Model model) {
        model.addAttribute("totalStudents", studentManager.getTotalStudentCount());
        model.addAttribute("systemStatus", "Operational");
        model.addAttribute("timestamp", java.time.LocalDateTime.now().toString());
        return "status";
    }

    // ========== EMAIL VALIDATION TEST ENDPOINT ==========

    @GetMapping("/validate-email-test")
    public String emailValidationTestPage() {
        return "email-validation-test";
    }

    @PostMapping("/validate-email-test")
    public String handleEmailValidationTest(@RequestParam String testEmail, Model model) {
        System.out.println("🧪 Testing email validation for: " + testEmail);

        Map<String, Object> validationResult = emailService.validateEmailWithDetails(testEmail);
        model.addAttribute("validationResult", validationResult);
        model.addAttribute("testEmail", testEmail);

        System.out.println("🧪 Validation result: " + validationResult);

        return "email-validation-test";
    }
}
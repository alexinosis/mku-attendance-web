package com.mku.attendance.controllers;

import com.mku.attendance.services.LecturerAuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@Controller
@RequestMapping("/lecturer")
public class LecturerAuthController {

    @Autowired
    private LecturerAuthService lecturerAuthService;

    /**
     * Show lecturer login form
     */
    @GetMapping("/login")
    public String showLoginForm() {
        return "lecturer-login";
    }

    /**
     * Process lecturer login
     */
    @PostMapping("/login")
    public String loginLecturer(
            @RequestParam String lecturerId,
            @RequestParam String password,
            HttpSession session,
            Model model) {

        System.out.println("Lecturer login attempt for: " + lecturerId);

        if (lecturerId == null || lecturerId.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            model.addAttribute("error", "Enter lecturer ID and password.");
            return "lecturer-login";
        }

        // Use LecturerAuthService for login
        Map<String, Object> loginResult = lecturerAuthService.loginLecturerForWeb(lecturerId.trim(), password.trim());

        if (Boolean.TRUE.equals(loginResult.get("success"))) {
            // Store lecturer in session
            session.setAttribute("lecturerId", lecturerId.toUpperCase());
            session.setAttribute("lecturerName", loginResult.get("lecturerName"));

            System.out.println("Lecturer login successful: " + lecturerId);
            return "redirect:/lecturer/dashboard";
        } else {
            model.addAttribute("error", loginResult.get("error"));
            return "lecturer-login";
        }
    }

    /**
     * Show forgot password form
     */
    @GetMapping("/forgot-password")
    public String showForgotPassword() {
        return "lecturer-forgot-password";
    }

    /**
     * Initiate password reset process
     */
    @PostMapping("/forgot-password")
    public String initiatePasswordReset(
            @RequestParam String lecturerId,
            @RequestParam String email,
            Model model) {

        System.out.println("Password reset initiated for lecturer: " + lecturerId);

        if (lecturerId == null || lecturerId.trim().isEmpty()) {
            model.addAttribute("error", "Please enter your Lecturer ID.");
            return "lecturer-forgot-password";
        }

        if (email == null || email.trim().isEmpty()) {
            model.addAttribute("error", "Please enter your registered email address.");
            return "lecturer-forgot-password";
        }

        // Use LecturerAuthService for password reset initiation
        Map<String, Object> resetResult = lecturerAuthService.initiatePasswordReset(
                lecturerId.trim(), email.trim()
        );

        if (Boolean.TRUE.equals(resetResult.get("success"))) {
            model.addAttribute("emailSent", true);
            model.addAttribute("lecturerId", lecturerId);
            model.addAttribute("email", email);
            model.addAttribute("lecturerName", resetResult.get("lecturerName"));
            model.addAttribute("message", resetResult.get("message"));

            // If email failed, show OTP in the model (for development/testing)
            if (Boolean.TRUE.equals(resetResult.get("emailFailed"))) {
                model.addAttribute("otp", resetResult.get("otp"));
            }
        } else {
            model.addAttribute("error", resetResult.get("message"));
        }

        return "lecturer-forgot-password";
    }

    /**
     * Verify OTP (separate from password reset)
     */
    @PostMapping("/verify-otp")
    public String verifyOTP(
            @RequestParam String lecturerId,
            @RequestParam String otp,
            Model model) {

        System.out.println("OTP verification for lecturer: " + lecturerId);

        if (lecturerId == null || lecturerId.trim().isEmpty()) {
            model.addAttribute("error", "Lecturer ID required.");
            model.addAttribute("emailSent", true);
            model.addAttribute("lecturerId", lecturerId);
            return "lecturer-forgot-password";
        }

        if (otp == null || otp.trim().isEmpty()) {
            model.addAttribute("error", "OTP required.");
            model.addAttribute("emailSent", true);
            model.addAttribute("lecturerId", lecturerId);
            return "lecturer-forgot-password";
        }

        // Use LecturerAuthService for OTP verification only
        Map<String, Object> verificationResult = lecturerAuthService.verifyOTP(
                lecturerId.trim(), otp.trim()
        );

        if (Boolean.TRUE.equals(verificationResult.get("success"))) {
            // OTP verified successfully, show password reset form
            model.addAttribute("showResetForm", true);
            model.addAttribute("lecturerId", lecturerId);
            model.addAttribute("otp", otp); // Keep OTP for the reset step
            model.addAttribute("success", "OTP verified successfully. Please set your new password.");
            model.addAttribute("lecturerName", verificationResult.get("lecturerName"));
        } else {
            model.addAttribute("error", verificationResult.get("message"));
            model.addAttribute("emailSent", true);
            model.addAttribute("lecturerId", lecturerId);
        }

        return "lecturer-forgot-password";
    }

    /**
     * Resend OTP
     */
    @PostMapping("/resend-otp")
    public String resendOTP(
            @RequestParam String lecturerId,
            Model model) {

        System.out.println("Resending OTP for lecturer: " + lecturerId);

        if (lecturerId == null || lecturerId.trim().isEmpty()) {
            model.addAttribute("error", "Lecturer ID required.");
            return "lecturer-forgot-password";
        }

        // Use LecturerAuthService to resend OTP
        Map<String, Object> resendResult = lecturerAuthService.resendOTP(lecturerId.trim());

        if (Boolean.TRUE.equals(resendResult.get("success"))) {
            model.addAttribute("emailSent", true);
            model.addAttribute("lecturerId", lecturerId);
            model.addAttribute("message", resendResult.get("message"));

            // If email failed, show OTP in the model (for development/testing)
            if (Boolean.TRUE.equals(resendResult.get("emailFailed"))) {
                model.addAttribute("otp", resendResult.get("otp"));
            }
        } else {
            model.addAttribute("error", resendResult.get("message"));
        }

        return "lecturer-forgot-password";
    }

    /**
     * Reset password after OTP verification
     */
    @PostMapping("/reset-password")
    public String resetPassword(
            @RequestParam String lecturerId,
            @RequestParam String otp,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            Model model) {

        System.out.println("Password reset for lecturer: " + lecturerId);

        // Validation
        if (lecturerId == null || lecturerId.trim().isEmpty()) {
            model.addAttribute("error", "Lecturer ID required.");
            model.addAttribute("showResetForm", true);
            model.addAttribute("lecturerId", lecturerId);
            model.addAttribute("otp", otp);
            return "lecturer-forgot-password";
        }

        if (otp == null || otp.trim().isEmpty()) {
            model.addAttribute("error", "OTP required.");
            model.addAttribute("showResetForm", true);
            model.addAttribute("lecturerId", lecturerId);
            model.addAttribute("otp", otp);
            return "lecturer-forgot-password";
        }

        if (newPassword == null || newPassword.trim().isEmpty()) {
            model.addAttribute("error", "New password required.");
            model.addAttribute("showResetForm", true);
            model.addAttribute("lecturerId", lecturerId);
            model.addAttribute("otp", otp);
            return "lecturer-forgot-password";
        }

        if (newPassword.length() < 6) {
            model.addAttribute("error", "Password must be at least 6 characters long.");
            model.addAttribute("showResetForm", true);
            model.addAttribute("lecturerId", lecturerId);
            model.addAttribute("otp", otp);
            return "lecturer-forgot-password";
        }

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match.");
            model.addAttribute("showResetForm", true);
            model.addAttribute("lecturerId", lecturerId);
            model.addAttribute("otp", otp);
            return "lecturer-forgot-password";
        }

        // Use LecturerAuthService for final password reset
        Map<String, Object> resetResult = lecturerAuthService.resetPassword(
                lecturerId.trim(), otp.trim(), newPassword.trim()
        );

        if (Boolean.TRUE.equals(resetResult.get("success"))) {
            model.addAttribute("success", "Password reset successfully! You can now login with your new password.");
            return "lecturer-login";
        } else {
            model.addAttribute("error", resetResult.get("message"));
            model.addAttribute("showResetForm", true);
            model.addAttribute("lecturerId", lecturerId);
            model.addAttribute("otp", otp);
            return "lecturer-forgot-password";
        }
    }

    /**
     * Logout lecturer
     */
    @GetMapping("/logout")
    public String logoutLecturer(HttpSession session) {
        session.removeAttribute("lecturerId");
        session.removeAttribute("lecturerName");
        session.invalidate();
        return "redirect:/lecturer/login";
    }
}
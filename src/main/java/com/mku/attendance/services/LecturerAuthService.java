package com.mku.attendance.services;

import com.mku.attendance.entities.LecturerData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Random;

@Service
public class LecturerAuthService {

    @Autowired
    private HODManager hodManager;

    @Autowired
    private EmailService emailService;

    // Simple OTP storage - lecturerId -> OTP data
    private final Map<String, Map<String, Object>> otpStore = new ConcurrentHashMap<>();
    private final Random random = new Random();

    /**
     * Simple OTP Generator
     */
    private String generateOTP() {
        return String.format("%06d", random.nextInt(999999));
    }

    /**
     * Simple email validation
     */
    private boolean isValidEmail(String email) {
        return email != null && email.contains("@") && email.contains(".");
    }

    /**
     * Find lecturer by ID using your HODManager's actual methods
     */
    private LecturerData findLecturerById(String lecturerId) {
        return hodManager.getLecturer(lecturerId);
    }

    /**
     * Save lecturers using HODManager's method
     */
    private boolean saveLecturers() {
        try {
            hodManager.saveLecturersToFile();
            return true;
        } catch (Exception e) {
            System.out.println("❌ Error saving lecturers: " + e.getMessage());
            return false;
        }
    }

    /**
     * Initiate password reset process for lecturer
     */
    public Map<String, Object> initiatePasswordReset(String lecturerId, String email) {
        Map<String, Object> result = new HashMap<>();

        System.out.println("\n🚀 ===== LECTURER PASSWORD RESET INITIATED =====");
        System.out.println("🔍 Lecturer ID: " + lecturerId);
        System.out.println("📧 Email: " + email);

        // Simple email validation
        boolean isValidEmail = isValidEmail(email);
        System.out.println("🔍 Email validation result: " + isValidEmail);

        if (!isValidEmail) {
            result.put("success", false);
            result.put("message", "Invalid email address format.");
            return result;
        }

        // Find lecturer
        LecturerData lecturer = findLecturerById(lecturerId);
        if (lecturer == null) {
            System.out.println("❌ Lecturer not found: " + lecturerId);
            result.put("success", false);
            result.put("message", "Lecturer ID not found.");
            return result;
        }

        String lecturerName = lecturer.getName();
        String registeredEmail = lecturer.getEmail();

        System.out.println("✅ Lecturer found: " + lecturerName + " (" + lecturerId + ")");

        // Check if lecturer email matches
        if (registeredEmail == null || !registeredEmail.equalsIgnoreCase(email)) {
            System.out.println("❌ Email mismatch. Registered: " + registeredEmail + ", Provided: " + email);
            result.put("success", false);
            result.put("message", "Email does not match the registered email for this lecturer ID.");
            return result;
        }

        // Generate OTP
        String otp = generateOTP();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(10);

        // Store OTP data
        Map<String, Object> otpData = new HashMap<>();
        otpData.put("otp", otp);
        otpData.put("createdAt", now);
        otpData.put("expiresAt", expiresAt);
        otpData.put("lecturerId", lecturerId);
        otpData.put("lecturerName", lecturerName);
        otpData.put("email", email);
        otpData.put("verified", false);
        otpData.put("attempts", 0);

        otpStore.put(lecturerId, otpData);

        System.out.println("\n🎯 ===== LECTURER OTP GENERATED =====");
        System.out.println("🎯 Lecturer ID: " + lecturerId);
        System.out.println("🎯 Lecturer Name: " + lecturerName);
        System.out.println("🎯 Email: " + email);
        System.out.println("🎯 OTP CODE: " + otp);
        System.out.println("🎯 Expires: " + expiresAt);
        System.out.println("🎯 ================================\n");

        // Send OTP email using REAL EmailService
        boolean emailSent = false;
        String emailError = null;

        try {
            System.out.println("📧 Attempting to send OTP email to: " + email);
            emailService.sendOTPEmail(email, lecturerName, otp);
            emailSent = true;
            System.out.println("✅ OTP email sent successfully to: " + email);
        } catch (Exception e) {
            emailError = e.getMessage();
            System.out.println("❌ Failed to send OTP email: " + emailError);
        }

        if (emailSent) {
            result.put("success", true);
            result.put("message", "OTP sent successfully to your registered email address.");
            result.put("lecturerName", lecturerName);
            result.put("emailSent", true);
        } else {
            result.put("success", true);
            result.put("message", "OTP generated but email delivery failed. Use OTP: " + otp);
            result.put("lecturerName", lecturerName);
            result.put("emailSent", false);
            result.put("emailFailed", true);
            result.put("otp", otp);
        }

        System.out.println("✅ Password reset initiated successfully for lecturer: " + lecturerId);
        return result;
    }

    /**
     * Verify OTP for lecturer
     */
    public Map<String, Object> verifyOTP(String lecturerId, String otp) {
        Map<String, Object> result = new HashMap<>();

        System.out.println("\n🔐 ===== LECTURER OTP VERIFICATION ATTEMPT =====");
        System.out.println("🔍 Lecturer: " + lecturerId);
        System.out.println("🔍 OTP Provided: " + otp);

        // Find lecturer
        LecturerData lecturer = findLecturerById(lecturerId);
        if (lecturer == null) {
            System.out.println("❌ Lecturer not found: " + lecturerId);
            result.put("success", false);
            result.put("message", "Lecturer ID not found.");
            return result;
        }

        // Get OTP data
        Map<String, Object> otpData = otpStore.get(lecturerId);
        if (otpData == null) {
            System.out.println("❌ OTP verification failed: No OTP found for lecturer");
            result.put("success", false);
            result.put("message", "No OTP found. Please request a new OTP.");
            return result;
        }

        // Check if OTP is expired
        LocalDateTime expiresAt = (LocalDateTime) otpData.get("expiresAt");
        if (LocalDateTime.now().isAfter(expiresAt)) {
            System.out.println("❌ OTP verification failed: OTP expired");
            otpStore.remove(lecturerId);
            result.put("success", false);
            result.put("message", "OTP has expired. Please request a new OTP.");
            return result;
        }

        // Check attempts
        int attempts = (int) otpData.get("attempts");
        attempts++;
        otpData.put("attempts", attempts);

        System.out.println("🔍 Attempt: " + attempts + "/3");

        if (attempts > 3) {
            System.out.println("❌ OTP verification failed: Too many attempts");
            otpStore.remove(lecturerId);
            result.put("success", false);
            result.put("message", "Too many failed attempts. Please request a new OTP.");
            return result;
        }

        // Verify OTP
        String expectedOtp = (String) otpData.get("otp");
        if (!expectedOtp.equals(otp)) {
            System.out.println("❌ OTP verification failed: Invalid OTP");
            System.out.println("⚠️ Remaining attempts: " + (3 - attempts));
            result.put("success", false);
            result.put("message", "Invalid OTP. " + (3 - attempts) + " attempts remaining.");
            return result;
        }

        System.out.println("✅ OTP verification successful!");

        // Mark OTP as verified but don't remove it yet
        otpData.put("verified", true);
        result.put("success", true);
        result.put("message", "OTP verified successfully.");
        result.put("lecturerName", otpData.get("lecturerName"));

        return result;
    }

    /**
     * Reset password after OTP verification
     */
    public Map<String, Object> resetPassword(String lecturerId, String otp, String newPassword) {
        Map<String, Object> result = new HashMap<>();

        System.out.println("\n🔑 ===== LECTURER PASSWORD RESET ATTEMPT =====");
        System.out.println("🔍 Lecturer: " + lecturerId);
        System.out.println("🔍 OTP Provided: " + otp);

        // Find lecturer
        LecturerData lecturer = findLecturerById(lecturerId);
        if (lecturer == null) {
            System.out.println("❌ Lecturer not found: " + lecturerId);
            result.put("success", false);
            result.put("message", "Lecturer ID not found.");
            return result;
        }

        // Get OTP data
        Map<String, Object> otpData = otpStore.get(lecturerId);
        if (otpData == null) {
            System.out.println("❌ Password reset failed: No OTP session found");
            result.put("success", false);
            result.put("message", "No OTP session found. Please restart the password reset process.");
            return result;
        }

        // Check if OTP was verified
        boolean isVerified = (boolean) otpData.get("verified");
        if (!isVerified) {
            System.out.println("❌ Password reset failed: OTP not verified");
            result.put("success", false);
            result.put("message", "OTP not verified. Please verify OTP first.");
            return result;
        }

        // Verify OTP again for security
        String expectedOtp = (String) otpData.get("otp");
        if (!expectedOtp.equals(otp)) {
            System.out.println("❌ Password reset failed: Invalid OTP");
            result.put("success", false);
            result.put("message", "Invalid OTP. Please use the correct OTP.");
            return result;
        }

        // Check if OTP is expired
        LocalDateTime expiresAt = (LocalDateTime) otpData.get("expiresAt");
        if (LocalDateTime.now().isAfter(expiresAt)) {
            System.out.println("❌ Password reset failed: OTP expired");
            otpStore.remove(lecturerId);
            result.put("success", false);
            result.put("message", "OTP has expired. Please request a new OTP.");
            return result;
        }

        // Reset password
        try {
            // Update lecturer password
            lecturer.setPassword(newPassword);

            // Save the changes
            boolean saved = saveLecturers();

            if (saved) {
                System.out.println("✅ Password updated successfully for lecturer: " + lecturerId);

                // Send success email
                try {
                    String email = (String) otpData.get("email");
                    String lecturerName = (String) otpData.get("lecturerName");
                    emailService.sendPasswordResetSuccessEmail(email, lecturerName);
                    System.out.println("✅ Password reset success email sent to: " + email);
                } catch (Exception e) {
                    System.out.println("⚠️ Password reset successful but failed to send email: " + e.getMessage());
                }

                // Clear OTP data after successful password reset
                otpStore.remove(lecturerId);

                result.put("success", true);
                result.put("message", "Password reset successfully! You can now login with your new password.");
            } else {
                System.out.println("❌ Failed to save password for lecturer: " + lecturerId);
                result.put("success", false);
                result.put("message", "Failed to reset password. Please try again.");
            }

        } catch (Exception e) {
            System.out.println("❌ Error resetting password: " + e.getMessage());
            result.put("success", false);
            result.put("message", "Failed to reset password. Please try again.");
        }

        return result;
    }

    /**
     * Resend OTP
     */
    public Map<String, Object> resendOTP(String lecturerId) {
        Map<String, Object> result = new HashMap<>();

        System.out.println("\n🔄 ===== RESENDING OTP =====");
        System.out.println("🔍 Lecturer ID: " + lecturerId);

        // Find lecturer
        LecturerData lecturer = findLecturerById(lecturerId);
        if (lecturer == null) {
            System.out.println("❌ Lecturer not found: " + lecturerId);
            result.put("success", false);
            result.put("message", "Lecturer ID not found.");
            return result;
        }

        // Generate new OTP
        String newOtp = generateOTP();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(10);

        // Update OTP data
        Map<String, Object> otpData = otpStore.get(lecturerId);
        if (otpData != null) {
            otpData.put("otp", newOtp);
            otpData.put("createdAt", now);
            otpData.put("expiresAt", expiresAt);
            otpData.put("verified", false);
            otpData.put("attempts", 0);
        } else {
            otpData = new HashMap<>();
            otpData.put("otp", newOtp);
            otpData.put("createdAt", now);
            otpData.put("expiresAt", expiresAt);
            otpData.put("lecturerId", lecturerId);
            otpData.put("lecturerName", lecturer.getName());
            otpData.put("email", lecturer.getEmail());
            otpData.put("verified", false);
            otpData.put("attempts", 0);
            otpStore.put(lecturerId, otpData);
        }

        System.out.println("🎯 New OTP generated: " + newOtp);
        System.out.println("🎯 Expires: " + expiresAt);

        // Send new OTP email using REAL EmailService
        boolean emailSent = false;

        try {
            emailService.sendOTPEmail(lecturer.getEmail(), lecturer.getName(), newOtp);
            emailSent = true;
            System.out.println("✅ New OTP email sent successfully to: " + lecturer.getEmail());
        } catch (Exception e) {
            System.out.println("❌ Failed to send new OTP email: " + e.getMessage());
        }

        if (emailSent) {
            result.put("success", true);
            result.put("message", "New OTP sent successfully to your email address.");
        } else {
            result.put("success", true);
            result.put("message", "New OTP generated but email delivery failed. Use OTP: " + newOtp);
            result.put("emailFailed", true);
            result.put("otp", newOtp);
        }

        return result;
    }

    /**
     * Login lecturer for web
     */
    public Map<String, Object> loginLecturerForWeb(String lecturerId, String password) {
        Map<String, Object> result = new HashMap<>();

        System.out.println("🌐 Web login attempt for lecturer: " + lecturerId);

        // Find lecturer
        LecturerData lecturer = findLecturerById(lecturerId);
        if (lecturer == null) {
            System.out.println("❌ Web login failed for lecturer: " + lecturerId + " (Lecturer not found)");
            result.put("success", false);
            result.put("error", "Invalid Lecturer ID or password.");
            return result;
        }

        // Verify password
        String storedPassword = lecturer.getPassword();
        if (storedPassword != null && storedPassword.equals(password)) {
            System.out.println("✅ Web login successful for lecturer: " + lecturerId);
            result.put("success", true);
            result.put("lecturerName", lecturer.getName());
            result.put("lecturerId", lecturerId);
        } else {
            System.out.println("❌ Web login failed for lecturer: " + lecturerId + " (Invalid password)");
            result.put("success", false);
            result.put("error", "Invalid Lecturer ID or password.");
        }

        return result;
    }

    /**
     * Clean up expired OTPs
     */
    public void cleanupExpiredOTPs() {
        LocalDateTime now = LocalDateTime.now();
        otpStore.entrySet().removeIf(entry -> {
            Map<String, Object> otpData = entry.getValue();
            LocalDateTime expiresAt = (LocalDateTime) otpData.get("expiresAt");
            return now.isAfter(expiresAt);
        });
    }
}
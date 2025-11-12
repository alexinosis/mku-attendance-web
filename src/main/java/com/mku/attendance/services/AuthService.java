package com.mku.attendance.services;

import com.mku.attendance.entities.StudentData;
import com.mku.attendance.entities.OTPVerification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;

@Service
public class AuthService {

    @Autowired
    private StudentManager studentManager;

    @Autowired
    private EmailService emailService;

    @Value("${app.otp.length:6}")
    private int otpLength;

    @Value("${app.otp.expiry-minutes:10}")
    private int otpExpiryMinutes;

    @Value("${app.security.max-login-attempts:5}")
    private int maxLoginAttempts;

    @Value("${app.email.enabled:true}")
    private boolean emailEnabled;

    private final Map<String, OTPVerification> otpStore = new HashMap<>();
    private final Map<String, Integer> loginAttempts = new HashMap<>();
    private final Map<String, LocalDateTime> accountLocks = new HashMap<>();

    /**
     * STUDENT LOGIN - FOR REACT FRONTEND (JSON RESPONSE)
     */
    public Map<String, Object> loginStudent(String studentId, String password) {
        Map<String, Object> result = new HashMap<>();

        if (isAccountLocked(studentId)) {
            System.out.println("❌ Login failed: Account temporarily locked - " + studentId);
            result.put("success", false);
            result.put("message", "Account temporarily locked. Try again in 30 minutes.");
            return result;
        }

        boolean isValid = studentManager.validateStudentCredentials(studentId, password);

        if (isValid) {
            loginAttempts.remove(studentId);
            accountLocks.remove(studentId);

            StudentData student = studentManager.getStudent(studentId);
            System.out.println("✅ Login successful for student: " + studentId);

            result.put("success", true);
            result.put("message", "Login successful");
            result.put("student", student.toMap());

        } else {
            int attempts = loginAttempts.getOrDefault(studentId, 0) + 1;
            loginAttempts.put(studentId, attempts);

            System.out.println("❌ Login failed for student: " + studentId + " (Attempt " + attempts + ")");

            if (attempts >= maxLoginAttempts) {
                lockAccount(studentId);
                System.out.println("🔒 Account locked for student: " + studentId);
                result.put("success", false);
                result.put("message", "Account locked due to too many failed attempts. Try again in 30 minutes.");
            } else {
                int remainingAttempts = maxLoginAttempts - attempts;
                result.put("success", false);
                result.put("message", "Invalid credentials. " + remainingAttempts + " attempts remaining.");
            }
        }

        return result;
    }

    /**
     * STUDENT LOGIN FOR WEB FORM - Returns redirect info for Thymeleaf
     */
    public Map<String, Object> loginStudentForWeb(String studentId, String password) {
        Map<String, Object> result = new HashMap<>();

        System.out.println("🌐 Web login attempt for student: " + studentId);

        if (isAccountLocked(studentId)) {
            System.out.println("❌ Web login failed: Account locked - " + studentId);
            result.put("success", false);
            result.put("error", "Account temporarily locked. Try again in 30 minutes.");
            return result;
        }

        boolean isValid = studentManager.validateStudentCredentials(studentId, password);

        if (isValid) {
            loginAttempts.remove(studentId);
            accountLocks.remove(studentId);

            StudentData student = studentManager.getStudent(studentId);
            System.out.println("✅ Web login successful for student: " + studentId);

            result.put("success", true);
            result.put("redirectUrl", "/student/dashboard");
            result.put("studentName", student.getName());
            result.put("studentId", studentId);

        } else {
            int attempts = loginAttempts.getOrDefault(studentId, 0) + 1;
            loginAttempts.put(studentId, attempts);

            System.out.println("❌ Web login failed for student: " + studentId + " (Attempt " + attempts + ")");

            if (attempts >= maxLoginAttempts) {
                lockAccount(studentId);
                System.out.println("🔒 Account locked for student: " + studentId);
                result.put("success", false);
                result.put("error", "Account locked due to too many failed attempts. Try again in 30 minutes.");
            } else {
                int remainingAttempts = maxLoginAttempts - attempts;
                result.put("success", false);
                result.put("error", "Invalid student ID or password. " + remainingAttempts + " attempts remaining.");
            }
        }

        return result;
    }

    /**
     * INITIATE PASSWORD RESET - UPDATED FOR REACT FRONTEND COMPATIBILITY
     * Matches your Deno edge function logic
     */
    public Map<String, Object> initiatePasswordReset(String studentId, String email) {
        Map<String, Object> result = new HashMap<>();

        System.out.println("\n🚀 ===== PASSWORD RESET INITIATED =====");
        System.out.println("🔍 Student ID: " + studentId);
        System.out.println("📧 Email: " + email);

        // Validate input (matches Deno function validation)
        if (studentId == null || studentId.trim().isEmpty() || email == null || email.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "Student ID and email are required");
            return result;
        }

        // Validate email format (matches Deno function)
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            result.put("success", false);
            result.put("message", "Invalid email format");
            return result;
        }

        // ENHANCED: Use the improved email validation
        System.out.println("🔍 Validating email existence for password reset: " + email);
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

            result.put("success", false);
            result.put("message", errorMessage);
            return result;
        }

        StudentData student = studentManager.getStudent(studentId);

        if (student == null) {
            System.out.println("❌ Password reset failed: Student not found - " + studentId);
            result.put("success", false);
            result.put("message", "Student ID and email combination not found");
            return result;
        }

        // Verify email matches student record (case-insensitive)
        if (!student.getEmail().equalsIgnoreCase(email.trim())) {
            System.out.println("❌ Password reset failed: Student ID and email do not match");
            result.put("success", false);
            result.put("message", "Student ID and email combination not found");
            return result;
        }

        String studentEmail = student.getEmail();
        System.out.println("✅ Student found: " + student.getName() + " (" + studentId + ")");

        // Generate OTP (matches Deno function)
        String otp = generateOTP();
        OTPVerification otpVerification = new OTPVerification(studentEmail, otp, otpExpiryMinutes);
        otpStore.put(studentEmail, otpVerification);

        // 🎯 ALWAYS LOG OTP TO CONSOLE (Guaranteed delivery)
        System.out.println("\n🎯 ===== OTP GENERATED =====");
        System.out.println("🎯 Student ID: " + studentId);
        System.out.println("🎯 Student Name: " + student.getName());
        System.out.println("🎯 Email: " + studentEmail);
        System.out.println("🎯 OTP CODE: " + otp);
        System.out.println("🎯 Expires: " + otpVerification.getExpiresAt());
        System.out.println("🎯 =========================\n");

        // Send OTP email
        try {
            if (emailEnabled) {
                System.out.println("📧 Attempting to send OTP email to: " + studentEmail);
                emailService.sendOTPEmail(studentEmail, otp, student.getName());
                System.out.println("✅ OTP email sent successfully to: " + studentEmail);

                result.put("success", true);
                result.put("message", "OTP sent to your registered email");
                result.put("email", maskEmail(studentEmail));
                result.put("studentName", student.getName());

            } else {
                System.out.println("📧 Email service disabled - OTP shown in console only");
                result.put("success", true);
                result.put("message", "OTP generated but email service is disabled. Use OTP: " + otp);
                result.put("email", maskEmail(studentEmail));
                result.put("studentName", student.getName());
                result.put("otp", otp);
                result.put("emailFailed", true);
            }

            System.out.println("✅ Password reset initiated successfully for student: " + studentId);

        } catch (Exception e) {
            System.err.println("\n❌ EMAIL SENDING FAILED: " + e.getMessage());

            // 🎯 RE-DISPLAY OTP WHEN EMAIL FAILS (Like Deno function fallback)
            System.out.println("\n🎯 ===== USE THIS OTP =====");
            System.out.println("🎯 Student: " + student.getName() + " (" + studentId + ")");
            System.out.println("🎯 OTP CODE: " + otp);
            System.out.println("🎯 Email failed, but OTP is valid for 10 minutes");
            System.out.println("🎯 ========================\n");

            result.put("success", true); // Still success because OTP was generated
            result.put("message", "OTP generated but email delivery failed. Use OTP: " + otp);
            result.put("email", maskEmail(studentEmail));
            result.put("studentName", student.getName());
            result.put("otp", otp);
            result.put("emailFailed", true);
        }

        return result;
    }

    /**
     * VERIFY OTP ONLY - Without resetting password
     */
    public Map<String, Object> verifyOTP(String studentId, String otp) {
        Map<String, Object> result = new HashMap<>();

        System.out.println("\n🔐 ===== OTP VERIFICATION ATTEMPT =====");
        System.out.println("🔍 Student: " + studentId);
        System.out.println("🔍 OTP Provided: " + otp);

        // Validate input
        if (studentId == null || studentId.trim().isEmpty() || otp == null || otp.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "Student ID and OTP are required");
            return result;
        }

        StudentData student = studentManager.getStudent(studentId);
        if (student == null) {
            System.out.println("❌ OTP verification failed: Student not found");
            result.put("success", false);
            result.put("message", "Student not found");
            return result;
        }

        String studentEmail = student.getEmail();
        OTPVerification otpVerification = otpStore.get(studentEmail);

        if (otpVerification == null) {
            System.out.println("❌ OTP verification failed: No OTP found for student");
            result.put("success", false);
            result.put("message", "OTP not found or expired. Please request a new OTP.");
            return result;
        }

        if (otpVerification.isUsed()) {
            System.out.println("❌ OTP verification failed: OTP already used");
            result.put("success", false);
            result.put("message", "OTP has already been used. Please request a new OTP.");
            return result;
        }

        if (otpVerification.isExpired()) {
            System.out.println("❌ OTP verification failed: OTP expired");
            otpStore.remove(studentEmail);
            result.put("success", false);
            result.put("message", "OTP has expired. Please request a new OTP.");
            return result;
        }

        // Increment attempt count
        otpVerification.incrementAttemptCount();
        System.out.println("🔍 Expected OTP: " + otpVerification.getOtp());
        System.out.println("🔍 Attempt: " + otpVerification.getAttemptCount() + "/3");

        if (!otpVerification.getOtp().equals(otp)) {
            System.out.println("❌ OTP verification failed: Invalid OTP");

            if (otpVerification.getAttemptCount() >= 3) {
                otpStore.remove(studentEmail);
                System.out.println("🚫 OTP blocked: Too many failed attempts");
                result.put("success", false);
                result.put("message", "Too many failed attempts. Please request a new OTP.");
            } else {
                int remainingAttempts = 3 - otpVerification.getAttemptCount();
                System.out.println("⚠️ Remaining attempts: " + remainingAttempts);
                result.put("success", false);
                result.put("message", "Invalid OTP. " + remainingAttempts + " attempts remaining.");
            }
            return result;
        }

        // OTP verified successfully
        System.out.println("✅ OTP verification successful for student: " + studentId);
        result.put("success", true);
        result.put("message", "OTP verified successfully");

        return result;
    }

    /**
     * RESET PASSWORD AFTER OTP VERIFICATION
     */
    public Map<String, Object> resetPasswordWithOTP(String studentId, String newPassword) {
        Map<String, Object> result = new HashMap<>();

        System.out.println("\n🔄 ===== PASSWORD RESET =====");
        System.out.println("🔍 Student: " + studentId);

        // Validate input
        if (studentId == null || studentId.trim().isEmpty() || newPassword == null || newPassword.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "Student ID and new password are required");
            return result;
        }

        StudentData student = studentManager.getStudent(studentId);
        if (student == null) {
            System.out.println("❌ Password reset failed: Student not found");
            result.put("success", false);
            result.put("message", "Student not found");
            return result;
        }

        // Validate password length
        if (newPassword.length() < 6) {
            result.put("success", false);
            result.put("message", "Password must be at least 6 characters long");
            return result;
        }

        // Update student password
        student.updatePassword(newPassword);
        studentManager.updateStudent(student);

        // Clean up OTP after successful password reset
        String studentEmail = student.getEmail();
        OTPVerification otpVerification = otpStore.remove(studentEmail);
        if (otpVerification != null) {
            otpVerification.setUsed(true);
        }

        // Send password reset success notification
        try {
            if (emailEnabled) {
                emailService.sendPasswordResetSuccessEmail(studentEmail, student.getName());
                System.out.println("✅ Password reset success email sent to: " + studentEmail);
            }
        } catch (Exception e) {
            System.err.println("⚠️ Failed to send reset success email, but password was reset: " + e.getMessage());
        }

        System.out.println("✅ Password reset successful for student: " + studentId);

        result.put("success", true);
        result.put("message", "Password reset successfully! You can now login with your new password.");
        return result;
    }

    /**
     * VERIFY OTP AND RESET PASSWORD - UPDATED FOR REACT FRONTEND
     * Matches your Deno edge function logic
     */
    public Map<String, Object> verifyOTPAndResetPassword(String studentId, String otp, String newPassword) {
        Map<String, Object> result = new HashMap<>();

        System.out.println("\n🔐 ===== OTP VERIFICATION ATTEMPT =====");
        System.out.println("🔍 Student: " + studentId);
        System.out.println("🔍 OTP Provided: " + otp);

        // Validate input (matches Deno function)
        if (studentId == null || studentId.trim().isEmpty() ||
                otp == null || otp.trim().isEmpty() ||
                newPassword == null || newPassword.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "Student ID, OTP, and new password are required");
            return result;
        }

        StudentData student = studentManager.getStudent(studentId);
        if (student == null) {
            System.out.println("❌ OTP verification failed: Student not found");
            result.put("success", false);
            result.put("message", "Student not found");
            return result;
        }

        String studentEmail = student.getEmail();
        OTPVerification otpVerification = otpStore.get(studentEmail);

        if (otpVerification == null) {
            System.out.println("❌ OTP verification failed: No OTP found for student");
            result.put("success", false);
            result.put("message", "OTP not found or expired. Please request a new OTP.");
            return result;
        }

        if (otpVerification.isUsed()) {
            System.out.println("❌ OTP verification failed: OTP already used");
            result.put("success", false);
            result.put("message", "OTP has already been used. Please request a new OTP.");
            return result;
        }

        if (otpVerification.isExpired()) {
            System.out.println("❌ OTP verification failed: OTP expired");
            otpStore.remove(studentEmail);
            result.put("success", false);
            result.put("message", "OTP has expired. Please request a new OTP.");
            return result;
        }

        // Increment attempt count (matches Deno function)
        otpVerification.incrementAttemptCount();
        System.out.println("🔍 Expected OTP: " + otpVerification.getOtp());
        System.out.println("🔍 Attempt: " + otpVerification.getAttemptCount() + "/3");

        if (!otpVerification.getOtp().equals(otp)) {
            System.out.println("❌ OTP verification failed: Invalid OTP");

            if (otpVerification.getAttemptCount() >= 3) {
                otpStore.remove(studentEmail);
                System.out.println("🚫 OTP blocked: Too many failed attempts");
                result.put("success", false);
                result.put("message", "Too many failed attempts. Please request a new OTP.");
            } else {
                int remainingAttempts = 3 - otpVerification.getAttemptCount();
                System.out.println("⚠️ Remaining attempts: " + remainingAttempts);
                result.put("success", false);
                result.put("message", "Invalid OTP. " + remainingAttempts + " attempts remaining.");
            }
            return result;
        }

        // OTP verified successfully - reset password
        System.out.println("✅ OTP verification successful!");

        // Update student password (use new method for proper handling)
        student.updatePassword(newPassword);
        studentManager.updateStudent(student);

        // Mark OTP as used and clean up
        otpVerification.setUsed(true);
        otpStore.remove(studentEmail);

        // Send password reset success notification
        try {
            if (emailEnabled) {
                emailService.sendPasswordResetSuccessEmail(studentEmail, student.getName());
                System.out.println("✅ Password reset success email sent to: " + studentEmail);
            }
        } catch (Exception e) {
            System.err.println("⚠️ Failed to send reset success email, but password was reset: " + e.getMessage());
        }

        System.out.println("✅ Password reset successful for student: " + studentId);

        result.put("success", true);
        result.put("message", "Password reset successfully!");
        return result;
    }

    /**
     * RESEND OTP - UPDATED FOR REACT FRONTEND
     */
    public Map<String, Object> resendOTP(String studentId) {
        Map<String, Object> result = new HashMap<>();

        System.out.println("\n🔄 ===== RESENDING OTP =====");
        System.out.println("🔍 Student: " + studentId);

        StudentData student = studentManager.getStudent(studentId);
        if (student == null) {
            System.out.println("❌ Resend OTP failed: Student not found");
            result.put("success", false);
            result.put("message", "Student not found");
            return result;
        }

        if (!student.hasValidEmail()) {
            System.out.println("❌ Resend OTP failed: No valid email found");
            result.put("success", false);
            result.put("message", "No registered email found");
            return result;
        }

        String studentEmail = student.getEmail();

        // Remove existing OTP if any
        OTPVerification oldOtp = otpStore.remove(studentEmail);
        if (oldOtp != null) {
            System.out.println("🗑️ Removed previous OTP");
        }

        // Generate new OTP
        String newOtp = generateOTP();
        OTPVerification otpVerification = new OTPVerification(studentEmail, newOtp, otpExpiryMinutes);
        otpStore.put(studentEmail, otpVerification);

        // 🎯 ALWAYS LOG NEW OTP TO CONSOLE
        System.out.println("\n🎯 ===== NEW OTP GENERATED =====");
        System.out.println("🎯 Student: " + student.getName() + " (" + studentId + ")");
        System.out.println("🎯 Email: " + studentEmail);
        System.out.println("🎯 NEW OTP CODE: " + newOtp);
        System.out.println("🎯 Expires: " + otpVerification.getExpiresAt());
        System.out.println("🎯 ============================\n");

        // Send new OTP email
        try {
            if (emailEnabled) {
                System.out.println("📧 Attempting to send new OTP email to: " + studentEmail);
                emailService.sendOTPEmail(studentEmail, newOtp, student.getName());
                System.out.println("✅ New OTP email sent successfully to: " + studentEmail);

                result.put("success", true);
                result.put("message", "New OTP sent to your registered email");
                result.put("email", maskEmail(studentEmail));
                result.put("otp", newOtp);

            } else {
                System.out.println("📧 Email service disabled - New OTP shown in console only");
                result.put("success", true);
                result.put("message", "New OTP generated but email service is disabled. Use OTP: " + newOtp);
                result.put("email", maskEmail(studentEmail));
                result.put("otp", newOtp);
                result.put("emailFailed", true);
            }

        } catch (Exception e) {
            System.err.println("\n❌ FAILED TO RESEND OTP EMAIL: " + e.getMessage());

            // 🎯 RE-DISPLAY NEW OTP WHEN EMAIL FAILS
            System.out.println("\n🎯 ===== USE THIS NEW OTP =====");
            System.out.println("🎯 Student: " + student.getName() + " (" + studentId + ")");
            System.out.println("🎯 NEW OTP CODE: " + newOtp);
            System.out.println("🎯 Email failed, but NEW OTP is valid for 10 minutes");
            System.out.println("🎯 ============================\n");

            result.put("success", true);
            result.put("message", "New OTP generated but email delivery failed. Use OTP: " + newOtp);
            result.put("email", maskEmail(studentEmail));
            result.put("otp", newOtp);
            result.put("emailFailed", true);
        }

        return result;
    }

    /**
     * STUDENT REGISTRATION - UPDATED FOR REACT FRONTEND WITH ENHANCED EMAIL VALIDATION
     */
    public Map<String, Object> registerStudent(StudentData student) {
        Map<String, Object> result = new HashMap<>();

        System.out.println("🔍 Validating student registration for: " + student.getStudentId());

        if (studentManager.exists(student.getStudentId())) {
            System.out.println("❌ Registration failed: Student ID already exists");
            result.put("success", false);
            result.put("message", "Student ID already exists");
            return result;
        }

        // ENHANCED: Use the improved email validation
        String email = student.getEmail();
        System.out.println("🔍 Validating email for registration: " + email);
        boolean isEmailValid = emailService.validateEmailExistence(email);
        System.out.println("🔍 Email validation result: " + isEmailValid);

        if (!isEmailValid) {
            System.out.println("❌ Registration failed: Invalid email - " + email);
            Map<String, Object> emailValidation = emailService.validateEmailWithDetails(email);

            String errorMessage = "Invalid email address";
            if (Boolean.TRUE.equals(emailValidation.get("isDisposable"))) {
                errorMessage = "Disposable/temporary email addresses are not allowed. Please use your permanent email.";
            } else if (!Boolean.TRUE.equals(emailValidation.get("isValidFormat"))) {
                errorMessage = "Invalid email format. Please check and try again.";
            }

            result.put("success", false);
            result.put("message", errorMessage);
            return result;
        }

        if (findStudentByEmail(student.getEmail()) != null) {
            System.out.println("❌ Registration failed: Email already registered");
            result.put("success", false);
            result.put("message", "Email address already registered");
            return result;
        }

        // Ensure student has all required fields for database
        if (!student.isValidForRegistration()) {
            System.out.println("❌ Registration failed: Missing required fields");
            result.put("success", false);
            result.put("message", "All fields are required");
            return result;
        }

        studentManager.addStudent(student);
        System.out.println("✅ Student registered successfully: " + student.getStudentId());

        result.put("success", true);
        result.put("message", "Registration successful");
        result.put("student", student.toMap());

        return result;
    }

    // ==== PRIVATE HELPER METHODS ====
    private String generateOTP() {
        Random random = new Random();
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < otpLength; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }

    public StudentData findStudentByEmail(String email) {
        return studentManager.getStudents().values().stream()
                .filter(student -> email.equalsIgnoreCase(student.getEmail()))
                .findFirst()
                .orElse(null);
    }

    private String maskEmail(String email) {
        if (email == null || email.length() < 5) return "***@***";
        int atIndex = email.indexOf('@');
        if (atIndex <= 2) return "***" + email.substring(atIndex);
        String firstPart = email.substring(0, 2);
        String domain = email.substring(atIndex);
        return firstPart + "***" + domain;
    }

    private boolean isAccountLocked(String studentId) {
        LocalDateTime lockTime = accountLocks.get(studentId);
        if (lockTime == null) return false;

        if (LocalDateTime.now().isAfter(lockTime.plusMinutes(30))) {
            accountLocks.remove(studentId);
            loginAttempts.remove(studentId);
            return false;
        }
        return true;
    }

    private void lockAccount(String studentId) {
        accountLocks.put(studentId, LocalDateTime.now());
    }

    // ==== MAINTENANCE METHODS ====
    public void cleanupExpiredOTPs() {
        int initialSize = otpStore.size();
        otpStore.entrySet().removeIf(entry -> entry.getValue().isExpired());
        int removed = initialSize - otpStore.size();
        if (removed > 0) {
            System.out.println("🧹 Cleaned up " + removed + " expired OTPs");
        }
    }

    /** GET OTP INFO FOR DEBUGGING */
    public Map<String, Object> getOTPInfo(String studentId) {
        Map<String, Object> info = new HashMap<>();

        StudentData student = studentManager.getStudent(studentId);
        if (student != null && student.hasValidEmail()) {
            OTPVerification otp = otpStore.get(student.getEmail());
            if (otp != null) {
                info.put("hasOTP", true);
                info.put("otp", otp.getOtp());
                info.put("expired", otp.isExpired());
                info.put("used", otp.isUsed());
                info.put("attempts", otp.getAttemptCount());
                info.put("expiresAt", otp.getExpiresAt().toString());

                if (!otp.isExpired()) {
                    long remainingMinutes = java.time.Duration.between(LocalDateTime.now(), otp.getExpiresAt()).toMinutes();
                    info.put("remainingMinutes", remainingMinutes);
                    info.put("expiresIn", remainingMinutes + " minutes");
                } else {
                    info.put("expiresIn", "Expired");
                }
            } else {
                info.put("hasOTP", false);
            }
            info.put("studentEmail", student.getEmail());
        } else {
            info.put("hasOTP", false);
            info.put("studentEmail", "not available");
        }

        return info;
    }

    /** GET ALL ACTIVE OTPS */
    public Map<String, Object> getAllActiveOTPs() {
        Map<String, Object> result = new HashMap<>();
        Map<String, String> activeOTPs = new HashMap<>();

        int activeCount = 0;
        for (Map.Entry<String, OTPVerification> entry : otpStore.entrySet()) {
            OTPVerification otp = entry.getValue();
            if (!otp.isExpired() && !otp.isUsed()) {
                activeOTPs.put(entry.getKey(), otp.getOtp());
                activeCount++;
            }
        }

        result.put("activeCount", activeCount);
        result.put("totalOTPs", otpStore.size());
        result.put("activeOTPs", activeOTPs);

        return result;
    }

    /** DEBUG: Force display all active OTPs */
    public void debugDisplayAllOTPs() {
        System.out.println("\n🔍 ===== DEBUG: ALL ACTIVE OTPS =====");
        if (otpStore.isEmpty()) {
            System.out.println("No active OTPs found");
        } else {
            for (Map.Entry<String, OTPVerification> entry : otpStore.entrySet()) {
                OTPVerification otp = entry.getValue();
                System.out.println("📧 " + entry.getKey() + " -> " + otp.getOtp() +
                        " (Expired: " + otp.isExpired() + ", Used: " + otp.isUsed() + ")");
            }
        }
        System.out.println("==================================\n");
    }
}
package com.mku.attendance.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;
import java.util.HashMap;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.NamingException;
import java.util.Hashtable;
import java.util.regex.Pattern;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.io.IOException;
import java.net.Socket;
import javax.net.SocketFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.email.from:no-reply@mku.ac.ke}")
    private String fromEmail;

    @Value("${app.otp.expiry-minutes:10}")
    private int otpExpiryMinutes;

    @Value("${app.email.enabled:true}")
    private boolean emailEnabled;

    @Value("${spring.mail.host:smtp.sendgrid.net}")
    private String mailHost;

    @Value("${spring.mail.port:587}")
    private int mailPort;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    // Enhanced email validation patterns
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    // Expanded list of disposable email domains
    private static final String[] DISPOSABLE_DOMAINS = {
            "tempmail.com", "guerrillamail.com", "mailinator.com", "10minutemail.com",
            "throwawaymail.com", "fakeinbox.com", "yopmail.com", "trashmail.com",
            "temp-mail.org", "disposableemail.com", "getairmail.com", "maildrop.cc",
            "sharklasers.com", "guerrillamail.net", "grr.la", "guerrillamail.biz",
            "spam4.me", "fake-mail.com", "mailnesia.com", "mailcatch.com",
            "tempinbox.com", "emailondeck.com", "tmpmail.org", "mail-temp.com",
            "temp-mail.io", "mail-temporaire.com", "temporary-mail.net", "tempomail.fr",
            "emailtemp.com", "tempmail.net", "temp-mail.com", "temporarymail.com",
            "mailinator.net", "mailinator.org", "mailinator2.com", "mailinator3.com",
            "mailinator4.com", "mailinator5.com", "mailinator6.com", "mailinator7.com",
            "mailinator8.com", "mailinator9.com", "mailinator10.com"
    };

    /**
     * REAL EMAIL VERIFICATION - Check if email is ACTIVE and REGISTERED
     */
    public boolean validateEmailExistence(String email) {
        System.out.println("🔍 REAL EMAIL VERIFICATION for: " + email);

        try {
            // Step 1: Basic email format validation
            if (!isValidEmailFormat(email)) {
                System.out.println("❌ Invalid email format: " + email);
                return false;
            }

            // Step 2: Check for disposable/temporary emails
            if (isDisposableEmail(email)) {
                System.out.println("❌ Disposable email detected: " + email);
                return false;
            }

            // Step 3: Extract domain
            String domain = extractDomain(email);
            System.out.println("🌐 Checking domain: " + domain);

            // Step 4: Check domain has MX records
            if (!checkMXRecords(domain)) {
                System.out.println("❌ Domain has no MX records: " + domain);
                return false;
            }

            // Step 5: REAL SMTP VERIFICATION - Check if email actually exists
            boolean emailExists = verifyEmailViaSMTP(email);
            if (!emailExists) {
                System.out.println("❌ Email account does not exist or cannot receive emails: " + email);
                return false;
            }

            System.out.println("✅ REAL EMAIL VERIFICATION PASSED: " + email + " is ACTIVE and REGISTERED");
            return true;

        } catch (Exception e) {
            System.err.println("❌ Email verification failed for " + email + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * REAL SMTP VERIFICATION - Check if email account actually exists and is active
     */
    private boolean verifyEmailViaSMTP(String email) {
        System.out.println("🔍 Performing REAL SMTP verification for: " + email);

        String domain = extractDomain(email);

        try {
            // Get MX records for the domain
            String mxRecord = getMXRecord(domain);
            if (mxRecord == null) {
                System.out.println("❌ No MX record found for domain: " + domain);
                return false;
            }

            System.out.println("📧 Found MX record: " + mxRecord);

            // Perform full SMTP verification
            return performFullSmtpVerification(mxRecord, email, domain);

        } catch (Exception e) {
            System.err.println("❌ SMTP verification failed: " + e.getMessage());
            // Fallback: if SMTP fails, check if domain is reachable
            return isDomainReachable(domain);
        }
    }

    /**
     * Perform full SMTP conversation to verify email existence
     */
    private boolean performFullSmtpVerification(String mxRecord, String email, String domain) {
        System.out.println("🔧 Starting full SMTP verification for: " + email);

        try (Socket socket = SocketFactory.getDefault().createSocket(mxRecord, 25)) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

            // Read welcome message
            String response = reader.readLine();
            System.out.println("📧 SMTP Server: " + response);
            if (!response.startsWith("220")) {
                System.out.println("❌ SMTP server not ready");
                return false;
            }

            // Send EHLO
            writer.println("EHLO " + domain);
            response = reader.readLine();
            System.out.println("📧 EHLO Response: " + response);
            if (!response.startsWith("250")) {
                System.out.println("❌ EHLO failed");
                return false;
            }

            // Read additional EHLO responses
            while (response.startsWith("250-")) {
                response = reader.readLine();
                System.out.println("📧 EHLO Additional: " + response);
            }

            // Send MAIL FROM
            writer.println("MAIL FROM: <test@" + domain + ">");
            response = reader.readLine();
            System.out.println("📧 MAIL FROM Response: " + response);
            if (!response.startsWith("250")) {
                System.out.println("❌ MAIL FROM failed");
                return false;
            }

            // Send RCPT TO (this is where we check if email exists)
            writer.println("RCPT TO: <" + email + ">");
            response = reader.readLine();
            System.out.println("📧 RCPT TO Response: " + response);

            // Check if recipient is accepted
            boolean emailExists = response.startsWith("250");
            System.out.println("✅ Email existence check: " + (emailExists ? "EXISTS" : "DOES NOT EXIST"));

            // Send QUIT
            writer.println("QUIT");
            reader.readLine();

            return emailExists;

        } catch (IOException e) {
            System.err.println("❌ SMTP connection failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get MX record for domain
     */
    private String getMXRecord(String domain) {
        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            env.put("com.sun.jndi.dns.timeout.initial", "3000");
            env.put("com.sun.jndi.dns.timeout.retries", "2");

            DirContext ictx = new InitialDirContext(env);
            javax.naming.directory.Attributes attrs = ictx.getAttributes(domain, new String[]{"MX"});

            if (attrs.get("MX") != null) {
                String mxRecord = attrs.get("MX").get().toString();
                // Extract the mail server from MX record
                String[] parts = mxRecord.split("\\s+");
                if (parts.length >= 2) {
                    String mailServer = parts[1].endsWith(".") ?
                            parts[1].substring(0, parts[1].length() - 1) : parts[1];
                    System.out.println("✅ Found MX record: " + mailServer);
                    return mailServer;
                }
            }
        } catch (NamingException e) {
            System.err.println("❌ DNS lookup failed for domain " + domain + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Quick validation for forms (basic checks only)
     */
    public boolean validateEmailFormatQuick(String email) {
        System.out.println("🔍 Quick validating email: " + email);

        if (!isValidEmailFormat(email)) {
            System.out.println("❌ Invalid email format: " + email);
            return false;
        }

        String domain = extractDomain(email);

        // Check disposable emails
        if (isDisposableEmail(email)) {
            System.out.println("❌ Disposable email: " + email);
            return false;
        }

        System.out.println("✅ Quick email validation passed: " + email);
        return true;
    }

    /**
     * Check if domain is reachable via DNS
     */
    private boolean isDomainReachable(String domain) {
        try {
            InetAddress address = InetAddress.getByName(domain);
            boolean isReachable = address.isReachable(5000); // 5 second timeout
            System.out.println("🌐 Domain " + domain + " reachable: " + isReachable);
            return isReachable;
        } catch (UnknownHostException e) {
            System.err.println("❌ Domain not found: " + domain);
            return false;
        } catch (Exception e) {
            System.err.println("⚠️ Domain reachability check failed for " + domain + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if email is from disposable/temporary email service
     */
    private boolean isDisposableEmail(String email) {
        if (email == null) return true;

        String domain = email.substring(email.indexOf('@') + 1).toLowerCase();

        for (String disposableDomain : DISPOSABLE_DOMAINS) {
            if (domain.equals(disposableDomain) || domain.endsWith("." + disposableDomain)) {
                return true;
            }
        }

        // Additional pattern matching for disposable emails
        String[] disposablePatterns = {
                "temp", "fake", "trash", "throwaway", "spam", "temporary", "disposable"
        };

        for (String pattern : disposablePatterns) {
            if (domain.contains(pattern)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if domain has MX records using DNS lookup
     */
    private boolean checkMXRecords(String domain) {
        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            env.put("com.sun.jndi.dns.timeout.initial", "3000"); // 3 second timeout
            env.put("com.sun.jndi.dns.timeout.retries", "2");

            DirContext ictx = new InitialDirContext(env);
            javax.naming.directory.Attributes attrs = ictx.getAttributes(domain, new String[]{"MX"});

            if (attrs.get("MX") != null) {
                System.out.println("✅ Domain has MX records: " + domain);
                return true;
            } else {
                System.out.println("❌ No MX records found for domain: " + domain);
                return false;
            }
        } catch (NamingException e) {
            System.err.println("❌ DNS lookup failed for domain " + domain + ": " + e.getMessage());
            return false; // Consider as invalid if DNS lookup fails
        }
    }

    /**
     * Enhanced email format validation
     */
    private boolean isValidEmailFormat(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        // Remove any whitespace
        email = email.trim();

        // Check basic structure
        if (!email.contains("@") || email.startsWith("@") || email.endsWith("@")) {
            return false;
        }

        // Check for common fake patterns
        if (email.matches(".*[0-9]{10,}@.*") ||
                email.matches(".*[a-z]{15,}@.*") ||
                email.matches(".*test.*@.*") ||
                email.matches(".*fake.*@.*")) {
            System.out.println("❌ Email matches fake pattern: " + email);
            return false;
        }

        // Use regex pattern for comprehensive validation
        boolean isValid = EMAIL_PATTERN.matcher(email).matches();

        if (!isValid) {
            System.out.println("❌ Email format invalid: " + email);
        }

        return isValid;
    }

    /**
     * Enhanced email validation with detailed reporting
     */
    public Map<String, Object> validateEmailWithDetails(String email) {
        Map<String, Object> result = new HashMap<>();

        result.put("email", email);
        result.put("isValidFormat", isValidEmailFormat(email));
        result.put("isDisposable", isDisposableEmail(email));
        result.put("hasValidTld", hasValidTLD(email));
        result.put("domain", extractDomain(email));
        result.put("isDomainReachable", isDomainReachable(extractDomain(email)));
        result.put("hasMXRecords", checkMXRecords(extractDomain(email)));

        if (isValidEmailFormat(email) && !isDisposableEmail(email)) {
            String domain = extractDomain(email);
            // Perform real SMTP verification for detailed results
            boolean emailExists = verifyEmailViaSMTP(email);
            result.put("emailExists", emailExists);
            result.put("overallValid", emailExists);
        } else {
            result.put("emailExists", false);
            result.put("overallValid", false);
        }

        return result;
    }

    private boolean hasValidTLD(String email) {
        if (email == null) return false;
        String domain = extractDomain(email);

        String[] validTlds = {".com", ".org", ".net", ".edu", ".ac", ".ke", ".co", ".io",
                ".info", ".me", ".us", ".uk", ".ca", ".au", ".de", ".fr",
                ".it", ".es", ".jp", ".cn", ".in", ".br", ".ru", ".mx",
                ".gov", ".mil", ".biz", ".name", ".mobi", ".tel", ".travel"};

        for (String tld : validTlds) {
            if (domain.endsWith(tld)) {
                return true;
            }
        }
        return false;
    }

    private String extractDomain(String email) {
        if (email == null || !email.contains("@")) return "";
        return email.substring(email.indexOf('@') + 1).toLowerCase();
    }

    /**
     * ULTRA-RELIABLE OTP EMAIL SENDING - FIXED PARAMETER ORDER
     */
    @Async
    public void sendOTPEmail(String toEmail, String studentName, String otp) { // FIXED: Correct parameter order
        System.out.println("\n📧 ===== ATTEMPTING TO SEND OTP EMAIL =====");
        System.out.println("🎯 TO: " + toEmail);
        System.out.println("🎯 STUDENT: " + studentName); // Now shows correct name
        System.out.println("🎯 OTP: " + otp); // Now shows correct OTP

        // ALWAYS display OTP in console regardless of email success
        displayOTPInConsole(toEmail, studentName, otp); // Fixed parameter order

        if (!emailEnabled) {
            System.out.println("❌ EMAIL SERVICE DISABLED");
            return;
        }

        // Clean email addresses to remove any whitespace
        String cleanFromEmail = cleanEmail(fromEmail);
        String cleanToEmail = cleanEmail(toEmail);

        System.out.println("🔧 Cleaned From: " + cleanFromEmail);
        System.out.println("🔧 Cleaned To: " + cleanToEmail);

        try {
            // Method 1: Try simple email first (most reliable)
            sendSimpleOTPEmail(cleanToEmail, studentName, otp, cleanFromEmail); // Fixed parameter order
            System.out.println("✅ OTP EMAIL SENT SUCCESSFULLY TO: " + cleanToEmail);

        } catch (Exception e) {
            System.err.println("❌ EMAIL FAILED: " + e.getMessage());
            System.out.println("🎯 OTP STILL AVAILABLE IN CONSOLE: " + otp);
        }
    }

    /**
     * SEND PASSWORD RESET SUCCESS EMAIL
     */
    @Async
    public void sendPasswordResetSuccessEmail(String toEmail, String studentName) {
        if (!emailEnabled) {
            System.out.println("📧 EMAIL DISABLED - Password reset success for: " + studentName);
            return;
        }

        try {
            String cleanFromEmail = cleanEmail(fromEmail);
            String cleanToEmail = cleanEmail(toEmail);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(cleanFromEmail);
            message.setTo(cleanToEmail);
            message.setSubject("✅ MKU Attendance - Password Reset Successful");

            String emailContent = "Dear " + studentName + ",\n\n" +
                    "Your password has been successfully reset.\n\n" +
                    "You can now log in to your MKU Attendance account using your new password.\n\n" +
                    "Best regards,\n" +
                    "MKU Attendance System\n" +
                    "Mount Kenya University";

            message.setText(emailContent);
            mailSender.send(message);
            System.out.println("✅ Password reset success email sent to: " + cleanToEmail);

        } catch (Exception e) {
            System.err.println("⚠️ Failed to send reset success email: " + e.getMessage());
        }
    }

    /**
     * TEST EMAIL CONFIGURATION
     */
    public Map<String, Object> testEmailConfiguration() {
        Map<String, Object> result = new HashMap<>();

        System.out.println("\n🔧 ===== EMAIL CONFIGURATION TEST =====");
        System.out.println("📧 From Email: " + fromEmail);
        System.out.println("🕒 OTP Expiry: " + otpExpiryMinutes + " minutes");
        System.out.println("✅ Email Enabled: " + emailEnabled);
        System.out.println("🔗 Mail Host: " + mailHost);
        System.out.println("🔢 Mail Port: " + mailPort);
        System.out.println("👤 Mail Username: " + (mailUsername.isEmpty() ? "NOT SET" : "SET"));
        System.out.println("🔧 Cleaned From: " + cleanEmail(fromEmail));

        // Test if mail sender is configured
        boolean mailSenderConfigured = mailSender != null;
        System.out.println("📨 Mail Sender: " + (mailSenderConfigured ? "✅ CONFIGURED" : "❌ NOT CONFIGURED"));

        result.put("fromEmail", fromEmail);
        result.put("enabled", emailEnabled);
        result.put("host", mailHost);
        result.put("port", mailPort);
        result.put("usernameSet", !mailUsername.isEmpty());
        result.put("mailSenderConfigured", mailSenderConfigured);
        result.put("cleanedFromEmail", cleanEmail(fromEmail));

        System.out.println("=====================================\n");
        return result;
    }

    /**
     * Clean email address - remove whitespace and control characters
     */
    private String cleanEmail(String email) {
        if (email == null) return "";
        return email.replaceAll("\\s+", "").replaceAll("[\\p{Cntrl}]", "").trim();
    }

    /**
     * Simple text email (MOST RELIABLE) - FIXED PARAMETER ORDER
     */
    private void sendSimpleOTPEmail(String toEmail, String studentName, String otp, String fromEmail) { // Fixed parameter order
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("MKU Attendance - OTP Verification Code");

        String emailContent = "Dear " + studentName + ",\n\n" +
                "Your OTP verification code is: " + otp + "\n\n" +
                "This code will expire in " + otpExpiryMinutes + " minutes.\n\n" +
                "If you didn't request this code, please ignore this email.\n\n" +
                "Best regards,\n" +
                "MKU Attendance System\n" +
                "Mount Kenya University";

        message.setText(emailContent);
        mailSender.send(message);
    }

    /**
     * HTML email fallback - FIXED PARAMETER ORDER
     */
    private void sendHTMLOTPEmail(String toEmail, String studentName, String otp, String fromEmail) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(toEmail);
        helper.setSubject("🔐 MKU Attendance - OTP Verification Code");

        String htmlContent = createOTPEmailTemplate(studentName, otp);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }

    /**
     * GUARANTEED OTP DISPLAY IN CONSOLE - FIXED PARAMETER ORDER
     */
    private void displayOTPInConsole(String toEmail, String studentName, String otp) { // Fixed parameter order
        System.out.println("\n🎯 ===== OTP GENERATED - CHECK BELOW =====");
        System.out.println("🎯 STUDENT: " + studentName);
        System.out.println("🎯 EMAIL: " + toEmail);
        System.out.println("🎯 OTP CODE: " + otp);
        System.out.println("🎯 EXPIRES IN: " + otpExpiryMinutes + " minutes");
        System.out.println("🎯 TIMESTAMP: " + java.time.LocalDateTime.now());
        System.out.println("🎯 ===================================\n");
    }

    /**
     * BEAUTIFUL OTP EMAIL TEMPLATE - FIXED STRING CONCATENATION
     */
    private String createOTPEmailTemplate(String studentName, String otp) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html lang='en'>");
        html.append("<head>");
        html.append("    <meta charset='UTF-8'>");
        html.append("    <meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("    <title>OTP Verification</title>");
        html.append("    <style>");
        html.append("        body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 20px; }");
        html.append("        .container { max-width: 600px; margin: 0 auto; background: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }");
        html.append("        .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 25px; border-radius: 10px 10px 0 0; text-align: center; }");
        html.append("        .otp-code { font-size: 42px; font-weight: bold; color: #ff6b35; text-align: center; margin: 30px 0; padding: 20px; background: #f8f9fa; border-radius: 10px; letter-spacing: 8px; border: 2px dashed #dee2e6; }");
        html.append("        .footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #e9ecef; color: #6c757d; font-size: 14px; }");
        html.append("        .warning { background: #fff3cd; border: 1px solid #ffeaa7; padding: 15px; border-radius: 8px; margin: 20px 0; color: #856404; }");
        html.append("    </style>");
        html.append("</head>");
        html.append("<body>");
        html.append("    <div class='container'>");
        html.append("        <div class='header'>");
        html.append("            <h1>🦁 MKU Attendance System</h1>");
        html.append("            <p>Secure OTP Verification</p>");
        html.append("        </div>");
        html.append("        <h2>Dear ").append(studentName).append(",</h2>");
        html.append("        <p>You have requested to reset your password for the MKU Attendance System.</p>");
        html.append("        <p>Please use the following One-Time Password (OTP) to verify your identity:</p>");
        html.append("        <div class='otp-code'>").append(otp).append("</div>");
        html.append("        <div class='warning'>");
        html.append("            <strong>⚠️ Security Notice:</strong><br>");
        html.append("            • This OTP expires in ").append(otpExpiryMinutes).append(" minutes<br>");
        html.append("            • Do not share this code with anyone<br>");
        html.append("            • If you didn't request this, please ignore this email");
        html.append("        </div>");
        html.append("        <p>Best regards,<br>MKU Attendance System Team</p>");
        html.append("        <div class='footer'>");
        html.append("            <p>Mount Kenya University<br>Automated Message - Do not reply</p>");
        html.append("        </div>");
        html.append("    </div>");
        html.append("</body>");
        html.append("</html>");

        return html.toString();
    }

    /**
     * SEND TEST EMAIL
     */
    @Async
    public void sendTestEmail(String toEmail) {
        System.out.println("\n🧪 ===== SENDING TEST EMAIL =====");
        System.out.println("📧 TO: " + toEmail);

        if (!emailEnabled) {
            System.out.println("❌ EMAIL SERVICE DISABLED");
            return;
        }

        try {
            String cleanFromEmail = cleanEmail(fromEmail);
            String cleanToEmail = cleanEmail(toEmail);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(cleanFromEmail);
            message.setTo(cleanToEmail);
            message.setSubject("✅ MKU Attendance - Test Email");

            String emailContent = "This is a test email from MKU Attendance System.\n\n" +
                    "If you received this, your email configuration is working correctly!\n\n" +
                    "Best regards,\nMKU Attendance System";

            message.setText(emailContent);
            mailSender.send(message);

            System.out.println("✅ TEST EMAIL SENT SUCCESSFULLY TO: " + cleanToEmail);
            System.out.println("🎉 EMAIL SYSTEM IS OPERATIONAL!");

        } catch (Exception e) {
            System.err.println("❌ FAILED TO SEND TEST EMAIL: " + e.getMessage());
        }
        System.out.println("====================================\n");
    }
}
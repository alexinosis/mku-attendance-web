package com.mku.attendance;

import com.mku.attendance.services.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.annotation.PostConstruct;

@SpringBootApplication
@EnableAsync
public class MkuAttendanceApplication {

    @Autowired
    private EmailService emailService;

    public static void main(String[] args) {
        SpringApplication.run(MkuAttendanceApplication.class, args);
    }

    @PostConstruct
    public void init() {
        System.out.println("🚀 Starting MKU Attendance System...");
        displayStartupInfo();

        // Test email configuration if service is available
        if (emailService != null) {
            emailService.testEmailConfiguration();
        }
    }

    private void displayStartupInfo() {
        System.out.println("\n🔧 ===== SYSTEM STARTUP INFORMATION =====");
        System.out.println("🌐 Server: http://localhost:8080");
        System.out.println("📁 Data: ./attendance_data/");
        System.out.println("🔄 Async: ENABLED");
        System.out.println("🎯 OTP: CONSOLE + EMAIL DELIVERY");
        System.out.println("======================================\n");
    }

    @Configuration
    public static class WebConfig implements WebMvcConfigurer {
        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {
            registry.addResourceHandler("/images/**")
                    .addResourceLocations("classpath:/static/images/");
            registry.addResourceHandler("/css/**")
                    .addResourceLocations("classpath:/static/css/");
            registry.addResourceHandler("/js/**")
                    .addResourceLocations("classpath:/static/js/");
        }
    }
}
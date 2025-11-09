package com.mku.attendance.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    
    @GetMapping("/")
    public String home() {
        return "MKU Attendance System is running! Visit /images/ for static resources.";
    }
    
    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
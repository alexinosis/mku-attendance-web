package com.mku.attendance.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        // This will load home.html from src/main/resources/templates/
        return "home";
    }

    @GetMapping("/home")
    public String redirectHome() {
        // Optional: handles both "/" and "/home"
        return "home";
    }
}

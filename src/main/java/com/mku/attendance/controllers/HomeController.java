package com.mku.attendance.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(@RequestParam(value = "name", required = false) String name) {
        // This will accept the name parameter if provided, but not require it
        System.out.println("🏠 Home page accessed" + (name != null ? ", name parameter: " + name : ""));
        // This will load home.html from src/main/resources/templates/
        return "home";
    }

    @GetMapping("/home")
    public String redirectHome(@RequestParam(value = "name", required = false) String name) {
        // Optional: handles both "/" and "/home"
        System.out.println("🏠 Home page accessed via /home" + (name != null ? ", name parameter: " + name : ""));
        return "home";
    }
}
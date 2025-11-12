package com.mku.attendance.controllers;

import com.mku.attendance.services.HODManager;
import com.mku.attendance.services.UnitManager;
import com.mku.attendance.entities.LecturerData;
import com.mku.attendance.entities.Unit;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.*;

@Controller
@RequestMapping("/lecturer")
public class LecturerController {

    @Autowired
    private HODManager hodManager;

    @Autowired
    private UnitManager unitManager;

    /**
     * Show lecturer dashboard
     */
    @GetMapping("/dashboard")
    public String showDashboard(HttpSession session, Model model) {
        // Check if lecturer is logged in
        String lecturerId = (String) session.getAttribute("lecturerId");
        if (lecturerId == null) {
            return "redirect:/lecturer/login";
        }

        String lecturerName = (String) session.getAttribute("lecturerName");

        // Get lecturer details from HODManager
        LecturerData lecturer = hodManager.getLecturer(lecturerId);

        // Add lecturer info to model
        model.addAttribute("lecturerId", lecturerId);
        model.addAttribute("lecturerName", lecturerName);
        model.addAttribute("lecturer", lecturer);

        // FIXED: Properly handle units retrieval
        try {
            // Get units as Map and convert to List
            Map<String, Unit> unitsMap = unitManager.getUnits();
            List<Unit> unitsList = new ArrayList<>(unitsMap.values());

            model.addAttribute("units", unitsList);
            model.addAttribute("totalUnits", unitsList.size());

        } catch (Exception e) {
            System.out.println("❌ Error getting units: " + e.getMessage());
            model.addAttribute("units", List.of());
            model.addAttribute("totalUnits", 0);
        }

        // FIXED: Add missing isAttendanceActive attribute with default value
        model.addAttribute("isAttendanceActive", false);

        System.out.println("Lecturer dashboard accessed by: " + lecturerId);
        return "lecturer-dashboard";
    }

    /**
     * Show lecturer profile
     */
    @GetMapping("/profile")
    public String showProfile(HttpSession session, Model model) {
        String lecturerId = (String) session.getAttribute("lecturerId");
        if (lecturerId == null) {
            return "redirect:/lecturer/login";
        }

        String lecturerName = (String) session.getAttribute("lecturerName");

        // Get lecturer details
        LecturerData lecturer = hodManager.getLecturer(lecturerId);

        // Add lecturer info to model
        model.addAttribute("lecturerId", lecturerId);
        model.addAttribute("lecturerName", lecturerName);
        model.addAttribute("lecturer", lecturer);

        System.out.println("Lecturer profile accessed by: " + lecturerId);
        return "lecturer-profile";
    }

    /**
     * Show attendance management page
     */
    @GetMapping("/attendance")
    public String showAttendanceManagement(HttpSession session, Model model) {
        String lecturerId = (String) session.getAttribute("lecturerId");
        if (lecturerId == null) {
            return "redirect:/lecturer/login";
        }

        String lecturerName = (String) session.getAttribute("lecturerName");

        // Get lecturer details
        LecturerData lecturer = hodManager.getLecturer(lecturerId);

        model.addAttribute("lecturerId", lecturerId);
        model.addAttribute("lecturerName", lecturerName);
        model.addAttribute("lecturer", lecturer);

        // FIXED: Simplified units retrieval for attendance page
        try {
            Map<String, Unit> unitsMap = unitManager.getUnits();
            List<Unit> unitsList = new ArrayList<>(unitsMap.values());
            model.addAttribute("units", unitsList);
        } catch (Exception e) {
            model.addAttribute("units", List.of());
        }

        // FIXED: Add missing attribute
        model.addAttribute("isAttendanceActive", false);

        System.out.println("Lecturer attendance page accessed by: " + lecturerId);
        return "lecturer-attendance";
    }

    /**
     * Show reports page
     */
    @GetMapping("/reports")
    public String showReports(HttpSession session, Model model) {
        String lecturerId = (String) session.getAttribute("lecturerId");
        if (lecturerId == null) {
            return "redirect:/lecturer/login";
        }

        String lecturerName = (String) session.getAttribute("lecturerName");

        // Get lecturer details
        LecturerData lecturer = hodManager.getLecturer(lecturerId);

        model.addAttribute("lecturerId", lecturerId);
        model.addAttribute("lecturerName", lecturerName);
        model.addAttribute("lecturer", lecturer);

        System.out.println("Lecturer reports page accessed by: " + lecturerId);
        return "lecturer-reports";
    }

    /**
     * Show settings page
     */
    @GetMapping("/settings")
    public String showSettings(HttpSession session, Model model) {
        String lecturerId = (String) session.getAttribute("lecturerId");
        if (lecturerId == null) {
            return "redirect:/lecturer/login";
        }

        String lecturerName = (String) session.getAttribute("lecturerName");

        // Get lecturer details
        LecturerData lecturer = hodManager.getLecturer(lecturerId);

        model.addAttribute("lecturerId", lecturerId);
        model.addAttribute("lecturerName", lecturerName);
        model.addAttribute("lecturer", lecturer);

        System.out.println("Lecturer settings page accessed by: " + lecturerId);
        return "lecturer-settings";
    }
}
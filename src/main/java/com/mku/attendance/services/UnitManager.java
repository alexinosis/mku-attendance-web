package com.mku.attendance.services;

import com.mku.attendance.entities.Unit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class UnitManager {
    private Map<String, Unit> units = new HashMap<>();
    private final FileDataService fileDataService;

    @Autowired
    public UnitManager(FileDataService fileDataService) {
        this.fileDataService = fileDataService;

        // Load units from file on startup
        try {
            units = fileDataService.loadUnits();
            if (units == null) {
                units = new HashMap<>();
                System.out.println("No units data loaded, initializing empty unit map");
            } else {
                System.out.println("UnitManager initialized with " + units.size() + " units");
            }
        } catch (Exception e) {
            System.err.println("Error loading units: " + e.getMessage());
            units = new HashMap<>();
        }

        System.out.println("✅ UnitManager initialized with file persistence");
    }

    public void addUnit(String code, String name, String courseCode) {
        if (code != null && name != null && courseCode != null && !units.containsKey(code)) {
            units.put(code, new Unit(code, name, courseCode));
            saveUnitsToFile();
            System.out.println("✅ Unit added and saved: code=" + code + ", name=" + name + ", courseCode=" + courseCode);
        } else {
            System.out.println("❌ Unit addition failed: Unit " + code + " already exists or invalid data");
        }
    }

    public Map<String, Unit> getUnits() {
        return new HashMap<>(units);
    }

    public Map<String, Unit> getUnitDatabase() {
        return new HashMap<>(units);
    }

    // Save units to file
    public void saveUnitsToFile() {
        try {
            fileDataService.saveUnits(units);
            System.out.println("✅ Units data saved successfully");
        } catch (Exception e) {
            System.err.println("❌ Error saving units data: " + e.getMessage());
        }
    }

    // Get unit by code
    public Unit getUnit(String code) {
        return units.get(code);
    }

    // Check if unit exists
    public boolean unitExists(String code) {
        return units.containsKey(code);
    }

    // Remove unit
    public boolean removeUnit(String code) {
        if (units.containsKey(code)) {
            units.remove(code);
            saveUnitsToFile();
            System.out.println("✅ Unit removed: " + code);
            return true;
        }
        return false;
    }

    // Get units by course
    public List<Unit> getUnitsByCourse(String courseCode) {
        List<Unit> courseUnits = new ArrayList<>();
        for (Unit unit : units.values()) {
            if (unit.getCourseCode().equals(courseCode)) {
                courseUnits.add(unit);
            }
        }
        return courseUnits;
    }
}
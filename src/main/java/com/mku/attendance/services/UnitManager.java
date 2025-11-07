package com.mku.attendance.services;

import com.mku.attendance.entities.Unit;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class UnitManager {
    private static final Map<String, Unit> units = new HashMap<>();

    public UnitManager() {
        System.out.println("UnitManager initialized");
    }

    public void addUnit(String code, String name, String courseCode) {
        if (!units.containsKey(code)) {
            units.put(code, new Unit(code, name, courseCode));
            System.out.println("Unit added: code=" + code + ", name=" + name + ", courseCode=" + courseCode);
        } else {
            System.out.println("Unit addition failed: Unit " + code + " already exists");
        }
    }

    public Map<String, Unit> getUnits() {
        return units;
    }

    public static Map<String, Unit> getUnitDatabase() {
        return units;
    }
}
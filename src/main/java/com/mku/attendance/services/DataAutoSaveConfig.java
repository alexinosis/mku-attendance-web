package com.mku.attendance.config;

import com.mku.attendance.services.FileDataService;
import com.mku.attendance.services.HODManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class DataAutoSaveConfig {

    @Autowired
    private FileDataService fileDataService;

    @Autowired
    private HODManager hodManager;

    // Auto-save HODs data every 5 minutes
    @Scheduled(fixedRate = 300000) // 300,000 ms = 5 minutes
    public void autoSaveHODs() {
        try {
            fileDataService.saveHODs(hodManager.getHODs());
            System.out.println("Auto-save: HODs data saved successfully");
        } catch (Exception e) {
            System.err.println("Auto-save error: " + e.getMessage());
        }
    }
}
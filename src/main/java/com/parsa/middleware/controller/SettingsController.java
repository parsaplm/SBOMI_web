package com.parsa.middleware.controller;

import com.parsa.middleware.config.ConfigProperties;
import com.parsa.middleware.config.StatusColorConfig;
import com.parsa.middleware.enums.ImportStatus;
import com.parsa.middleware.model.StatusColor;
import com.parsa.middleware.service.SettingsService;
import com.parsa.middleware.service.StatusColorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/settings")
public class SettingsController {
    private final SettingsService settingsService;
    private final ConfigProperties configProperties;

    @Autowired
    private StatusColorService statusColorService;


    @Autowired
    private StatusColorConfig statusColorProperties;

    @Autowired
    public SettingsController(SettingsService settingsService, ConfigProperties configProperties) {
        this.settingsService = settingsService;
        this.configProperties = configProperties;
    }

    @PostMapping("/update")
    public String updateSettings(@RequestBody ConfigProperties newConfig) {
        settingsService.updateSettings(newConfig);
        settingsService.triggerReload();
        return "Settings updated successfully!";
    }

    @GetMapping("/config")
    public Map<String, String> getAllConfigurations() {
        Map<String, String> configurations = new HashMap<>();

        // Add more properties as needed
        return  settingsService.getAllConfigurations();
    }

    @GetMapping("/getAllStatus")
    public ImportStatus[] getAllStatus() {
        return ImportStatus.values();
    }

    @GetMapping("/status-colors")
    public List<StatusColor> getStatusColors() {
        return statusColorService.getAllStatusColors();
    }

    @GetMapping("/status-colors/{status}")
    public StatusColor getStatusColor(@PathVariable String status) {
        return statusColorService.getStatusColor(status)
                .orElseThrow(() -> new RuntimeException("Status not found"));
    }

    @PutMapping("/status-colors/{status}")
    public StatusColor updateStatusColor(@PathVariable String status, @RequestBody String color) {
        return statusColorService.updateStatusColor(status, color);
    }

    @PostMapping("/status-colors/add")
    public List<StatusColor> addStatusColors(@RequestBody List<StatusColor> statusColors) {
        return statusColorService.addStatusColors(statusColors);
    }


}
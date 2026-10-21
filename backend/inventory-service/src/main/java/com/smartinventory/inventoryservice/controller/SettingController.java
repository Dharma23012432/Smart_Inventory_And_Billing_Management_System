package com.smartinventory.inventoryservice.controller;

import com.smartinventory.inventoryservice.dto.ApiResponse;
import com.smartinventory.inventoryservice.dto.SettingResponseDTO;
import com.smartinventory.inventoryservice.service.SettingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller exposing application settings configuration endpoints.
 * Strictly adheres to Section 3.12 of API_SPECIFICATION.md with consistent /api/v1 prefix.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/settings")
public class SettingController {

    private final SettingService settingService;

    public SettingController(SettingService settingService) {
        this.settingService = settingService;
    }

    /**
     * GET /api/v1/settings
     * Fetches all application profile and configuration settings as a key-value map.
     *
     * @return Standardized ApiResponse with settings dictionary
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> getAllSettings() {
        log.info("REST request to get all settings");

        Map<String, String> response = settingService.getAllSettings();
        return ResponseEntity.ok(ApiResponse.success("Settings retrieved", response));
    }

    /**
     * GET /api/v1/settings/{key}
     * Retrieves an individual setting by key.
     *
     * @param key Setting identifier
     * @return Standardized ApiResponse with setting details
     */
    @GetMapping("/{key}")
    public ResponseEntity<ApiResponse<SettingResponseDTO>> getSettingByKey(@PathVariable String key) {
        log.info("REST request to get setting by key: {}", key);

        SettingResponseDTO response = settingService.getSettingByKey(key);
        return ResponseEntity.ok(ApiResponse.success("Setting retrieved", response));
    }

    /**
     * PUT /api/v1/settings
     * Updates multiple application settings in batch.
     *
     * @param settingsMap Key-value pairs to update
     * @return Standardized ApiResponse with updated settings dictionary
     */
    @PutMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> updateSettings(
            @RequestBody Map<String, String> settingsMap) {

        log.info("REST request to update settings in batch");

        Map<String, String> response = settingService.updateSettings(settingsMap);
        return ResponseEntity.ok(ApiResponse.success("Settings updated successfully", response));
    }
}

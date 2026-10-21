package com.smartinventory.inventoryservice.service;

import com.smartinventory.inventoryservice.dto.SettingResponseDTO;

import java.util.Map;

/**
 * Service interface defining runtime application settings operations.
 * Strictly adheres to Section 3.12 of API_SPECIFICATION.md.
 */
public interface SettingService {

    /**
     * Retrieves all system settings as a key-value dictionary.
     *
     * @return Map of setting keys to values
     */
    Map<String, String> getAllSettings();

    /**
     * Retrieves an individual setting by key.
     *
     * @param key Setting key
     * @return SettingResponseDTO
     */
    SettingResponseDTO getSettingByKey(String key);

    /**
     * Retrieves setting string value with a default fallback.
     *
     * @param key          Setting key
     * @param defaultValue Default fallback if key not found
     * @return Setting value or defaultValue
     */
    String getSettingValue(String key, String defaultValue);

    /**
     * Updates multiple settings in batch.
     *
     * @param settingsMap Key-value pairs to persist
     * @return Updated settings dictionary
     */
    Map<String, String> updateSettings(Map<String, String> settingsMap);
}

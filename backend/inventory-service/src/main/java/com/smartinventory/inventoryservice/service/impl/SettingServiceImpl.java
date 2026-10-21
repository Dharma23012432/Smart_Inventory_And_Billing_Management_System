package com.smartinventory.inventoryservice.service.impl;

import com.smartinventory.inventoryservice.dto.SettingResponseDTO;
import com.smartinventory.inventoryservice.entity.Setting;
import com.smartinventory.inventoryservice.exception.ResourceNotFoundException;
import com.smartinventory.inventoryservice.repository.SettingRepository;
import com.smartinventory.inventoryservice.service.SettingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Production implementation of SettingService.
 * Enforces key-value application configuration, layered architecture, constructor injection, and SLF4J logging.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class SettingServiceImpl implements SettingService {

    private final SettingRepository settingRepository;

    public SettingServiceImpl(SettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    @Override
    public Map<String, String> getAllSettings() {
        log.info("Fetching all application settings");
        List<Setting> settingsList = settingRepository.findAll();

        Map<String, String> result = new LinkedHashMap<>();
        for (Setting setting : settingsList) {
            result.put(setting.getKey(), setting.getValue());
        }
        return result;
    }

    @Override
    public SettingResponseDTO getSettingByKey(String key) {
        log.info("Fetching setting with key: {}", key);

        Setting setting = settingRepository.findByKey(key)
                .orElseThrow(() -> new ResourceNotFoundException("Setting not found with key: " + key));

        return SettingResponseDTO.builder()
                .key(setting.getKey())
                .value(setting.getValue())
                .description(setting.getDescription())
                .build();
    }

    @Override
    public String getSettingValue(String key, String defaultValue) {
        return settingRepository.findByKey(key)
                .map(Setting::getValue)
                .orElse(defaultValue);
    }

    @Override
    @Transactional
    public Map<String, String> updateSettings(Map<String, String> settingsMap) {
        log.info("Updating {} settings in batch", settingsMap != null ? settingsMap.size() : 0);

        if (settingsMap == null || settingsMap.isEmpty()) {
            return getAllSettings();
        }

        for (Map.Entry<String, String> entry : settingsMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            Optional<Setting> existingOpt = settingRepository.findByKey(key);
            if (existingOpt.isPresent()) {
                Setting setting = existingOpt.get();
                setting.setValue(value);
                settingRepository.save(setting);
            } else {
                Setting newSetting = Setting.builder()
                        .key(key)
                        .value(value)
                        .build();
                settingRepository.save(newSetting);
            }
        }

        return getAllSettings();
    }
}

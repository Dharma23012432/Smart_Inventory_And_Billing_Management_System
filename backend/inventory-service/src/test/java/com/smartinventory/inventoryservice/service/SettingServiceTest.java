package com.smartinventory.inventoryservice.service;

import com.smartinventory.inventoryservice.dto.SettingResponseDTO;
import com.smartinventory.inventoryservice.entity.Setting;
import com.smartinventory.inventoryservice.exception.ResourceNotFoundException;
import com.smartinventory.inventoryservice.repository.SettingRepository;
import com.smartinventory.inventoryservice.service.impl.SettingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SettingService Unit Tests")
class SettingServiceTest {

    @Mock
    private SettingRepository settingRepository;

    @InjectMocks
    private SettingServiceImpl settingService;

    private Setting sampleSetting;

    @BeforeEach
    void setUp() {
        sampleSetting = Setting.builder()
                .id(1L)
                .key("company_name")
                .value("Apex Retail Solutions")
                .description("Business legal name")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("getAllSettings: Should return all settings mapped as key-value pairs")
    void getAllSettings_Success() {
        when(settingRepository.findAll()).thenReturn(Collections.singletonList(sampleSetting));

        Map<String, String> result = settingService.getAllSettings();

        assertThat(result).isNotNull();
        assertThat(result).containsEntry("company_name", "Apex Retail Solutions");
    }

    @Test
    @DisplayName("getSettingByKey: Should return DTO when key exists")
    void getSettingByKey_Success() {
        when(settingRepository.findByKey("company_name")).thenReturn(Optional.of(sampleSetting));

        SettingResponseDTO result = settingService.getSettingByKey("company_name");

        assertThat(result).isNotNull();
        assertThat(result.getKey()).isEqualTo("company_name");
        assertThat(result.getValue()).isEqualTo("Apex Retail Solutions");
    }

    @Test
    @DisplayName("getSettingByKey: Should throw ResourceNotFoundException when key not found")
    void getSettingByKey_NotFound() {
        when(settingRepository.findByKey("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> settingService.getSettingByKey("unknown"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Setting not found with key: unknown");
    }

    @Test
    @DisplayName("getSettingValue: Should return fallback default when key not found")
    void getSettingValue_DefaultFallback() {
        when(settingRepository.findByKey("unknown")).thenReturn(Optional.empty());

        String val = settingService.getSettingValue("unknown", "default_val");

        assertThat(val).isEqualTo("default_val");
    }

    @Test
    @DisplayName("updateSettings: Should update existing settings and create new ones")
    void updateSettings_Success() {
        Map<String, String> updates = new HashMap<>();
        updates.put("company_name", "Apex New Ltd");
        updates.put("new_key", "new_val");

        when(settingRepository.findByKey("company_name")).thenReturn(Optional.of(sampleSetting));
        when(settingRepository.findByKey("new_key")).thenReturn(Optional.empty());
        when(settingRepository.findAll()).thenReturn(Arrays.asList(
                Setting.builder().key("company_name").value("Apex New Ltd").build(),
                Setting.builder().key("new_key").value("new_val").build()
        ));

        Map<String, String> result = settingService.updateSettings(updates);

        assertThat(result).isNotNull();
        assertThat(result).containsEntry("company_name", "Apex New Ltd");
        assertThat(result).containsEntry("new_key", "new_val");
        verify(settingRepository, times(2)).save(any(Setting.class));
    }
}

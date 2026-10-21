package com.smartinventory.inventoryservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartinventory.inventoryservice.dto.SettingResponseDTO;
import com.smartinventory.inventoryservice.exception.GlobalExceptionHandler;
import com.smartinventory.inventoryservice.exception.ResourceNotFoundException;
import com.smartinventory.inventoryservice.service.SettingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SettingController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("SettingController WebMvc Tests")
class SettingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SettingService settingService;

    @Test
    @DisplayName("GET /api/v1/settings: Should return 200 with dictionary")
    void getAllSettings_Success() throws Exception {
        Map<String, String> settings = Collections.singletonMap("company_name", "Apex Retail Solutions");
        when(settingService.getAllSettings()).thenReturn(settings);

        mockMvc.perform(get("/api/v1/settings")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Settings retrieved")))
                .andExpect(jsonPath("$.data.company_name", is("Apex Retail Solutions")));
    }

    @Test
    @DisplayName("GET /api/v1/settings/{key}: Should return 200 with setting details")
    void getSettingByKey_Success() throws Exception {
        SettingResponseDTO response = SettingResponseDTO.builder()
                .key("company_name")
                .value("Apex Retail Solutions")
                .description("Business name")
                .build();
        when(settingService.getSettingByKey("company_name")).thenReturn(response);

        mockMvc.perform(get("/api/v1/settings/company_name")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.key", is("company_name")))
                .andExpect(jsonPath("$.data.value", is("Apex Retail Solutions")));
    }

    @Test
    @DisplayName("GET /api/v1/settings/{key}: Should return 404 when key not found")
    void getSettingByKey_NotFound() throws Exception {
        when(settingService.getSettingByKey("unknown"))
                .thenThrow(new ResourceNotFoundException("Setting not found with key: unknown"));

        mockMvc.perform(get("/api/v1/settings/unknown")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("RESOURCE_NOT_FOUND")));
    }

    @Test
    @DisplayName("PUT /api/v1/settings: Should return 200 with updated settings")
    void updateSettings_Success() throws Exception {
        Map<String, String> updates = new HashMap<>();
        updates.put("company_name", "Apex New Ltd");

        when(settingService.updateSettings(updates)).thenReturn(updates);

        mockMvc.perform(put("/api/v1/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.company_name", is("Apex New Ltd")));
    }
}

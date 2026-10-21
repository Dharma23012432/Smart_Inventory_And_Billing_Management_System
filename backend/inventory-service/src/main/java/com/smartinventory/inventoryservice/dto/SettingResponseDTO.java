package com.smartinventory.inventoryservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO representing an individual application setting.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettingResponseDTO {

    private String key;

    private String value;

    private String description;
}

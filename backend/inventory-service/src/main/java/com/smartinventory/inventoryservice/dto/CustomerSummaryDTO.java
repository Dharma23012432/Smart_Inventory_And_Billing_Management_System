package com.smartinventory.inventoryservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Summary DTO of Customer included within Bill responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerSummaryDTO {

    private Long id;

    private String name;

    private String email;
}

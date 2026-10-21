package com.smartinventory.inventoryservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Summary DTO of Category included within Product responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategorySummaryDTO {

    private Long id;

    private String name;
}

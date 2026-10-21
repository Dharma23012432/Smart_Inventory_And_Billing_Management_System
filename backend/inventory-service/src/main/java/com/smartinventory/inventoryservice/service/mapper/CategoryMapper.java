package com.smartinventory.inventoryservice.service.mapper;

import com.smartinventory.inventoryservice.dto.CategoryRequestDTO;
import com.smartinventory.inventoryservice.dto.CategoryResponseDTO;
import com.smartinventory.inventoryservice.entity.Category;
import org.springframework.stereotype.Component;

/**
 * Component for bidirectional mapping between Category entity and DTOs.
 */
@Component
public class CategoryMapper {

    /**
     * Converts Category entity to CategoryResponseDTO.
     */
    public CategoryResponseDTO toResponseDTO(Category category) {
        if (category == null) {
            return null;
        }

        return CategoryResponseDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }

    /**
     * Converts CategoryRequestDTO to a new Category entity.
     */
    public Category toEntity(CategoryRequestDTO requestDTO) {
        if (requestDTO == null) {
            return null;
        }

        return Category.builder()
                .name(requestDTO.getName() != null ? requestDTO.getName().trim() : null)
                .description(requestDTO.getDescription() != null ? requestDTO.getDescription().trim() : null)
                .build();
    }

    /**
     * Updates an existing Category entity with values from CategoryRequestDTO.
     */
    public void updateEntityFromDTO(CategoryRequestDTO requestDTO, Category entity) {
        if (requestDTO == null || entity == null) {
            return;
        }

        if (requestDTO.getName() != null) {
            entity.setName(requestDTO.getName().trim());
        }
        if (requestDTO.getDescription() != null) {
            entity.setDescription(requestDTO.getDescription().trim());
        }
    }
}

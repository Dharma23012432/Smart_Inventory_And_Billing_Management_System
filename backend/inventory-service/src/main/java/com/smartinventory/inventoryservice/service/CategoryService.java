package com.smartinventory.inventoryservice.service;

import com.smartinventory.inventoryservice.dto.CategoryRequestDTO;
import com.smartinventory.inventoryservice.dto.CategoryResponseDTO;

import java.util.List;

/**
 * Service interface defining business operations for Category management.
 */
public interface CategoryService {

    /**
     * Retrieves all categories sorted alphabetically by name.
     *
     * @return List of CategoryResponseDTOs
     */
    List<CategoryResponseDTO> getAllCategories();

    /**
     * Retrieves a single category by its primary key.
     *
     * @param id Category ID
     * @return CategoryResponseDTO
     * @throws com.smartinventory.inventoryservice.exception.ResourceNotFoundException if not found
     */
    CategoryResponseDTO getCategoryById(Long id);

    /**
     * Creates a new category after validating unique name constraints.
     *
     * @param requestDTO Category creation payload
     * @return CategoryResponseDTO
     * @throws com.smartinventory.inventoryservice.exception.DuplicateResourceException if name already exists
     */
    CategoryResponseDTO createCategory(CategoryRequestDTO requestDTO);

    /**
     * Updates an existing category's name and description.
     *
     * @param id         Category ID
     * @param requestDTO Category update payload
     * @return CategoryResponseDTO
     * @throws com.smartinventory.inventoryservice.exception.ResourceNotFoundException if not found
     * @throws com.smartinventory.inventoryservice.exception.DuplicateResourceException if name belongs to another category
     */
    CategoryResponseDTO updateCategory(Long id, CategoryRequestDTO requestDTO);

    /**
     * Deletes a category by ID if no products are associated with it.
     *
     * @param id Category ID
     * @throws com.smartinventory.inventoryservice.exception.ResourceNotFoundException if not found
     * @throws com.smartinventory.inventoryservice.exception.ResourceConflictException if products are attached
     */
    void deleteCategory(Long id);
}

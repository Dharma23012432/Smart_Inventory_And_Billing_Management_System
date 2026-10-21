package com.smartinventory.inventoryservice.controller;

import com.smartinventory.inventoryservice.dto.ApiResponse;
import com.smartinventory.inventoryservice.dto.CategoryRequestDTO;
import com.smartinventory.inventoryservice.dto.CategoryResponseDTO;
import com.smartinventory.inventoryservice.service.CategoryService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller exposing Category management endpoints.
 * Strictly adheres to Section 3.1 of API_SPECIFICATION.md.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * Constructor injection for CategoryService.
     */
    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * GET /api/categories
     * Retrieves all categories sorted alphabetically by name.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponseDTO>>> getAllCategories() {
        log.info("REST request to get all categories");
        List<CategoryResponseDTO> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.success("Categories retrieved successfully", categories));
    }

    /**
     * GET /api/categories/{id}
     * Retrieves a single category by primary key.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponseDTO>> getCategoryById(@PathVariable Long id) {
        log.info("REST request to get category with ID: {}", id);
        CategoryResponseDTO category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(ApiResponse.success("Category retrieved successfully", category));
    }

    /**
     * POST /api/categories
     * Creates a new category.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponseDTO>> createCategory(
            @Valid @RequestBody CategoryRequestDTO requestDTO) {

        log.info("REST request to create category: '{}'", requestDTO.getName());
        CategoryResponseDTO createdCategory = categoryService.createCategory(requestDTO);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Category created successfully", createdCategory));
    }

    /**
     * PUT /api/categories/{id}
     * Updates an existing category's name and description.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponseDTO>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequestDTO requestDTO) {

        log.info("REST request to update category with ID: {}", id);
        CategoryResponseDTO updatedCategory = categoryService.updateCategory(id, requestDTO);

        return ResponseEntity.ok(ApiResponse.success("Category updated successfully", updatedCategory));
    }

    /**
     * DELETE /api/categories/{id}
     * Deletes a category if not referenced by products.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        log.info("REST request to delete category with ID: {}", id);
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success("Category deleted successfully", null));
    }
}

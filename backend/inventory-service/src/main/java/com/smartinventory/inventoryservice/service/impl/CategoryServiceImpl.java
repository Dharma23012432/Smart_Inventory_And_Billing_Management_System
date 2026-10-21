package com.smartinventory.inventoryservice.service.impl;

import com.smartinventory.inventoryservice.dto.CategoryRequestDTO;
import com.smartinventory.inventoryservice.dto.CategoryResponseDTO;
import com.smartinventory.inventoryservice.entity.Category;
import com.smartinventory.inventoryservice.exception.DuplicateResourceException;
import com.smartinventory.inventoryservice.exception.ResourceNotFoundException;
import com.smartinventory.inventoryservice.repository.CategoryRepository;
import com.smartinventory.inventoryservice.service.CategoryService;
import com.smartinventory.inventoryservice.service.mapper.CategoryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Production implementation of CategoryService.
 * Uses constructor injection and SLF4J logging.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    /**
     * Constructor injection for required dependencies.
     */
    public CategoryServiceImpl(CategoryRepository categoryRepository, CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
    }

    @Override
    public List<CategoryResponseDTO> getAllCategories() {
        log.info("Fetching all categories sorted by name");
        List<Category> categories = categoryRepository.findAllByOrderByNameAsc();
        log.debug("Retrieved {} categories from database", categories.size());

        return categories.stream()
                .map(categoryMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryResponseDTO getCategoryById(Long id) {
        log.info("Fetching category with ID: {}", id);
        Category category = findCategoryOrThrow(id);
        return categoryMapper.toResponseDTO(category);
    }

    @Override
    @Transactional
    public CategoryResponseDTO createCategory(CategoryRequestDTO requestDTO) {
        String trimmedName = requestDTO.getName().trim();
        log.info("Creating category with name: '{}'", trimmedName);

        if (categoryRepository.existsByNameIgnoreCase(trimmedName)) {
            log.warn("Category creation rejected — name '{}' already exists", trimmedName);
            throw new DuplicateResourceException("Category", "name", trimmedName);
        }

        Category category = categoryMapper.toEntity(requestDTO);
        Category savedCategory = categoryRepository.save(category);
        log.info("Category created successfully with ID: {}", savedCategory.getId());

        return categoryMapper.toResponseDTO(savedCategory);
    }

    @Override
    @Transactional
    public CategoryResponseDTO updateCategory(Long id, CategoryRequestDTO requestDTO) {
        String trimmedName = requestDTO.getName().trim();
        log.info("Updating category with ID: {}, new name: '{}'", id, trimmedName);

        Category category = findCategoryOrThrow(id);

        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(trimmedName, id)) {
            log.warn("Category update rejected — name '{}' already belongs to another category", trimmedName);
            throw new DuplicateResourceException("Category", "name", trimmedName);
        }

        categoryMapper.updateEntityFromDTO(requestDTO, category);
        Category updatedCategory = categoryRepository.save(category);
        log.info("Category with ID: {} updated successfully", id);

        return categoryMapper.toResponseDTO(updatedCategory);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        log.info("Attempting to delete category with ID: {}", id);
        Category category = findCategoryOrThrow(id);

        categoryRepository.delete(category);
        categoryRepository.flush();
        log.info("Category with ID: {} deleted successfully", id);
    }

    // -----------------------------------------------------------------
    // Internal Helper Methods
    // -----------------------------------------------------------------

    private Category findCategoryOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Category with ID: {} not found", id);
                    return new ResourceNotFoundException("Category", "id", id);
                });
    }
}

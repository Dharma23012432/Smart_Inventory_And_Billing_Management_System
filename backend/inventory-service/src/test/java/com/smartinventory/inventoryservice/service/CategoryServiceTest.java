package com.smartinventory.inventoryservice.service;

import com.smartinventory.inventoryservice.dto.CategoryRequestDTO;
import com.smartinventory.inventoryservice.dto.CategoryResponseDTO;
import com.smartinventory.inventoryservice.entity.Category;
import com.smartinventory.inventoryservice.exception.DuplicateResourceException;
import com.smartinventory.inventoryservice.exception.ResourceNotFoundException;
import com.smartinventory.inventoryservice.repository.CategoryRepository;
import com.smartinventory.inventoryservice.service.impl.CategoryServiceImpl;
import com.smartinventory.inventoryservice.service.mapper.CategoryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService Unit Tests")
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Spy
    private CategoryMapper categoryMapper = new CategoryMapper();

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category testCategory;
    private CategoryRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        testCategory = Category.builder()
                .id(1L)
                .name("Electronics")
                .description("Electronic gadgets and devices")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        requestDTO = CategoryRequestDTO.builder()
                .name("Electronics")
                .description("Electronic gadgets and devices")
                .build();
    }

    @Test
    @DisplayName("getAllCategories should return sorted list of CategoryResponseDTOs")
    void getAllCategories_shouldReturnListOfCategories() {
        Category cat2 = Category.builder()
                .id(2L)
                .name("Stationery")
                .description("Office supplies")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(categoryRepository.findAllByOrderByNameAsc()).thenReturn(Arrays.asList(testCategory, cat2));

        List<CategoryResponseDTO> result = categoryService.getAllCategories();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Electronics");
        assertThat(result.get(1).getName()).isEqualTo("Stationery");
        verify(categoryRepository, times(1)).findAllByOrderByNameAsc();
    }

    @Test
    @DisplayName("getCategoryById should return DTO when category exists")
    void getCategoryById_whenFound_shouldReturnDTO() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));

        CategoryResponseDTO result = categoryService.getCategoryById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Electronics");
        verify(categoryRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("getCategoryById should throw ResourceNotFoundException when not found")
    void getCategoryById_whenNotFound_shouldThrowException() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getCategoryById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category with id '99' was not found");
        verify(categoryRepository, times(1)).findById(99L);
    }

    @Test
    @DisplayName("createCategory should successfully save and return CategoryResponseDTO")
    void createCategory_whenValid_shouldCreateAndReturnDTO() {
        when(categoryRepository.existsByNameIgnoreCase("Electronics")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);

        CategoryResponseDTO result = categoryService.createCategory(requestDTO);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Electronics");
        verify(categoryRepository, times(1)).existsByNameIgnoreCase("Electronics");
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    @DisplayName("createCategory should throw DuplicateResourceException when name already exists")
    void createCategory_whenDuplicateName_shouldThrowException() {
        when(categoryRepository.existsByNameIgnoreCase("Electronics")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory(requestDTO))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Category with name 'Electronics' already exists");

        verify(categoryRepository, times(1)).existsByNameIgnoreCase("Electronics");
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("updateCategory should successfully modify and return updated DTO")
    void updateCategory_whenValid_shouldUpdateAndReturnDTO() {
        CategoryRequestDTO updateDTO = CategoryRequestDTO.builder()
                .name("Consumer Electronics")
                .description("Updated description")
                .build();

        Category updatedCategory = Category.builder()
                .id(1L)
                .name("Consumer Electronics")
                .description("Updated description")
                .createdAt(testCategory.getCreatedAt())
                .updatedAt(Instant.now())
                .build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("Consumer Electronics", 1L)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(updatedCategory);

        CategoryResponseDTO result = categoryService.updateCategory(1L, updateDTO);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Consumer Electronics");
        verify(categoryRepository, times(1)).findById(1L);
        verify(categoryRepository, times(1)).save(testCategory);
    }

    @Test
    @DisplayName("updateCategory should throw DuplicateResourceException when name belongs to another category")
    void updateCategory_whenDuplicateName_shouldThrowException() {
        CategoryRequestDTO updateDTO = CategoryRequestDTO.builder()
                .name("Stationery")
                .build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("Stationery", 1L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.updateCategory(1L, updateDTO))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Category with name 'Stationery' already exists");

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("deleteCategory should delete successfully when category exists")
    void deleteCategory_whenFound_shouldDelete() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        doNothing().when(categoryRepository).delete(testCategory);
        doNothing().when(categoryRepository).flush();

        categoryService.deleteCategory(1L);

        verify(categoryRepository, times(1)).findById(1L);
        verify(categoryRepository, times(1)).delete(testCategory);
    }

    @Test
    @DisplayName("deleteCategory should throw ResourceNotFoundException when category does not exist")
    void deleteCategory_whenNotFound_shouldThrowException() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.deleteCategory(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(categoryRepository, never()).delete(any(Category.class));
    }
}

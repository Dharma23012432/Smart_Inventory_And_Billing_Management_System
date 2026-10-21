package com.smartinventory.inventoryservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartinventory.inventoryservice.dto.CategoryRequestDTO;
import com.smartinventory.inventoryservice.dto.CategoryResponseDTO;
import com.smartinventory.inventoryservice.exception.DuplicateResourceException;
import com.smartinventory.inventoryservice.exception.GlobalExceptionHandler;
import com.smartinventory.inventoryservice.exception.ResourceNotFoundException;
import com.smartinventory.inventoryservice.service.CategoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("CategoryController WebMvc Tests")
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

    @Test
    @DisplayName("GET /api/v1/categories should return 200 OK and category list")
    void getAllCategories_shouldReturn200() throws Exception {
        CategoryResponseDTO responseDTO = CategoryResponseDTO.builder()
                .id(1L)
                .name("Electronics")
                .description("Devices")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(categoryService.getAllCategories()).thenReturn(Collections.singletonList(responseDTO));

        mockMvc.perform(get("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("Categories retrieved")))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name", is("Electronics")));

        verify(categoryService, times(1)).getAllCategories();
    }

    @Test
    @DisplayName("GET /api/v1/categories/{id} should return 200 OK when category exists")
    void getCategoryById_whenFound_shouldReturn200() throws Exception {
        CategoryResponseDTO responseDTO = CategoryResponseDTO.builder()
                .id(1L)
                .name("Electronics")
                .description("Devices")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(categoryService.getCategoryById(1L)).thenReturn(responseDTO);

        mockMvc.perform(get("/api/v1/categories/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(1)))
                .andExpect(jsonPath("$.data.name", is("Electronics")));

        verify(categoryService, times(1)).getCategoryById(1L);
    }

    @Test
    @DisplayName("GET /api/v1/categories/{id} should return 404 NOT_FOUND when category does not exist")
    void getCategoryById_whenNotFound_shouldReturn404() throws Exception {
        when(categoryService.getCategoryById(99L))
                .thenThrow(new ResourceNotFoundException("Category", "id", 99L));

        mockMvc.perform(get("/api/v1/categories/99")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("RESOURCE_NOT_FOUND")))
                .andExpect(jsonPath("$.error.message", containsString("Category with id '99' was not found")));
    }

    @Test
    @DisplayName("POST /api/v1/categories should return 201 CREATED when request is valid")
    void createCategory_whenValid_shouldReturn201() throws Exception {
        CategoryRequestDTO requestDTO = CategoryRequestDTO.builder()
                .name("Books")
                .description("Printed books and novels")
                .build();

        CategoryResponseDTO responseDTO = CategoryResponseDTO.builder()
                .id(10L)
                .name("Books")
                .description("Printed books and novels")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(categoryService.createCategory(any(CategoryRequestDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("Category created successfully")))
                .andExpect(jsonPath("$.data.id", is(10)))
                .andExpect(jsonPath("$.data.name", is("Books")));

        verify(categoryService, times(1)).createCategory(any(CategoryRequestDTO.class));
    }

    @Test
    @DisplayName("POST /api/v1/categories should return 400 BAD_REQUEST when name is blank")
    void createCategory_whenNameBlank_shouldReturn400() throws Exception {
        CategoryRequestDTO invalidRequest = CategoryRequestDTO.builder()
                .name("   ")
                .description("Some description")
                .build();

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.error.details", hasSize(greaterThanOrEqualTo(1))));

        verify(categoryService, never()).createCategory(any(CategoryRequestDTO.class));
    }

    @Test
    @DisplayName("POST /api/v1/categories should return 409 CONFLICT when name already exists")
    void createCategory_whenDuplicateName_shouldReturn409() throws Exception {
        CategoryRequestDTO requestDTO = CategoryRequestDTO.builder()
                .name("Electronics")
                .build();

        when(categoryService.createCategory(any(CategoryRequestDTO.class)))
                .thenThrow(new DuplicateResourceException("Category", "name", "Electronics"));

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("DUPLICATE_RESOURCE")))
                .andExpect(jsonPath("$.error.message", containsString("already exists")));
    }

    @Test
    @DisplayName("PUT /api/v1/categories/{id} should return 200 OK when update is valid")
    void updateCategory_whenValid_shouldReturn200() throws Exception {
        CategoryRequestDTO requestDTO = CategoryRequestDTO.builder()
                .name("Consumer Electronics")
                .description("Updated")
                .build();

        CategoryResponseDTO responseDTO = CategoryResponseDTO.builder()
                .id(1L)
                .name("Consumer Electronics")
                .description("Updated")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(categoryService.updateCategory(eq(1L), any(CategoryRequestDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(put("/api/v1/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is("Consumer Electronics")));

        verify(categoryService, times(1)).updateCategory(eq(1L), any(CategoryRequestDTO.class));
    }

    @Test
    @DisplayName("DELETE /api/v1/categories/{id} should return 200 OK")
    void deleteCategory_whenValid_shouldReturn200() throws Exception {
        doNothing().when(categoryService).deleteCategory(1L);

        mockMvc.perform(delete("/api/v1/categories/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("Category deleted successfully")));

        verify(categoryService, times(1)).deleteCategory(1L);
    }
}

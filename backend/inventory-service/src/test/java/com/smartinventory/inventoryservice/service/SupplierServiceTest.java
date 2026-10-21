package com.smartinventory.inventoryservice.service;

import com.smartinventory.inventoryservice.dto.PageResponse;
import com.smartinventory.inventoryservice.dto.SupplierRequestDTO;
import com.smartinventory.inventoryservice.dto.SupplierResponseDTO;
import com.smartinventory.inventoryservice.entity.Supplier;
import com.smartinventory.inventoryservice.entity.enums.SupplierStatus;
import com.smartinventory.inventoryservice.exception.DuplicateResourceException;
import com.smartinventory.inventoryservice.exception.ResourceNotFoundException;
import com.smartinventory.inventoryservice.repository.SupplierRepository;
import com.smartinventory.inventoryservice.service.impl.SupplierServiceImpl;
import com.smartinventory.inventoryservice.service.mapper.SupplierMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SupplierService Unit Tests")
class SupplierServiceTest {

    @Mock
    private SupplierRepository supplierRepository;

    @Spy
    private SupplierMapper supplierMapper = new SupplierMapper();

    @InjectMocks
    private SupplierServiceImpl supplierService;

    private Supplier testSupplier;
    private SupplierRequestDTO createRequest;

    @BeforeEach
    void setUp() {
        testSupplier = Supplier.builder()
                .id(1L)
                .name("TechSource Distribution Ltd")
                .contactPerson("Rajesh Sharma")
                .email("orders@techsource.com")
                .phone("+91 98765 43210")
                .address("100 Industrial Area, Phase 2, Bangalore")
                .status(SupplierStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        createRequest = SupplierRequestDTO.builder()
                .name("TechSource Distribution Ltd")
                .contactPerson("Rajesh Sharma")
                .email("orders@techsource.com")
                .phone("+91 98765 43210")
                .address("100 Industrial Area, Phase 2, Bangalore")
                .status("ACTIVE")
                .build();
    }

    @Test
    @DisplayName("getSuppliers should return paginated response")
    void getSuppliers_shouldReturnPagedResponse() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Supplier> page = new PageImpl<>(Collections.singletonList(testSupplier), pageable, 1);

        when(supplierRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        PageResponse<SupplierResponseDTO> response = supplierService.getSuppliers("tech", "ACTIVE", pageable);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getName()).isEqualTo("TechSource Distribution Ltd");
        verify(supplierRepository, times(1)).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("getAllActiveSuppliers should return active suppliers list")
    void getAllActiveSuppliers_shouldReturnList() {
        Supplier inactiveSupplier = Supplier.builder()
                .id(2L)
                .name("Old Supplies")
                .email("old@supplies.com")
                .status(SupplierStatus.INACTIVE)
                .build();

        when(supplierRepository.findAllByOrderByNameAsc()).thenReturn(Arrays.asList(testSupplier, inactiveSupplier));

        List<SupplierResponseDTO> response = supplierService.getAllActiveSuppliers();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getId()).isEqualTo(1L);
        verify(supplierRepository, times(1)).findAllByOrderByNameAsc();
    }

    @Test
    @DisplayName("getSupplierById when exists should return response")
    void getSupplierById_whenExists_shouldReturn() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));

        SupplierResponseDTO response = supplierService.getSupplierById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("orders@techsource.com");
        verify(supplierRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("getSupplierById when not found should throw ResourceNotFoundException")
    void getSupplierById_whenNotFound_shouldThrow() {
        when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplierService.getSupplierById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Supplier with id '99' was not found");
    }

    @Test
    @DisplayName("createSupplier success should save and return response")
    void createSupplier_success() {
        when(supplierRepository.existsByEmailIgnoreCase("orders@techsource.com")).thenReturn(false);
        when(supplierRepository.save(any(Supplier.class))).thenReturn(testSupplier);

        SupplierResponseDTO response = supplierService.createSupplier(createRequest);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("orders@techsource.com");
        verify(supplierRepository, times(1)).existsByEmailIgnoreCase("orders@techsource.com");
        verify(supplierRepository, times(1)).save(any(Supplier.class));
    }

    @Test
    @DisplayName("createSupplier when email already exists should throw DuplicateResourceException")
    void createSupplier_whenEmailExists_shouldThrow() {
        when(supplierRepository.existsByEmailIgnoreCase("orders@techsource.com")).thenReturn(true);

        assertThatThrownBy(() -> supplierService.createSupplier(createRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Supplier with email 'orders@techsource.com' already exists");

        verify(supplierRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateSupplier success should update fields")
    void updateSupplier_success() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
        when(supplierRepository.existsByEmailIgnoreCaseAndIdNot("orders@techsource.com", 1L)).thenReturn(false);
        when(supplierRepository.save(any(Supplier.class))).thenReturn(testSupplier);

        SupplierRequestDTO updateRequest = SupplierRequestDTO.builder()
                .name("TechSource Solutions Ltd")
                .email("orders@techsource.com")
                .contactPerson("Rajesh Kumar Sharma")
                .phone("+91 98765 11111")
                .status("ACTIVE")
                .build();

        SupplierResponseDTO response = supplierService.updateSupplier(1L, updateRequest);

        assertThat(response).isNotNull();
        verify(supplierRepository, times(1)).save(testSupplier);
    }

    @Test
    @DisplayName("updateSupplier when email belongs to another supplier should throw DuplicateResourceException")
    void updateSupplier_whenEmailInUse_shouldThrow() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
        when(supplierRepository.existsByEmailIgnoreCaseAndIdNot("other@vendor.com", 1L)).thenReturn(true);

        SupplierRequestDTO updateRequest = SupplierRequestDTO.builder()
                .name("TechSource Solutions")
                .email("other@vendor.com")
                .build();

        assertThatThrownBy(() -> supplierService.updateSupplier(1L, updateRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Supplier with email 'other@vendor.com' already exists");

        verify(supplierRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteSupplier success should delete and flush")
    void deleteSupplier_success() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));

        supplierService.deleteSupplier(1L);

        verify(supplierRepository, times(1)).findById(1L);
        verify(supplierRepository, times(1)).delete(testSupplier);
        verify(supplierRepository, times(1)).flush();
    }

    @Test
    @DisplayName("deleteSupplier when not found should throw ResourceNotFoundException")
    void deleteSupplier_whenNotFound_shouldThrow() {
        when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplierService.deleteSupplier(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Supplier with id '99' was not found");

        verify(supplierRepository, never()).delete(any(Supplier.class));
    }

    @Test
    @DisplayName("updateSupplierStatus success should update status")
    void updateSupplierStatus_success() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
        when(supplierRepository.save(any(Supplier.class))).thenReturn(testSupplier);

        SupplierResponseDTO response = supplierService.updateSupplierStatus(1L, "INACTIVE");

        assertThat(response).isNotNull();
        assertThat(testSupplier.getStatus()).isEqualTo(SupplierStatus.INACTIVE);
        verify(supplierRepository, times(1)).save(testSupplier);
    }

    @Test
    @DisplayName("updateSupplierStatus with invalid status should throw IllegalArgumentException")
    void updateSupplierStatus_invalidStatus_shouldThrow() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));

        assertThatThrownBy(() -> supplierService.updateSupplierStatus(1L, "PENDING"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Status must be either ACTIVE or INACTIVE");

        verify(supplierRepository, never()).save(any());
    }
}

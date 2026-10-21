package com.smartinventory.inventoryservice.service;

import com.smartinventory.inventoryservice.dto.CustomerRequestDTO;
import com.smartinventory.inventoryservice.dto.CustomerResponseDTO;
import com.smartinventory.inventoryservice.dto.PageResponse;
import com.smartinventory.inventoryservice.entity.Customer;
import com.smartinventory.inventoryservice.entity.enums.CustomerStatus;
import com.smartinventory.inventoryservice.exception.ResourceNotFoundException;
import com.smartinventory.inventoryservice.repository.CustomerRepository;
import com.smartinventory.inventoryservice.service.impl.CustomerServiceImpl;
import com.smartinventory.inventoryservice.service.mapper.CustomerMapper;
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
@DisplayName("CustomerService Unit Tests")
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Spy
    private CustomerMapper customerMapper = new CustomerMapper();

    @InjectMocks
    private CustomerServiceImpl customerService;

    private Customer testCustomer;
    private CustomerRequestDTO createRequest;

    @BeforeEach
    void setUp() {
        testCustomer = Customer.builder()
                .id(1L)
                .name("Kavita Rao")
                .email("kavita.rao@example.com")
                .phone("+91 98765 12345")
                .address("12 Residency Road, Bangalore, KA")
                .status(CustomerStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        createRequest = CustomerRequestDTO.builder()
                .name("Kavita Rao")
                .email("kavita.rao@example.com")
                .phone("+91 98765 12345")
                .address("12 Residency Road, Bangalore, KA")
                .status("ACTIVE")
                .build();
    }

    @Test
    @DisplayName("getCustomers should return paginated response")
    void getCustomers_shouldReturnPagedResponse() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Customer> page = new PageImpl<>(Collections.singletonList(testCustomer), pageable, 1);

        when(customerRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        PageResponse<CustomerResponseDTO> response = customerService.getCustomers("kavita", "ACTIVE", pageable);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getName()).isEqualTo("Kavita Rao");
        verify(customerRepository, times(1)).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("getAllActiveCustomers should return active customers list")
    void getAllActiveCustomers_shouldReturnList() {
        Customer inactiveCustomer = Customer.builder()
                .id(2L)
                .name("Inactive Customer")
                .status(CustomerStatus.INACTIVE)
                .build();

        when(customerRepository.findAllByOrderByNameAsc()).thenReturn(Arrays.asList(testCustomer, inactiveCustomer));

        List<CustomerResponseDTO> response = customerService.getAllActiveCustomers();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getId()).isEqualTo(1L);
        verify(customerRepository, times(1)).findAllByOrderByNameAsc();
    }

    @Test
    @DisplayName("getCustomerById when exists should return response")
    void getCustomerById_whenExists_shouldReturn() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));

        CustomerResponseDTO response = customerService.getCustomerById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("kavita.rao@example.com");
        verify(customerRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("getCustomerById when not found should throw ResourceNotFoundException")
    void getCustomerById_whenNotFound_shouldThrow() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getCustomerById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer with id '99' was not found");
    }

    @Test
    @DisplayName("createCustomer success should save and return response")
    void createCustomer_success() {
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);

        CustomerResponseDTO response = customerService.createCustomer(createRequest);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Kavita Rao");
        verify(customerRepository, times(1)).save(any(Customer.class));
    }

    @Test
    @DisplayName("createCustomer without email should save walk-in customer")
    void createCustomer_withoutEmail_shouldSucceed() {
        Customer walkInCustomer = Customer.builder()
                .id(2L)
                .name("Walk-in Shopper")
                .status(CustomerStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(customerRepository.save(any(Customer.class))).thenReturn(walkInCustomer);

        CustomerRequestDTO walkInRequest = CustomerRequestDTO.builder()
                .name("Walk-in Shopper")
                .build();

        CustomerResponseDTO response = customerService.createCustomer(walkInRequest);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Walk-in Shopper");
        assertThat(response.getEmail()).isNull();
        verify(customerRepository, times(1)).save(any(Customer.class));
    }

    @Test
    @DisplayName("updateCustomer success should update fields")
    void updateCustomer_success() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);

        CustomerRequestDTO updateRequest = CustomerRequestDTO.builder()
                .name("Kavita Rao Sharma")
                .phone("+91 98765 99999")
                .address("New Address")
                .status("ACTIVE")
                .build();

        CustomerResponseDTO response = customerService.updateCustomer(1L, updateRequest);

        assertThat(response).isNotNull();
        verify(customerRepository, times(1)).save(testCustomer);
    }

    @Test
    @DisplayName("updateCustomer when not found should throw ResourceNotFoundException")
    void updateCustomer_whenNotFound_shouldThrow() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.updateCustomer(99L, createRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer with id '99' was not found");

        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteCustomer success should delete and flush")
    void deleteCustomer_success() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));

        customerService.deleteCustomer(1L);

        verify(customerRepository, times(1)).findById(1L);
        verify(customerRepository, times(1)).delete(testCustomer);
        verify(customerRepository, times(1)).flush();
    }

    @Test
    @DisplayName("deleteCustomer when not found should throw ResourceNotFoundException")
    void deleteCustomer_whenNotFound_shouldThrow() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.deleteCustomer(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer with id '99' was not found");

        verify(customerRepository, never()).delete(any(Customer.class));
    }

    @Test
    @DisplayName("updateCustomerStatus success should update status")
    void updateCustomerStatus_success() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);

        CustomerResponseDTO response = customerService.updateCustomerStatus(1L, "INACTIVE");

        assertThat(response).isNotNull();
        assertThat(testCustomer.getStatus()).isEqualTo(CustomerStatus.INACTIVE);
        verify(customerRepository, times(1)).save(testCustomer);
    }

    @Test
    @DisplayName("updateCustomerStatus with invalid status should throw IllegalArgumentException")
    void updateCustomerStatus_invalidStatus_shouldThrow() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));

        assertThatThrownBy(() -> customerService.updateCustomerStatus(1L, "INVALID"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Status must be either ACTIVE or INACTIVE");

        verify(customerRepository, never()).save(any());
    }
}

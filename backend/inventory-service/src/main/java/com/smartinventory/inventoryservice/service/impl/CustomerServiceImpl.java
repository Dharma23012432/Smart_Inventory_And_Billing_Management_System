package com.smartinventory.inventoryservice.service.impl;

import com.smartinventory.inventoryservice.dto.CustomerRequestDTO;
import com.smartinventory.inventoryservice.dto.CustomerResponseDTO;
import com.smartinventory.inventoryservice.dto.PageResponse;
import com.smartinventory.inventoryservice.entity.Customer;
import com.smartinventory.inventoryservice.entity.enums.CustomerStatus;
import com.smartinventory.inventoryservice.exception.ResourceNotFoundException;
import com.smartinventory.inventoryservice.repository.CustomerRepository;
import com.smartinventory.inventoryservice.service.CustomerService;
import com.smartinventory.inventoryservice.service.mapper.CustomerMapper;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Production implementation of CustomerService.
 * Follows layered architecture, constructor injection, and SLF4J logging.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    /**
     * Constructor injection for required dependencies.
     *
     * @param customerRepository Repository for Customer entities
     * @param customerMapper     Manual entity-to-DTO mapper
     */
    public CustomerServiceImpl(
            CustomerRepository customerRepository,
            CustomerMapper customerMapper) {
        this.customerRepository = customerRepository;
        this.customerMapper = customerMapper;
    }

    @Override
    public PageResponse<CustomerResponseDTO> getCustomers(String search, String status, Pageable pageable) {
        log.info("Fetching customers page with search='{}', status='{}', pageable={}", search, status, pageable);

        Specification<Customer> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Status filter: defaults to ACTIVE if null/empty, matches status if provided, skips if "ALL"
            if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("ALL")) {
                try {
                    CustomerStatus customerStatus = CustomerStatus.valueOf(status.trim().toUpperCase());
                    predicates.add(cb.equal(root.get("status"), customerStatus));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid customer status filter '{}' ignored", status);
                }
            } else if (status == null) {
                predicates.add(cb.equal(root.get("status"), CustomerStatus.ACTIVE));
            }

            // Search query across name, phone, and email (case-insensitive)
            if (search != null && !search.trim().isEmpty()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate namePred = cb.like(cb.lower(root.get("name")), pattern);
                Predicate phonePred = cb.like(cb.lower(root.get("phone")), pattern);
                Predicate emailPred = cb.like(cb.lower(root.get("email")), pattern);
                predicates.add(cb.or(namePred, phonePred, emailPred));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Customer> page = customerRepository.findAll(spec, pageable);
        log.debug("Retrieved {} customers across {} total pages", page.getNumberOfElements(), page.getTotalPages());

        return PageResponse.from(page, customerMapper::toResponseDTO);
    }

    @Override
    public List<CustomerResponseDTO> getAllActiveCustomers() {
        log.info("Fetching all active customers for UI selections");

        List<Customer> customers = customerRepository.findAllByOrderByNameAsc();
        return customers.stream()
                .filter(c -> c.getStatus() == CustomerStatus.ACTIVE)
                .map(customerMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public CustomerResponseDTO getCustomerById(Long id) {
        log.info("Fetching customer by ID: {}", id);
        Customer customer = findCustomerOrThrow(id);
        return customerMapper.toResponseDTO(customer);
    }

    @Override
    @Transactional
    public CustomerResponseDTO createCustomer(CustomerRequestDTO requestDTO) {
        log.info("Creating customer with name: '{}', phone: '{}'", requestDTO.getName(), requestDTO.getPhone());

        Customer customer = customerMapper.toEntity(requestDTO);
        Customer savedCustomer = customerRepository.save(customer);
        log.info("Customer created successfully with ID: {}", savedCustomer.getId());

        return customerMapper.toResponseDTO(savedCustomer);
    }

    @Override
    @Transactional
    public CustomerResponseDTO updateCustomer(Long id, CustomerRequestDTO requestDTO) {
        log.info("Updating customer ID: {}, name: '{}'", id, requestDTO.getName());

        Customer customer = findCustomerOrThrow(id);
        customerMapper.updateEntityFromDTO(requestDTO, customer);

        Customer updatedCustomer = customerRepository.save(customer);
        log.info("Customer with ID: {} updated successfully", id);

        return customerMapper.toResponseDTO(updatedCustomer);
    }

    @Override
    @Transactional
    public void deleteCustomer(Long id) {
        log.info("Processing delete request for customer ID: {}", id);
        Customer customer = findCustomerOrThrow(id);

        customerRepository.delete(customer);
        customerRepository.flush();
        log.info("Customer with ID: {} deleted successfully", id);
    }

    @Override
    @Transactional
    public CustomerResponseDTO updateCustomerStatus(Long id, String status) {
        log.info("Updating status for customer ID: {} to '{}'", id, status);
        Customer customer = findCustomerOrThrow(id);

        try {
            CustomerStatus customerStatus = CustomerStatus.valueOf(status.trim().toUpperCase());
            customer.setStatus(customerStatus);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid customer status '{}' rejected", status);
            throw new IllegalArgumentException("Status must be either ACTIVE or INACTIVE");
        }

        Customer updatedCustomer = customerRepository.save(customer);
        log.info("Customer ID: {} status updated to {}", id, customer.getStatus());

        return customerMapper.toResponseDTO(updatedCustomer);
    }

    // -----------------------------------------------------------------
    // Internal Helper Methods
    // -----------------------------------------------------------------

    private Customer findCustomerOrThrow(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Customer with ID: {} not found", id);
                    return new ResourceNotFoundException("Customer", "id", id);
                });
    }
}

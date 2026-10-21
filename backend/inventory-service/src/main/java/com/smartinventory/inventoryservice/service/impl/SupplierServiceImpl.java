package com.smartinventory.inventoryservice.service.impl;

import com.smartinventory.inventoryservice.dto.PageResponse;
import com.smartinventory.inventoryservice.dto.SupplierRequestDTO;
import com.smartinventory.inventoryservice.dto.SupplierResponseDTO;
import com.smartinventory.inventoryservice.entity.Supplier;
import com.smartinventory.inventoryservice.entity.enums.SupplierStatus;
import com.smartinventory.inventoryservice.exception.DuplicateResourceException;
import com.smartinventory.inventoryservice.exception.ResourceNotFoundException;
import com.smartinventory.inventoryservice.repository.SupplierRepository;
import com.smartinventory.inventoryservice.service.SupplierService;
import com.smartinventory.inventoryservice.service.mapper.SupplierMapper;
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
 * Production implementation of SupplierService.
 * Enforces email uniqueness, layered architecture, constructor injection, and SLF4J logging.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;
    private final SupplierMapper supplierMapper;

    /**
     * Constructor injection for required dependencies.
     *
     * @param supplierRepository Repository for Supplier entities
     * @param supplierMapper     Manual entity-to-DTO mapper
     */
    public SupplierServiceImpl(
            SupplierRepository supplierRepository,
            SupplierMapper supplierMapper) {
        this.supplierRepository = supplierRepository;
        this.supplierMapper = supplierMapper;
    }

    @Override
    public PageResponse<SupplierResponseDTO> getSuppliers(String search, String status, Pageable pageable) {
        log.info("Fetching suppliers page with search='{}', status='{}', pageable={}", search, status, pageable);

        Specification<Supplier> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Status filter: defaults to ACTIVE if null/empty, matches status if provided, skips if "ALL"
            if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("ALL")) {
                try {
                    SupplierStatus supplierStatus = SupplierStatus.valueOf(status.trim().toUpperCase());
                    predicates.add(cb.equal(root.get("status"), supplierStatus));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid supplier status filter '{}' ignored", status);
                }
            } else if (status == null) {
                predicates.add(cb.equal(root.get("status"), SupplierStatus.ACTIVE));
            }

            // Search query across name, contactPerson, and email (case-insensitive)
            if (search != null && !search.trim().isEmpty()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate namePred = cb.like(cb.lower(root.get("name")), pattern);
                Predicate contactPred = cb.like(cb.lower(root.get("contactPerson")), pattern);
                Predicate emailPred = cb.like(cb.lower(root.get("email")), pattern);
                predicates.add(cb.or(namePred, contactPred, emailPred));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Supplier> page = supplierRepository.findAll(spec, pageable);
        log.debug("Retrieved {} suppliers across {} total pages", page.getNumberOfElements(), page.getTotalPages());

        return PageResponse.from(page, supplierMapper::toResponseDTO);
    }

    @Override
    public List<SupplierResponseDTO> getAllActiveSuppliers() {
        log.info("Fetching all active suppliers for dropdown selections");

        List<Supplier> suppliers = supplierRepository.findAllByOrderByNameAsc();
        return suppliers.stream()
                .filter(s -> s.getStatus() == SupplierStatus.ACTIVE)
                .map(supplierMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public SupplierResponseDTO getSupplierById(Long id) {
        log.info("Fetching supplier by ID: {}", id);
        Supplier supplier = findSupplierOrThrow(id);
        return supplierMapper.toResponseDTO(supplier);
    }

    @Override
    @Transactional
    public SupplierResponseDTO createSupplier(SupplierRequestDTO requestDTO) {
        String trimmedEmail = requestDTO.getEmail().trim().toLowerCase();
        log.info("Creating supplier with email: '{}', name: '{}'", trimmedEmail, requestDTO.getName());

        if (supplierRepository.existsByEmailIgnoreCase(trimmedEmail)) {
            log.warn("Supplier creation rejected — email '{}' already in use", trimmedEmail);
            throw new DuplicateResourceException("Supplier", "email", trimmedEmail);
        }

        Supplier supplier = supplierMapper.toEntity(requestDTO);
        Supplier savedSupplier = supplierRepository.save(supplier);
        log.info("Supplier created successfully with ID: {}", savedSupplier.getId());

        return supplierMapper.toResponseDTO(savedSupplier);
    }

    @Override
    @Transactional
    public SupplierResponseDTO updateSupplier(Long id, SupplierRequestDTO requestDTO) {
        String trimmedEmail = requestDTO.getEmail().trim().toLowerCase();
        log.info("Updating supplier ID: {}, new email: '{}'", id, trimmedEmail);

        Supplier supplier = findSupplierOrThrow(id);

        if (supplierRepository.existsByEmailIgnoreCaseAndIdNot(trimmedEmail, id)) {
            log.warn("Supplier update rejected — email '{}' already belongs to another supplier", trimmedEmail);
            throw new DuplicateResourceException("Supplier", "email", trimmedEmail);
        }

        supplierMapper.updateEntityFromDTO(requestDTO, supplier);
        Supplier updatedSupplier = supplierRepository.save(supplier);
        log.info("Supplier with ID: {} updated successfully", id);

        return supplierMapper.toResponseDTO(updatedSupplier);
    }

    @Override
    @Transactional
    public void deleteSupplier(Long id) {
        log.info("Processing delete request for supplier ID: {}", id);
        Supplier supplier = findSupplierOrThrow(id);

        supplierRepository.delete(supplier);
        supplierRepository.flush();
        log.info("Supplier with ID: {} deleted successfully", id);
    }

    @Override
    @Transactional
    public SupplierResponseDTO updateSupplierStatus(Long id, String status) {
        log.info("Updating status for supplier ID: {} to '{}'", id, status);
        Supplier supplier = findSupplierOrThrow(id);

        try {
            SupplierStatus supplierStatus = SupplierStatus.valueOf(status.trim().toUpperCase());
            supplier.setStatus(supplierStatus);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid supplier status '{}' rejected", status);
            throw new IllegalArgumentException("Status must be either ACTIVE or INACTIVE");
        }

        Supplier updatedSupplier = supplierRepository.save(supplier);
        log.info("Supplier ID: {} status updated to {}", id, supplier.getStatus());

        return supplierMapper.toResponseDTO(updatedSupplier);
    }

    // -----------------------------------------------------------------
    // Internal Helper Methods
    // -----------------------------------------------------------------

    private Supplier findSupplierOrThrow(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Supplier with ID: {} not found", id);
                    return new ResourceNotFoundException("Supplier", "id", id);
                });
    }
}

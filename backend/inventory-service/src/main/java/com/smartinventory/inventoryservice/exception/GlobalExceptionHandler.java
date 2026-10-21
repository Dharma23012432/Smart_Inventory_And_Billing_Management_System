package com.smartinventory.inventoryservice.exception;

import com.smartinventory.inventoryservice.dto.ErrorDetail;
import com.smartinventory.inventoryservice.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Centralized exception handler for inventory-service.
 * Strictly adheres to Section 2.2 and Section 9 of API_SPECIFICATION.md.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles 404 Not Found exceptions.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, HttpServletRequest request) {

        log.warn("Resource not found at {}: {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse response = ErrorResponse.of(
                "RESOURCE_NOT_FOUND",
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handles 409 Duplicate Resource exceptions.
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResourceException(
            DuplicateResourceException ex, HttpServletRequest request) {

        log.warn("Duplicate resource at {}: {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse response = ErrorResponse.of(
                "DUPLICATE_RESOURCE",
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Handles 409 Resource Conflict exceptions.
     */
    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<ErrorResponse> handleResourceConflictException(
            ResourceConflictException ex, HttpServletRequest request) {

        log.warn("Resource conflict at {}: {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse response = ErrorResponse.of(
                "RESOURCE_CONFLICT",
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Handles database relational integrity violations (foreign key delete restrictions, unique constraint collisions).
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex, HttpServletRequest request) {

        log.error("Data integrity violation at {}: {}", request.getRequestURI(), ex.getMessage());

        String message = "Operation violates database constraints or relational integrity";
        if (ex.getMessage() != null && ex.getMessage().contains("fk_product_category")) {
            message = "Cannot delete category because it is referenced by existing products";
        } else if (ex.getMessage() != null && ex.getMessage().contains("uq_category_name")) {
            message = "A category with this name already exists";
        } else if (ex.getMessage() != null && ex.getMessage().contains("uq_product_sku")) {
            message = "A product with this SKU already exists";
        } else if (ex.getMessage() != null && (ex.getMessage().contains("fk_bill_item_product") || ex.getMessage().contains("fk_purchase_item_product"))) {
            message = "Cannot delete product because it has associated transaction records. Deactivate the product instead.";
        } else if (ex.getMessage() != null && ex.getMessage().contains("uq_supplier_email")) {
            message = "A supplier with this email address already exists";
        } else if (ex.getMessage() != null && ex.getMessage().contains("fk_purchase_supplier")) {
            message = "Cannot delete supplier because it is referenced by existing purchase orders. Deactivate the supplier instead.";
        } else if (ex.getMessage() != null && ex.getMessage().contains("fk_bill_customer")) {
            message = "Cannot delete customer because they have existing sales and billing records. Deactivate the customer instead.";
        }

        ErrorResponse response = ErrorResponse.of(
                "DATA_INTEGRITY_VIOLATION",
                message,
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Handles Bean Validation failures (@Valid).
     * Maps to HTTP 400 Bad Request with field-level details.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<ErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> ErrorDetail.builder()
                        .field(fieldError.getField())
                        .rejectedValue(fieldError.getRejectedValue())
                        .message(fieldError.getDefaultMessage())
                        .build())
                .collect(Collectors.toList());

        String message = String.format("Input validation failed for %d field(s)", details.size());
        log.warn("Validation error at {}: {}", request.getRequestURI(), message);

        ErrorResponse response = ErrorResponse.of(
                "VALIDATION_FAILED",
                message,
                details,
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles JPA optimistic locking concurrency collisions.
     * Maps to HTTP 409 Conflict.
     */
    @ExceptionHandler({org.springframework.orm.ObjectOptimisticLockingFailureException.class, jakarta.persistence.OptimisticLockException.class})
    public ResponseEntity<ErrorResponse> handleOptimisticLockingFailureException(
            Exception ex, HttpServletRequest request) {

        log.warn("Optimistic locking collision at {}: {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse response = ErrorResponse.of(
                "OPTIMISTIC_LOCK_CONFLICT",
                "The inventory record was updated by another concurrent transaction. Please refresh and retry.",
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Handles general illegal argument exceptions.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {

        log.warn("Illegal argument at {}: {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse response = ErrorResponse.of(
                "INVALID_ARGUMENT",
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles illegal domain state transitions (e.g. attempting to receive or cancel a non-pending order).
     * Maps to HTTP 400 Bad Request.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(
            IllegalStateException ex, HttpServletRequest request) {

        log.warn("Illegal state transition at {}: {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse response = ErrorResponse.of(
                "INVALID_STATE",
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles all unhandled runtime exceptions.
     * Maps to HTTP 500 Internal Server Error.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {

        log.error("Unhandled exception at {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        ErrorResponse response = ErrorResponse.of(
                "INTERNAL_SERVER_ERROR",
                "An unexpected internal error occurred. Please contact system administrator.",
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}

package com.smartinventory.inventoryservice.exception;

/**
 * Thrown when an operation cannot be performed due to business invariants or relational integrity.
 * Maps to HTTP 409 CONFLICT.
 */
public class ResourceConflictException extends RuntimeException {

    public ResourceConflictException(String message) {
        super(message);
    }
}

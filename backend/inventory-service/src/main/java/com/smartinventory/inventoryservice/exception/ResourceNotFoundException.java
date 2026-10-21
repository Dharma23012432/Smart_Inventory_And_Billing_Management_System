package com.smartinventory.inventoryservice.exception;

/**
 * Thrown when a requested resource is not found in the database.
 * Maps to HTTP 404 NOT_FOUND.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s with %s '%s' was not found", resourceName, fieldName, fieldValue));
    }
}

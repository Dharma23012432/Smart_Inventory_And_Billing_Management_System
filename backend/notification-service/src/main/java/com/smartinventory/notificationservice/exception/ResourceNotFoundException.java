package com.smartinventory.notificationservice.exception;

/**
 * Exception thrown when a requested notification resource cannot be found.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}

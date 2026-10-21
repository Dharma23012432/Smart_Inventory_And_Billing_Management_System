package com.smartinventory.payment.exception;

/**
 * Exception thrown when attempting to create a resource that already exists (e.g., payment already recorded for a bill).
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}

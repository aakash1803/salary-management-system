package com.salarymanagement.common;

/**
 * Thrown when a requested resource does not exist. Handled by
 * {@link GlobalExceptionHandler} and translated into an HTTP 404 response.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}

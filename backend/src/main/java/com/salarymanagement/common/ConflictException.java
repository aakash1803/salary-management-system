package com.salarymanagement.common;

/**
 * Thrown when a request conflicts with existing state (e.g. a uniqueness rule such as a
 * duplicate employee number). Handled by {@link GlobalExceptionHandler} and translated into an
 * HTTP 409 response.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}

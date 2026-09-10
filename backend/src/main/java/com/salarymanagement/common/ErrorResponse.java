package com.salarymanagement.common;

import java.time.Instant;

/**
 * Uniform JSON error body returned by the API. Deliberately excludes stack
 * traces or any other internal/persistence detail.
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {

    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(Instant.now(), status, error, message, path);
    }
}

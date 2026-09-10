package com.salarymanagement.auth;

/**
 * Login response body. Contains only the issued JWT and how to use it - never the username,
 * password, or password hash.
 */
public record LoginResponse(
        String token,
        String tokenType,
        long expiresInSeconds
) {
}

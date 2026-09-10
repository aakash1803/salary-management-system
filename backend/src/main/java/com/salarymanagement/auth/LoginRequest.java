package com.salarymanagement.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Login request body for {@code POST /api/auth/login}.
 */
public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password
) {
}

package com.salarymanagement.auth;

/**
 * Response body for {@code GET /api/auth/me}. Contains only the authenticated username - never
 * the token, since the client never needs to see it (it lives solely in the HttpOnly cookie).
 */
public record CurrentUserResponse(String username) {
}

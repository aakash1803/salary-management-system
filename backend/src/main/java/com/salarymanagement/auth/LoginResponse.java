package com.salarymanagement.auth;

/**
 * Login response body for {@code POST /api/auth/login}. Contains only the authenticated
 * username and the token's lifetime - never the JWT itself, the username/password, or the
 * password hash. The JWT is instead delivered as an HttpOnly {@code access_token} cookie (see
 * {@link AuthController}), so it is never exposed to frontend JavaScript.
 */
public record LoginResponse(
        String username,
        long expiresInSeconds
) {
}

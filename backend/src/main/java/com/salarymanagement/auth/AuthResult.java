package com.salarymanagement.auth;

/**
 * Internal result of a successful login - never serialized directly to a client.
 *
 * <p>{@link AuthController} uses {@link #token()} to set the HttpOnly authentication cookie and
 * folds {@link #username()}/{@link #expiresInSeconds()} into the public {@link LoginResponse}.
 * Keeping this separate from {@link LoginResponse} is what makes it structurally impossible for
 * the JWT to end up in the JSON response body - there is no token field on the DTO that actually
 * gets serialized.
 */
record AuthResult(String token, String username, long expiresInSeconds) {
}

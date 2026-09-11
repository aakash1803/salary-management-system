package com.salarymanagement.auth;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication API. Only login, logout, and "current user" are implemented - no registration,
 * password reset, OAuth, or refresh tokens, per the project's authentication requirements.
 *
 * <p>The JWT is transported exclusively as an HttpOnly {@code access_token} cookie (name and
 * {@code Secure} flag configured via {@code app.security.jwt.cookie-*}) - it is never present in
 * a JSON response body, so it can never be read or stored by frontend JavaScript. Both
 * {@link #login} and {@link #logout} build that cookie through the same two private helper
 * methods so the cookie's attributes (name, path, {@code SameSite}, {@code Secure}) stay defined
 * in exactly one place.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final String cookieName;
    private final boolean cookieSecure;

    public AuthController(
            AuthService authService,
            @Value("${app.security.jwt.cookie-name}") String cookieName,
            @Value("${app.security.jwt.cookie-secure}") boolean cookieSecure) {
        this.authService = authService;
        this.cookieName = cookieName;
        this.cookieSecure = cookieSecure;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthResult result = authService.login(request.username(), request.password());

        response.addHeader(HttpHeaders.SET_COOKIE, authCookie(result.token(), result.expiresInSeconds()).toString());

        return new LoginResponse(result.username(), result.expiresInSeconds());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        // Authentication is stateless (no server-side session to invalidate) - clearing the
        // cookie client-side is the entire logout operation.
        response.addHeader(HttpHeaders.SET_COOKIE, expiredCookie().toString());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public CurrentUserResponse me(Authentication authentication) {
        // The principal was already established by JwtAuthenticationFilter from a valid cookie -
        // this endpoint only reaches here at all if that succeeded (see SecurityConfig), so there
        // is no need to re-validate anything here.
        return new CurrentUserResponse(authentication.getName());
    }

    private ResponseCookie authCookie(String token, long maxAgeSeconds) {
        return ResponseCookie.from(cookieName, token)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();
    }

    private ResponseCookie expiredCookie() {
        return ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
    }
}

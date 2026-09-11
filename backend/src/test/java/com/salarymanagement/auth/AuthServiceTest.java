package com.salarymanagement.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Pure Mockito unit tests for {@link AuthService}. Uses a real {@link BCryptPasswordEncoder}
 * (fast, no Spring context) so the actual hashing/matching logic is genuinely exercised; only
 * {@link JwtService} is mocked, since it is an external collaborator.
 *
 * <p>{@link AuthService#login} returns the package-private {@link AuthResult} (token + username
 * + expiry) rather than a {@link LoginResponse} - the JWT itself is an internal detail that only
 * {@link AuthController} (same package) turns into a cookie; it is asserted on here since this
 * test lives in the same package, but a {@link LoginResponse} is never built with a token field
 * at all.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String USERNAME = "hr.manager";
    private static final String RAW_PASSWORD = "correct-horse-battery-staple";

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        String passwordHash = passwordEncoder.encode(RAW_PASSWORD);
        authService = new AuthService(USERNAME, passwordHash, passwordEncoder, jwtService);
    }

    @Test
    void loginWithCorrectCredentialsReturnsAnAuthResult() {
        when(jwtService.generateToken(USERNAME)).thenReturn("a.jwt.token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        AuthResult result = authService.login(USERNAME, RAW_PASSWORD);

        assertEquals("a.jwt.token", result.token());
        assertEquals(USERNAME, result.username());
        assertEquals(3600L, result.expiresInSeconds());
    }

    @Test
    void loginWithWrongPasswordIsRejected() {
        assertThrows(BadCredentialsException.class,
                () -> authService.login(USERNAME, "wrong-password"));
    }

    @Test
    void loginWithUnknownUsernameIsRejected() {
        assertThrows(BadCredentialsException.class,
                () -> authService.login("someone-else", RAW_PASSWORD));
    }
}

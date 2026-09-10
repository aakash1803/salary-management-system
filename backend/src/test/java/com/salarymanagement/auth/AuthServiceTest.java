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
    void loginWithCorrectCredentialsReturnsAToken() {
        when(jwtService.generateToken(USERNAME)).thenReturn("a.jwt.token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        LoginResponse response = authService.login(USERNAME, RAW_PASSWORD);

        assertEquals("a.jwt.token", response.token());
        assertEquals("Bearer", response.tokenType());
        assertEquals(3600L, response.expiresInSeconds());
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

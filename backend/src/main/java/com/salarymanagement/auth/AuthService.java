package com.salarymanagement.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Validates HR Manager credentials and issues a JWT on success.
 *
 * <p>There is a single configured HR Manager account (a username and a BCrypt password hash,
 * both externally configured - see {@link JwtService} and application.properties), not a
 * database-backed user store: the project has one primary user role, so a full user-management
 * subsystem would be unnecessary complexity for this requirement.
 */
@Service
public class AuthService {

    private final String configuredUsername;
    private final String configuredPasswordHash;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            @Value("${app.security.hr-manager.username}") String configuredUsername,
            @Value("${app.security.hr-manager.password-hash}") String configuredPasswordHash,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.configuredUsername = configuredUsername;
        this.configuredPasswordHash = configuredPasswordHash;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(String username, String password) {
        // Both checks are always evaluated (rather than short-circuiting as soon as the
        // username looks wrong) so a failed login takes a similar amount of time either way,
        // and the error response never reveals whether the username was even recognized.
        boolean usernameMatches = configuredUsername.equals(username);
        boolean passwordMatches = passwordEncoder.matches(password, configuredPasswordHash);

        if (!usernameMatches || !passwordMatches) {
            throw new BadCredentialsException("Invalid username or password.");
        }

        String token = jwtService.generateToken(username);
        return new LoginResponse(token, "Bearer", jwtService.getExpirationSeconds());
    }
}

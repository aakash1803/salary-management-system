package com.salarymanagement.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Issues and validates the HMAC-signed JWTs used for stateless authentication.
 *
 * <p>The signing secret and token lifetime are configured, never hard-coded - see
 * {@code app.security.jwt.*} in application.properties and docs/security-configuration.md.
 * The exact HMAC algorithm (HS256/HS384/HS512) is chosen automatically by
 * {@link Keys#hmacShaKeyFor(byte[])} based on the configured secret's byte length; the
 * configured secret must be at least 32 bytes (256 bits) as UTF-8, or key creation fails fast
 * at startup.
 */
@Component
public class JwtService {

    private final SecretKey signingKey;
    private final Duration tokenValidity;

    public JwtService(
            @Value("${app.security.jwt.secret}") String secret,
            @Value("${app.security.jwt.expiration-minutes:60}") long expirationMinutes) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.tokenValidity = Duration.ofMinutes(expirationMinutes);
    }

    public String generateToken(String username) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(tokenValidity)))
                .signWith(signingKey)
                .compact();
    }

    public long getExpirationSeconds() {
        return tokenValidity.getSeconds();
    }

    /**
     * Validates the given compact JWT (signature and expiration) and returns its subject
     * (the authenticated username).
     *
     * @throws JwtException if the token is malformed, expired, or fails signature verification
     */
    public String validateAndGetSubject(String token) {
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token);
        return jws.getPayload().getSubject();
    }
}

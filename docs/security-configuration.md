# Security Configuration

This document lists the configuration values the authentication vertical slice depends on,
where they live, and how to generate your own for any environment beyond local development.
It is a companion to the authentication flow already described in `architecture.md` section 5 -
this file is the concrete "how to configure it" reference; `architecture.md` remains the
conceptual description.

## Configuration values

All four values are read from `backend/src/main/resources/application.properties`, and every
one can be overridden by an environment variable (Spring Boot's standard relaxed binding):

| Property                                  | Environment variable                        | Purpose                                                        |
|--------------------------------------------|----------------------------------------------|------------------------------------------------------------------|
| `app.security.hr-manager.username`          | `APP_SECURITY_HR_MANAGER_USERNAME`            | The single HR Manager account's login username.                  |
| `app.security.hr-manager.password-hash`     | `APP_SECURITY_HR_MANAGER_PASSWORD_HASH`       | BCrypt hash of that account's password. Never the plaintext.     |
| `app.security.jwt.secret`                   | `APP_SECURITY_JWT_SECRET`                     | Raw key material used to sign/verify JWTs (HMAC).                |
| `app.security.jwt.expiration-minutes`       | `APP_SECURITY_JWT_EXPIRATION_MINUTES`         | Token lifetime in minutes. Optional, defaults to `60`.           |

## Dev-only defaults

`application.properties` (both `src/main` and `src/test`) ships with placeholder values so the
project builds and runs out of the box locally:

- Username: `hr.manager`
- Password: `changeme123!` (only its BCrypt hash is committed, not the plaintext)
- JWT secret: a randomly generated 64-character string (not derived from anything secret)

**These are not real secrets and must never be used outside local development.** Override all
three via environment variables for any shared, staging, or production deployment.

## Generating your own password hash

The password hash must be a BCrypt hash (Spring Security's `BCryptPasswordEncoder` accepts the
`$2a$`/`$2b$`/`$2y$` variants interchangeably). Any correct BCrypt implementation works; for
example, in a `jshell` session on this project's classpath:

```
jshell> import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
jshell> new BCryptPasswordEncoder().encode("your-new-password")
```

Or with Python's `bcrypt` package:

```python
import bcrypt
print(bcrypt.hashpw(b"your-new-password", bcrypt.gensalt(rounds=10)).decode())
```

Put the resulting hash in `APP_SECURITY_HR_MANAGER_PASSWORD_HASH` (or the property directly for
local-only use) - never commit a real password's hash to source control.

## Generating your own JWT secret

Any sufficiently long random string works; the configured secret's raw UTF-8 byte length must be
at least 32 bytes (256 bits) for `Keys.hmacShaKeyFor` (jjwt) to accept it as an HMAC key - it
throws at startup otherwise. For example:

```python
import secrets
print(secrets.token_urlsafe(48))
```

## Design notes

- **Single configured HR Manager account, not a database-backed user store.** The product has
  one primary user role; a `User` entity/repository/registration flow would be unused complexity
  the requirements do not call for.
- **Stateless JWT, no sessions, no HTTP Basic.** `SecurityConfig` sets
  `SessionCreationPolicy.STATELESS` and explicitly disables `httpBasic`/`formLogin`/CSRF (CSRF
  protection is for cookie/session-based auth, which this API does not use).
- **Login failures are generic.** `AuthService.login` always evaluates both the username and
  password checks, and always throws the same `BadCredentialsException` with the same message,
  regardless of which check failed - this avoids revealing whether a given username exists via
  response timing or message content.
- **Invalid and missing JWTs are handled identically.** `JwtAuthenticationFilter` never sets the
  security context for a missing, malformed, invalid, or expired token; `JwtAuthenticationEntryPoint`
  then rejects the resulting unauthenticated request with a uniform 401 JSON body (same shape as
  the rest of the API's error responses). Neither ever logs the raw token.
- **`SecurityConfig.securityFilterChain(...)` is `@ConditionalOnWebApplication(type = SERVLET)`.**
  Some of this project's existing persistence tests run with
  `@SpringBootTest(webEnvironment = WebEnvironment.NONE)` (a non-web `ApplicationContext`), which
  cannot supply the `HttpSecurity` bean this method needs. The `passwordEncoder()` bean in the
  same class is intentionally left unconditional, since `AuthService` needs it in every context.

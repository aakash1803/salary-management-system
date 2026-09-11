# Security Configuration

This document describes the authentication model, security configuration properties, JWT cookie transport, CORS setup, and CSRF decisions for the Salary Management System.

---

## 1. Authentication Model & Architecture

- **Single HR Manager Account**: The application configures a single administrative account (`ROLE_HR_MANAGER`) via environment variables/properties rather than a complex multi-tenant user table.
- **Authentication Flow**:
  1. Login (`POST /api/auth/login`) with username and password.
  2. Backend validates configured HR Manager credentials.
  3. Backend creates a signed HMAC-SHA256 JWT.
  4. Backend sets the JWT in an `HttpOnly`, `SameSite=Lax` `access_token` cookie.
  5. Browser automatically sends the cookie on subsequent API requests (`withCredentials: true`).
  6. Backend validates the JWT before serving protected resources.
- **HttpOnly Cookie Transport**: An HttpOnly cookie prevents client-side JavaScript from directly reading the JWT, reducing the risk of token exfiltration through client-side token access. The token is never stored in `localStorage`, `sessionStorage`, or JavaScript-accessible cookies.

---

## 2. Security Configuration Properties

All security settings are configured in `application.properties` and overridable via environment variables:

| Property | Environment Variable | Purpose | Default / Example |
| :--- | :--- | :--- | :--- |
| `app.security.hr-manager.username` | `APP_SECURITY_HR_MANAGER_USERNAME` | Login username | `hr.manager` |
| `app.security.hr-manager.password-hash` | `APP_SECURITY_HR_MANAGER_PASSWORD_HASH` | BCrypt password hash | Hash for `changeme123!` |
| `app.security.jwt.secret` | `APP_SECURITY_JWT_SECRET` | Raw key material for HMAC-SHA256 (>=256 bits) | Configured secret |
| `app.security.jwt.expiration-minutes` | `APP_SECURITY_JWT_EXPIRATION_MINUTES` | Token lifetime in minutes | `60` |
| `app.security.jwt.cookie-name` | `APP_SECURITY_JWT_COOKIE_NAME` | Name of HttpOnly cookie | `access_token` |
| `app.security.jwt.cookie-secure` | `APP_SECURITY_JWT_COOKIE_SECURE` | Set `Secure` flag on cookie | `false` (dev), `true` (prod HTTPS) |
| `app.security.cors.allowed-origin` | `APP_SECURITY_CORS_ALLOWED_ORIGIN` | Allowed origin for credentialed CORS | `http://localhost:4200` or `http://localhost` |

### Changing Credentials & Production Guidance
- **Development Credentials**: The documented username `hr.manager` and password `changeme123!` are checked into source control for local development/demo convenience only. Real production credentials and signing secrets must never be committed to source control.
- **Generating BCrypt Hashes**: Password hashes must be created using standard BCrypt implementations (e.g. Spring Security's `BCryptPasswordEncoder` or standard BCrypt CLI tools) and set via `APP_SECURITY_HR_MANAGER_PASSWORD_HASH`.

---

## 3. Endpoints & API Contract

- `POST /api/auth/login` — Public. Validates credentials, sets `access_token` HttpOnly cookie, and returns `{ "username": "...", "expiresInSeconds": 3600 }`. The raw JWT is never exposed in the response body.
- `POST /api/auth/logout` — Public. Clears the `access_token` cookie by returning `Max-Age=0` and 200 OK.
- `GET /api/auth/me` — Protected. Returns `{ "username": "hr.manager" }` for Angular route guards to verify active session state.

---

## 4. Security Decisions

### CORS Configuration
- CORS explicitly permits requests from the single configured origin (`http://localhost` for Docker Compose, `http://localhost:4200` for native Angular dev server).
- `Access-Control-Allow-Credentials` is set to `true` to allow HttpOnly cookie transmission.
- Wildcard origins (`*`) are prohibited by Spring Security when credentials are enabled.

### CSRF Decision
- CSRF protection is currently disabled as an intentional decision for the current authentication and deployment model, considering:
  1. `HttpOnly` JWT cookie transport
  2. Same-origin Nginx deployment topology
  3. Restricted credentialed CORS
  4. Absence of state-changing `GET` endpoints
- *This decision should be revisited if authentication semantics, deployment topology, or allowed origins change.*

### Container Security
- Backend container runs under non-root user `spring` (UID 1000).

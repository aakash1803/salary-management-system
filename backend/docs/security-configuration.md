# Security Configuration

This document explains the authentication and authorization setup for the backend: how the JWT
is issued and transported, how CORS is configured, and why CSRF protection is currently disabled.

## Authentication model

There is a single configured HR Manager account (username + BCrypt password hash), not a
database-backed user store - the application has exactly one authenticated role
(`ROLE_HR_MANAGER`), so a full user-management subsystem is unnecessary complexity for this
requirement.

On successful login, the backend issues a stateless, HMAC-signed JWT (see `JwtService`). The
token's signing secret and lifetime are configured via `app.security.jwt.secret` and
`app.security.jwt.expiration-minutes`.

## JWT transport: HttpOnly cookie only

The JWT is transported exclusively as an HttpOnly cookie:

- **Cookie name**: configured via `app.security.jwt.cookie-name` (`access_token` by default).
- **HttpOnly**: always `true` - the token is never readable by frontend JavaScript, which rules
  out theft via XSS reading `document.cookie` or any script-accessible storage
  (`localStorage`/`sessionStorage` are not used for this reason).
- **Path**: `/`.
- **SameSite**: `Lax` - appropriate for this application's same-site-in-practice topology
  (`http://localhost:4200` and `http://localhost:8080` are different ports but the same site).
  `Lax` still allows the cookie on normal top-level navigation and same-site `fetch`/XHR calls,
  while blocking it on cross-site requests originated by other sites - which is most of what
  CSRF protection would otherwise need to defend against (see "CSRF decision" below).
- **Secure**: configured via `app.security.jwt.cookie-secure` - `false` for local HTTP
  development (a `Secure` cookie is never sent over plain HTTP, so leaving this `true` locally
  would silently break login), and must be overridden to `true` (e.g. via the
  `APP_SECURITY_JWT_COOKIE_SECURE` environment variable) in any environment served over HTTPS.
- **Max-Age**: set to match the configured JWT expiration (`app.security.jwt.expiration-minutes`),
  so the cookie and the token it carries expire together.

The JWT is never logged, anywhere in the authentication code path (login, the JWT filter, or
logout).

## API contract

- `POST /api/auth/login` - public. On success, sets the `access_token` cookie and returns
  `{ "username": "...", "expiresInSeconds": <n> }`. The JWT itself never appears in the response
  body. On invalid credentials, returns the standard 401 error body and does not set a cookie.
- `POST /api/auth/logout` - public (logging out while already unauthenticated is harmless).
  Clears the `access_token` cookie (same name/path, `Max-Age=0`) and returns 200. Authentication
  is stateless, so there is no server-side session to invalidate.
- `GET /api/auth/me` - requires authentication (a valid `access_token` cookie). Returns
  `{ "username": "..." }` using the already-authenticated principal. Used by the frontend to
  restore login state on page load/refresh, since it can no longer inspect the cookie itself.

## CORS

A single trusted origin is allowed, configured via `app.security.cors.allowed-origin`
(`http://localhost:4200` for local Angular development). Credentialed requests are enabled
(`Access-Control-Allow-Credentials: true`), which is required for the browser to send the
HttpOnly cookie cross-port. **A wildcard origin (`*`) is never used** here - Spring Security
rejects that combination with `allowCredentials(true)` outright, and it would defeat the purpose
of restricting which origins can act on behalf of an authenticated user anyway. Preflight
(`OPTIONS`) requests are permitted through the authorization rules regardless of the target
endpoint's own auth requirement, since the browser sends preflight before any credentials are
attached.

## CSRF decision

**CSRF protection remains disabled for this application's current scope.** This is a deliberate,
documented decision, not a carried-over default:

- The JWT lives in a `SameSite=Lax` HttpOnly cookie, which already blocks the cookie from being
  attached to most cross-site requests that a forged page could issue.
- CORS allows only the single configured, trusted frontend origin, with no wildcard - a page
  hosted anywhere else cannot get a browser to make a credentialed cross-origin call that the
  backend will accept.
- There are no authenticated state-changing `GET` endpoints in this application (the classic gap
  that `SameSite` alone doesn't close), so there is no state-changing action reachable via a
  simple top-level navigation or an `<img>`/link-style request.

Given those three points together, the residual CSRF risk for this application's current scope is
low, and a full CSRF-token mechanism would add complexity (token issuance, storage, and
verification on every state-changing request) without a concrete threat it closes.

**If this changes** - for example, if the frontend and backend are later served from genuinely
different sites (not just different ports on `localhost`), if `SameSite` ever needs to relax to
`None`, or if browser-based state-changing operations broaden beyond this application's current
shape - a CSRF token mechanism (e.g. Spring Security's cookie-based double-submit token) should be
introduced at that point.

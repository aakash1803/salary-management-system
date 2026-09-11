# Salary Management System — Architecture

## 1. Architecture Overview

The system uses a **modular monolith** architecture: a single deployable Spring Boot application, internally organized into cohesive, feature-oriented modules (auth, employee, salary, dashboard, common), backed by an Angular frontend served via an Nginx reverse proxy in a containerized environment.

```
Browser (HTTP :80)
        |
        v
Nginx Frontend Container (:80)
        |
        | Reverse proxies /api/* calls
        v
Spring Boot Backend Container (:8080)
        |
        +-- Auth Module (HttpOnly JWT Cookie)
        +-- Employee Module (Server-side Search & Pagination)
        +-- Salary Module (Bulk Current Salary Lookup & History)
        +-- Dashboard Module (SQL Aggregations)
        +-- Common Module (Global Error Handling & Utilities)
        |
        v
Spring Data JPA / Hibernate
        |
        v
SQLite Database (/app/data/salary-management.db on volume sqlite_data)
```

This is appropriate for the system's scale and scope because:

- The organization has **~10,000 employees and ~20,000 salary records** (the deterministic seed contains 10,000 employees and 19,999 salary records) and a single primary user role (HR Manager) — this is a focused data and load profile that does not require independently scaled microservices.
- A single relational database (SQLite) is sufficient; there is no need to partition data or ownership across services.
- A modular monolith keeps deployment, transactions, and data consistency simple (one process, one database, no distributed transactions or network calls between modules).
- Module boundaries (auth, employee, salary, dashboard, common) enforce separation of concerns and leave a clear path to extraction later, without the operational cost of microservices up front.
- Containerization with Docker Compose and Nginx provides a consistent containerized runtime and deployment setup.

---

## 2. Technology Stack

- **Java 21** — backend language runtime (Eclipse Temurin).
- **Spring Boot 4.1.1** — application framework (web, dependency injection, configuration, security).
- **Spring Data JPA / Hibernate** — ORM and repository-based data access.
- **SQLite** — embedded relational database persisted to a mounted volume (`sqlite_data`).
- **Angular 21** — frontend SPA framework.
- **TypeScript** — frontend language.
- **Node.js 22** — frontend build toolchain.
- **Nginx** — web server serving static Angular production assets and proxying `/api` requests to the backend container.
- **JWT (JSON Web Tokens)** — stateless authentication mechanism delivered exclusively via `HttpOnly` cookies.
- **JUnit 5 & Spring Boot Test** — backend unit, integration, and full-system HTTP acceptance testing framework.
- **Vitest** — frontend unit testing framework.
- **Docker & Docker Compose** — multi-stage container build and environment orchestration.
- **GitHub Actions** — CI pipeline for automated backend, frontend, and Docker validation.

---

## 3. Backend Architecture

The backend is organized by **feature/module**:

```
auth/
employee/
salary/
dashboard/
common/
seed/
```

- **auth** — login and logout endpoints, JWT cookie issuance/validation, Spring Security configuration.
- **employee** — employee CRUD, server-side search, filtering, and pagination.
- **salary** — salary record creation, salary history, bulk current-salary lookup optimization.
- **dashboard** — aggregate SQL-based salary insights and statistics.
- **common** — global exception handling, DTO validation, shared utilities.
- **seed** — explicit database seeder generating a deterministic dataset of 10,000 employees and 19,999 salary records.

Within each business module, a layered flow is enforced:

```
Controller → Service → Repository → Database
```

- **Controller** — accepts HTTP requests, validates DTOs, delegates to services, and returns HTTP responses.
- **Service** — orchestrates business logic and domain rules (e.g., positive monetary amounts, uniqueness).
- **Repository** — Spring Data JPA interfaces responsible for database queries and persistence.
- **Database** — SQLite database stored on persistent storage.

---

## 4. Frontend & Reverse Proxy Architecture

### Nginx Reverse Proxy Container
In containerized environments, Nginx listens on port `80`:
- Serves built Angular static files from `/usr/share/nginx/html`.
- Implements single-page application fallback (`try_files $uri $uri/ /index.html`).
- Proxies `/api/` requests to `http://backend:8080/api/`.

### Angular Application Structure
```
core/
  auth/          # Auth state management, login/logout
  guards/        # Route protection (requires authenticated session)
  interceptors/  # Error response handling (e.g., 401 redirect)
  services/      # HTTP clients for backend APIs

shared/
  components/    # Reusable UI controls (pagination, tables, modals)
  models/        # Shared TypeScript interfaces

features/
  auth/          # Login screen
  dashboard/     # Salary insights cards & charts
  employees/     # Employee list, search, filter, detail, and edit
  salary/        # Salary history and change recording
```

### Authentication Handling
- The backend sets the JWT in an `HttpOnly` `access_token` cookie upon successful authentication.
- An HttpOnly cookie prevents client-side JavaScript from directly reading the JWT, reducing the risk of token exfiltration through client-side token access (tokens are not stored in `localStorage`, `sessionStorage`, or JavaScript-readable storage).
- `withCredentials: true` enables cookie transmission on HTTP requests issued by Angular's `HttpClient`.
- Route guards verify session state via `GET /api/auth/me`.

---

## 5. Authentication & Security

### Authentication Flow
```
Login (POST /api/auth/login)
  → Backend validates configured HR Manager credentials
  → Backend creates signed JWT
  → Backend sets JWT in HttpOnly access_token cookie
  → Browser automatically sends the cookie on subsequent API requests
  → Backend validates the JWT
```

- **Cookie Parameters**:
  - `HttpOnly = true` (prevents client-side JavaScript access).
  - `SameSite = Lax` (mitigates cross-site request forgery).
  - `Secure` configurable (`false` for local HTTP, `true` for production HTTPS).
  - `Path = /`.
- **CORS**: Restricted to the configured frontend origin with credentials enabled. No wildcard origins.
- **CSRF Decision**: CSRF protection is currently disabled as an intentional decision for the current authentication and deployment model, considering:
  1. `HttpOnly` JWT cookie transport
  2. Same-origin Nginx deployment topology
  3. Restricted credentialed CORS
  4. Absence of state-changing `GET` endpoints
  *This decision should be revisited if authentication semantics, deployment topology, or allowed origins change.*

---

## 6. Database & Seeding Strategy

- **SQLite Database**: Stored at `/app/data/salary-management.db`.
- **Persistent Volume**: Docker Compose mounts `sqlite_data` to `/app/data` to ensure container restarts do not delete application data.
- **Explicit Seeder**: Populates **10,000 employees** and **19,999 salary records** deterministically.
- **No Automatic Startup Seeding**: Seeding is intentionally decoupled from application startup to avoid overwriting persistent data when backend containers restart. Seeding is triggered explicitly via:
  ```bash
  docker compose --profile tools run --rm seeder
  ```
  or locally via `./gradlew seedDatabase`.

---

## 7. Performance & Optimization Strategy

1. **Server-Side Pagination & Search**: Employee list endpoints handle pagination, department/country filters, and text search at the database query level for ~10,000 employees and ~20,000 salary records.
2. **Bulk Current Salary Retrieval**: Solves potential N+1 query overhead when rendering paginated employee lists. The implementation:
   - Obtains the employee IDs for the requested page
   - Executes one bulk repository query for applicable salary records (`findByEmployee_IdInAndEffectiveFromLessThanEqualOrderByEmployee_IdAscEffectiveFromDescIdDesc`)
   - Orders records by employee and effective date
   - Selects the current record per employee in `SalaryService`
   - Maps salary/currency into the list response
3. **Database Aggregations**: Dashboard insights (min, max, average salaries, country/currency distributions) execute SQL aggregate functions (`AVG`, `MIN`, `MAX`, `COUNT`, `GROUP BY`) directly in SQLite.
4. **Database Indexing**: Unique index on `employee.employee_number`, indexes on `employee.country`, `employee.department`, `salary_record.employee_id`, and `salary_record.effective_from`.

---

## 8. Testing Architecture

The project employs a lightweight, highly reliable testing strategy:

1. **Backend Unit & Integration Tests**: JUnit 5 & Mockito test controllers, services, repositories, and security filters.
2. **Full-System HTTP Acceptance Test**: `FullSystemAcceptanceTest.java` uses `@SpringBootTest(webEnvironment = RANDOM_PORT)` to verify real HTTP user journeys (unauthenticated access rejection, login, dashboard retrieval, employee creation, employee search by `EMP-ACC-001`, employee detail lookup, salary creation, current salary derivation, and salary history preservation).
3. **Frontend Unit Tests**: Vitest runs isolated Angular component and service unit tests.
4. **No E2E Browser Automation Framework**: Heavy browser automation frameworks (Playwright, Cypress, Selenium) were deliberately omitted. Full-system HTTP tests combined with Vitest unit tests provide faster, deterministic execution without browser flakiness.

---

## 9. CI Workflow (GitHub Actions)

The workflow defined in `.github/workflows/ci.yml` runs on `push` to `main` and `pull_request` targeting `main`:

1. **Backend Job**: Java 21 setup (Temurin), Gradle cache, runs `./gradlew build --no-daemon` (compiles and runs unit/integration/acceptance tests).
2. **Frontend Job**: Node 22 setup, NPM cache, runs `npm ci`, `npm test -- --watch=false`, and `npm run build`.
3. **Docker Job**: Validates `docker compose config` and executes `docker compose build`.
4. **Seeding in CI**: Database seeding is intentionally excluded from CI to maintain fast build times.

---

## 10. Architectural Trade-offs Summary

- **Modular Monolith vs. Microservices**: Monolith selected for operational simplicity, single-database consistency, and low latency at ~10,000 employees and ~20,000 salary records scale.
- **SQLite vs. Dedicated Database Server**: SQLite selected to eliminate external database infrastructure requirements for this assessment size.
- **HttpOnly Cookie vs. LocalStorage JWT**: HttpOnly cookies prevent client-side JavaScript from directly reading the JWT, reducing the risk of token exfiltration through client-side token access.
- **Explicit Seeder vs. Startup Seeding**: Explicit container trigger prevents data loss on container restarts.
- **Multi-Currency Statistics**: Salary statistics are grouped by currency without applying synthetic cross-currency conversion, preserving audit accuracy.
- **Full-System HTTP Acceptance Test vs. Playwright/Cypress**: HTTP acceptance tests verify real API contracts and security headers without browser automation overhead.

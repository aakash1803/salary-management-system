# Salary Management System

[![CI Workflow](https://github.com/aakash1803/salary-management-system/actions/workflows/ci.yml/badge.svg)](https://github.com/aakash1803/salary-management-system/actions/workflows/ci.yml)

A web-based application designed to help HR Managers manage employee salary information, track historical salary changes, search and filter through employee records, and analyze organization-wide salary distribution and insights.

---

## 1. Executive Summary & Architecture

The Salary Management System is built as a **Modular Monolith** featuring a Spring Boot backend, an Angular frontend, an Nginx reverse proxy, and an embedded SQLite database.

```
Browser (HTTP :80)
  │
  ▼
Nginx Frontend Container (:80)
  │  ├── Serves Production Angular Single Page Application
  │  └── Reverse proxies /api/* calls to backend container
  ▼
Spring Boot Backend Container (:8080)
  │  ├── Auth Module (HttpOnly JWT Cookie)
  │  ├── Employee Module (Server-side Search/Pagination)
  │  ├── Salary Module (Bulk Current Salary Lookup, History Preservation)
  │  └── Dashboard Module (SQL Aggregate Insights)
  ▼
Persistent SQLite Volume (/app/data/salary-management.db)
```

### Core Architecture & Technical Highlights
* **Backend Framework**: Java 21 & Spring Boot 4.1.1.
* **Frontend Framework**: Angular 21 with TypeScript.
* **Database & Persistence**: SQLite accessed via Spring Data JPA / Hibernate, stored on a persistent Docker volume (`sqlite_data`). Designed to support **~10,000 employees and ~20,000 salary records**.
* **Authentication Architecture**:
  * Login → backend validates configured HR Manager credentials → backend creates signed JWT → backend sets JWT in HttpOnly `access_token` cookie → browser automatically sends the cookie on subsequent API requests (`withCredentials: true`) → backend validates the JWT.
  * An HttpOnly cookie prevents client-side JavaScript from directly reading the JWT, reducing the risk of token exfiltration through client-side token access.
* **Reverse Proxy**: Nginx proxies `/api` to the Spring Boot backend container (`http://backend:8080`) while serving static Angular production assets with single-page application fallback routing.
* **Performance Optimizations**:
  * **Server-side Search & Pagination**: Database-level filtering (`country`, `department`, `search`) and pagination for ~10,000 employees and ~20,000 salary records.
  * **Bulk Current Salary Lookup**: Performs a bulk repository query for the requested page's employee IDs, ordering records by employee and effective date to select the active salary in `SalaryService`, avoiding N+1 queries when listing employees.
  * **SQL Aggregations**: Fast database-level calculation for min/max/average salaries and country/currency distributions on the dashboard.
* **Seeding Workflow**: Deterministic seeder populating **10,000 employees** and **19,999 salary records** using explicit administrative commands.
* **CI & Automation**: Automated multi-job GitHub Actions workflow (`.github/workflows/ci.yml`) validating Java 21 backend build/tests, Node 22 frontend build/tests, and Docker Compose configuration.

---

## 2. Quick Start & Execution Commands

### Prerequisites
* Docker & Docker Compose (for containerized execution)
* Java 21 JDK & Node.js 22 (for local native development)

---

### Option A: Running with Docker Compose (Recommended)

#### 1. Build and Start Application Services
```bash
docker compose up -d --build
```
The application will be accessible at **`http://localhost`**.

#### 2. Seed Database (Explicit Operation)
Seeding is intentionally **not run on startup** to protect existing persistent data across container restarts. To execute the deterministic seeder:
```bash
docker compose --profile tools run --rm seeder
```

#### 3. View Logs & Stop Containers
```bash
# View backend logs
docker compose logs -f backend

# Stop containers and preserve database volume
docker compose down

# Stop containers and destroy database volume
docker compose down -v
```

---

### Option B: Local Native Development

#### 1. Start Backend (Spring Boot)
```bash
cd backend
./gradlew bootRun
```
Backend runs on `http://localhost:8080`.

#### 2. Seed Local Development Database
```bash
cd backend
./gradlew seedDatabase
```
Populates `backend/data/salary-management.db` with 10,000 employees and 19,999 salary records.

#### 3. Start Frontend (Angular)
```bash
cd frontend
npm ci
npm start
```
`npm start` runs `ng serve` on `http://localhost:4200` with `/api` requests proxied to `http://localhost:8080` via `proxy.conf.json`.

---

## 3. Testing & Validation Commands

### Backend Verification (JUnit 5 & Spring Boot Test)
```bash
cd backend

# Run all backend unit and integration tests
./gradlew test

# Run full-system HTTP acceptance test
./gradlew test --tests "com.salarymanagement.acceptance.FullSystemAcceptanceTest"

# Execute complete clean build and test verification
./gradlew build
```

### Frontend Verification (Vitest & Angular CLI)
```bash
cd frontend

# Run frontend unit tests (single-run CI mode)
npm test -- --watch=false

# Build production Angular application bundle
npm run build
```

### Docker Compose Validation
```bash
# Validate Docker Compose syntax and configuration
docker compose config

# Build container images without running
docker compose build
```

---

## 4. Security & Credential Configuration

### Development & Demo Credentials
For local development, manual verification, and demonstration:
* **Username**: `hr.manager`
* **Password**: `changeme123!`

> [!WARNING]
> Documented development credentials and default secret keys are for local development/demo use only. Real production credentials and signing secrets must never be committed to source control.

### Changing Credentials and Security Configuration
All security settings are driven by Spring Boot configuration properties and can be overridden using environment variables in staging or production environments:

| Property | Environment Variable | Purpose & Guidance |
| :--- | :--- | :--- |
| `app.security.hr-manager.username` | `APP_SECURITY_HR_MANAGER_USERNAME` | HR Manager login username. |
| `app.security.hr-manager.password-hash` | `APP_SECURITY_HR_MANAGER_PASSWORD_HASH` | BCrypt password hash of the HR Manager account. Generate using standard BCrypt tools (e.g. `BCryptPasswordEncoder`). |
| `app.security.jwt.secret` | `APP_SECURITY_JWT_SECRET` | Secret key used for signing HMAC-SHA256 JWTs (must be at least 256 bits/32 bytes long). |
| `app.security.jwt.expiration-minutes` | `APP_SECURITY_JWT_EXPIRATION_MINUTES` | Token validity duration in minutes (defaults to `60`). |
| `app.security.jwt.cookie-name` | `APP_SECURITY_JWT_COOKIE_NAME` | Name of the HttpOnly authentication cookie (defaults to `access_token`). |
| `app.security.jwt.cookie-secure` | `APP_SECURITY_JWT_COOKIE_SECURE` | Set to `true` in environments served over HTTPS. |
| `app.security.cors.allowed-origin` | `APP_SECURITY_CORS_ALLOWED_ORIGIN` | Allowed origin for credentialed CORS requests (e.g., `http://localhost`). |

---

## 5. Key Design & Engineering Trade-offs

| Decision | Rationale |
| :--- | :--- |
| **Modular Monolith over Microservices** | Single deployable Spring Boot service provides domain separation (auth, employee, salary, dashboard) without the operational overhead, network latency, and distributed transaction complexities of microservices for a ~10,000-employee scale. |
| **SQLite for Persistence** | Lightweight, file-based relational storage satisfies the data scale (~10,000 employees and ~20,000 salary records) without requiring a separate database server process. Mounted on a named Docker volume (`sqlite_data`) for persistence. |
| **HttpOnly JWT Cookie Transport** | Transporting JWTs in an `HttpOnly` cookie prevents client-side JavaScript from directly reading the JWT, reducing the risk of token exfiltration through client-side token access. |
| **Explicit Destructive Seeding** | Seeding must be triggered explicitly (`docker compose --profile tools run --rm seeder` or `./gradlew seedDatabase`) rather than running on container startup to prevent accidental data overwrites upon restart. |
| **No Cross-Currency Aggregation** | The system presents salary statistics grouped by currency rather than applying arbitrary or synthetic exchange rate conversions, preserving audit accuracy. |
| **No Playwright/Cypress Overhead** | Combining Spring Boot `@SpringBootTest` full-system HTTP acceptance tests (`FullSystemAcceptanceTest.java`) with JUnit 5 integration tests and Vitest frontend unit tests delivers deterministic end-to-end coverage without slow, fragile headless browser dependencies. |

---

## 6. Project Structure

```
├── backend/
│   ├── src/main/java/com/salarymanagement/
│   │   ├── auth/           # Login, JWT issuing/verification, Security config
│   │   ├── employee/       # Employee CRUD, DB-level search/pagination
│   │   ├── salary/         # Salary history, bulk current salary derivation
│   │   ├── dashboard/      # SQL aggregate insights
│   │   └── seed/           # Deterministic 10k employee database seeder
│   ├── src/test/java/      # Unit, integration, and full-system acceptance tests
│   └── Dockerfile          # Multi-stage Java 21 build & seeder target
├── frontend/
│   ├── src/app/
│   │   ├── core/           # Auth service, guards, interceptors
│   │   ├── features/       # Auth, Dashboard, Employee, and Salary pages
│   │   └── shared/         # Reusable UI components & models
│   ├── nginx.conf          # Reverse proxy and SPA routing config
│   └── Dockerfile          # Multi-stage Node 22 build & Nginx runtime
├── docs/                   # System requirements, architecture, domain model, security docs
├── .github/workflows/ci.yml # GitHub Actions CI workflow
└── docker-compose.yml      # Orchestration for frontend, backend, and seeder
```

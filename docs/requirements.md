# Salary Management System — Product Requirements & Implementation Status

## 1. Goal

Build a web-based Salary Management System to replace an Excel-based salary management process. The system allows an HR Manager to manage employee salary information, preserve historical salary records, and understand how the organization pays its employees.

## 2. Primary User

HR Manager (`ROLE_HR_MANAGER`).

---

## 3. Functional Requirements & Implementation Status

### 3.1 Authentication
- **Requirement**: The system must require authentication. An HR Manager should be able to log in securely. Unauthenticated users must not be able to access protected employee, salary, or dashboard functionality.
- **Implemented Behavior**: Authentication is handled via `POST /api/auth/login`. On success, the backend creates a signed JWT and sets an `HttpOnly`, `SameSite=Lax` `access_token` cookie. An HttpOnly cookie prevents client-side JavaScript from directly reading the JWT, reducing the risk of token exfiltration through client-side token access. All protected endpoints reject unauthenticated requests with HTTP 401. Logout (`POST /api/auth/logout`) clears the cookie.

### 3.2 Employee Management
- **Requirement**: View employees; search employees by name or employee number; filter employees by country and department; view employee details; create employee records; edit employee information; navigate employee records using pagination. Support approximately 10,000 employees.
- **Implemented Behavior**: Supported via `GET /api/employees` (with `search`, `country`, `department`, `page`, and `size` parameters), `GET /api/employees/{id}`, `POST /api/employees`, and `PUT /api/employees/{id}`. Server-side database querying and pagination ensure fast response times for datasets of ~10,000 employees and ~20,000 salary records.

### 3.3 Salary Management
- **Requirement**: View an employee's current salary, add a salary record, change an employee's salary, and view salary history. Salary changes must preserve historical records. Salary amounts must be positive monetary values.
- **Implemented Behavior**: Supported via `GET /api/employees/{id}/salary/history` and `POST /api/employees/{id}/salary`. To list current salaries efficiently without N+1 queries, the system obtains employee IDs for the requested page, executes one bulk repository query for applicable records, orders records by employee and effective date, and selects the current active record per employee in `SalaryService`. Historical records are preserved with effective dates (`effectiveFrom`).

### 3.4 Salary Insights (Dashboard)
- **Requirement**: Provide useful salary insights including total employee count, salary statistics (min, max, average salary), country distribution, salary distribution, and payroll grouped by country/currency.
- **Implemented Behavior**: `GET /api/dashboard` delivers SQL-aggregated statistics computed directly by SQLite, returning statistics grouped by currency without synthetic cross-currency conversion.

---

## 4. Technical Architecture & Verification

- **Backend**: Java 21 with Spring Boot 4.1.1.
- **Frontend**: Angular 21 with TypeScript.
- **Reverse Proxy**: Nginx proxying `/api` requests and serving SPA routing.
- **Database**: SQLite with persistent Docker volume storage (`sqlite_data`).
- **Seeder**: Explicit deterministic seeder generating **10,000 employees** and **19,999 salary records**.
- **Containerization**: Docker Compose setup orchestrating `frontend`, `backend`, and optional `seeder` containers with healthchecks and a non-root backend runtime.
- **CI Pipeline**: GitHub Actions (`.github/workflows/ci.yml`) enforcing Java 21 backend build/tests, Node 22 frontend build/tests, and Docker Compose validation on push and pull_request to `main`.
- **Acceptance Testing**: Full-system HTTP acceptance test (`FullSystemAcceptanceTest.java`) covering end-to-end user workflows.

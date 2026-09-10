# Salary Management System — Product Requirements

## 1. Goal

Build a web-based Salary Management System to replace an Excel-based salary management process. The system should allow an HR Manager to manage employee salary information and understand how the organization pays its employees.

## 2. Primary User

HR Manager.

## 3. Functional Requirements

### 3.1 Authentication

The system must require authentication. An HR Manager should be able to log in. Unauthenticated users must not be able to access protected employee, salary, or dashboard functionality.

### 3.2 Employee Management

The HR Manager should be able to view employees; search employees by employee name or employee number; filter employees by country and department; view employee details; create employee records; edit employee information; and navigate employee records using pagination. The system should support approximately 10,000 employees.

### 3.3 Salary Management

The HR Manager should be able to view an employee's current salary, add a salary record, change an employee's salary, and view salary history. Salary changes must preserve previous salary information rather than overwrite historical records. Each salary record must contain a salary amount, currency, and effective date. Salary amount must be a valid positive monetary value; invalid salary values must be rejected.

### 3.4 Salary Insights

The system should provide useful salary insights that help the HR Manager understand how the organization pays its employees. Insights may include total employee count; salary statistics such as minimum, maximum, and average salary; employee distribution by country; salary distribution; and payroll information grouped by country and/or currency.

## 4. Data and Performance Requirements

The system should support approximately 10,000 employees. Employee searching and filtering should be efficient. Employee listing should support pagination. Salary insights should be generated efficiently. Seed data should be deterministic and reproducible. The system should provide meaningful validation and error responses.

## 5. Technology Direction

The planned technology stack is: Backend — Java 21 with Spring Boot; Frontend — Angular with TypeScript; ORM — Spring Data JPA / Hibernate; Database — SQLite; Authentication — JWT. The application should be deployable end-to-end.

## 6. Success Criteria

An authenticated HR Manager should be able to log in securely; find employees efficiently; search and filter employee records; view employee details; view current salary; record salary changes; view salary history; view useful salary insights; and work effectively with approximately 10,000 employees. The system should be reliable, maintainable, and suitable for deployment.

## 7. Project Documentation

The overall project should document important architecture, design, testing, security, performance, trade-off, deployment, and AI-assisted development decisions.

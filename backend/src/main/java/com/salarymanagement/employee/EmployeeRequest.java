package com.salarymanagement.employee;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for creating or updating an employee ({@code POST}/{@code PUT} on
 * {@code /api/employees}). Create and update use exactly the same fields and validation rules,
 * so a single DTO is used for both rather than two near-identical classes.
 *
 * <p>Max lengths mirror the {@code @Column(length = ...)} constraints already declared on
 * {@link Employee}.
 */
public record EmployeeRequest(
        @NotBlank @Size(max = 50) String employeeNumber,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 100) String country,
        @NotBlank @Size(max = 100) String department
) {
}

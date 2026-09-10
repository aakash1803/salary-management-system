package com.salarymanagement.salary;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request body for adding a salary record for an employee
 * ({@code POST /api/employees/{employeeId}/salary}). The employee is identified by the path
 * variable, not a field on this DTO.
 *
 * <p>Validation constraints mirror {@link SalaryRecord}'s own constraints, the same way
 * {@code EmployeeRequest} mirrors {@link com.salarymanagement.employee.Employee}.
 */
public record SalaryRequest(
        @NotNull @Positive BigDecimal amount,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @NotNull LocalDate effectiveFrom
) {
}

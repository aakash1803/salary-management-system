package com.salarymanagement.employee;

import com.salarymanagement.salary.SalaryRecord;

import java.math.BigDecimal;

/**
 * API-facing representation of an {@link Employee}. Deliberately omits salary history for this
 * vertical slice - only the current salary/currency (as of today) are included, so the employee
 * list view can show them without the frontend calling the salary endpoint once per employee.
 * Full salary history remains available only via the dedicated salary endpoints.
 */
public record EmployeeResponse(
        Long id,
        String employeeNumber,
        String firstName,
        String lastName,
        String email,
        String country,
        String department,
        BigDecimal currentSalary,
        String currency
) {

    /**
     * Maps an employee with no current-salary information attached. {@code currentSalary} and
     * {@code currency} are {@code null} in the resulting response; used for endpoints
     * ({@code getById}, {@code create}, {@code update}) where salary is out of scope and remains
     * the responsibility of the dedicated salary endpoints.
     */
    public static EmployeeResponse from(Employee employee) {
        return from(employee, null);
    }

    /**
     * Maps an employee together with its current salary record, if any. Passing {@code null} for
     * {@code currentSalary} (e.g. an employee with no salary record currently in effect) produces
     * {@code null} {@code currentSalary}/{@code currency} fields rather than an error.
     */
    public static EmployeeResponse from(Employee employee, SalaryRecord currentSalary) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getEmployeeNumber(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getEmail(),
                employee.getCountry(),
                employee.getDepartment(),
                currentSalary != null ? currentSalary.getAmount() : null,
                currentSalary != null ? currentSalary.getCurrency() : null
        );
    }
}

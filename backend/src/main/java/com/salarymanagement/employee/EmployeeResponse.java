package com.salarymanagement.employee;

/**
 * API-facing representation of an {@link Employee}. Deliberately omits
 * salary history for this vertical slice.
 */
public record EmployeeResponse(
        Long id,
        String employeeNumber,
        String firstName,
        String lastName,
        String email,
        String country,
        String department
) {

    public static EmployeeResponse from(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getEmployeeNumber(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getEmail(),
                employee.getCountry(),
                employee.getDepartment()
        );
    }
}

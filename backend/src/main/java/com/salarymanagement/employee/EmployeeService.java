package com.salarymanagement.employee;

import com.salarymanagement.common.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Read/search application service for employees. Returns domain entities;
 * mapping to API response shapes is the controller's responsibility.
 */
@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    /**
     * Paginated, optionally filtered employee search. Delegates entirely to
     * the database via {@link EmployeeRepository#findAll(org.springframework.data.jpa.domain.Specification, Pageable)},
     * so the full employee set is never loaded into memory.
     */
    public Page<Employee> search(String search, String country, String department, Pageable pageable) {
        return employeeRepository.findAll(
                EmployeeSpecifications.filterBy(search, country, department), pageable);
    }

    public Employee getById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Employee not found with id: " + id));
    }

    public Employee getByEmployeeNumber(String employeeNumber) {
        return employeeRepository.findByEmployeeNumber(employeeNumber)
                .orElseThrow(() -> new NotFoundException(
                        "Employee not found with employee number: " + employeeNumber));
    }
}

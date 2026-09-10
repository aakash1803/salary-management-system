package com.salarymanagement.employee;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Exercises {@link EmployeeSpecifications} against the real (in-memory
 * SQLite) test datasource via {@link EmployeeRepository}, since a
 * Specification is only meaningful once translated into an actual query.
 *
 * Uses the same {@code @SpringBootTest(webEnvironment = NONE)} +
 * {@code @Transactional} style as the other repository tests in this
 * project, rather than {@code @DataJpaTest}, which would otherwise try to
 * replace the configured SQLite datasource with an embedded database that
 * is not on this project's classpath.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class EmployeeSpecificationsTest {

    @Autowired
    private EmployeeRepository employeeRepository;

    @BeforeEach
    void setUp() {
        employeeRepository.saveAll(List.of(
                new Employee("EMP-001", "Alice", "Johnson", "alice.johnson@example.com", "United Kingdom", "Engineering"),
                new Employee("EMP-002", "Bob", "Smith", "bob.smith@example.com", "United States", "Engineering"),
                new Employee("EMP-003", "Carol", "Jones", "carol.jones@example.com", "United Kingdom", "Finance")
        ));
    }

    @Test
    void searchMatchesByFirstName() {
        List<Employee> results = employeeRepository.findAll(EmployeeSpecifications.search("alice"));
        assertEquals(1, results.size());
        assertEquals("EMP-001", results.get(0).getEmployeeNumber());
    }

    @Test
    void searchMatchesByLastName() {
        List<Employee> results = employeeRepository.findAll(EmployeeSpecifications.search("smith"));
        assertEquals(1, results.size());
        assertEquals("EMP-002", results.get(0).getEmployeeNumber());
    }

    @Test
    void searchMatchesByEmployeeNumber() {
        List<Employee> results = employeeRepository.findAll(EmployeeSpecifications.search("emp-003"));
        assertEquals(1, results.size());
        assertEquals("Carol", results.get(0).getFirstName());
    }

    @Test
    void searchIsCaseInsensitiveAndMatchesPartialText() {
        List<Employee> results = employeeRepository.findAll(EmployeeSpecifications.search("OHN"));
        assertEquals(1, results.size());
        assertEquals("EMP-001", results.get(0).getEmployeeNumber());
    }

    @Test
    void countryFilterMatchesExactCountryOnly() {
        List<Employee> results = employeeRepository.findAll(EmployeeSpecifications.hasCountry("United Kingdom"));
        assertEquals(2, results.size());
    }

    @Test
    void departmentFilterMatchesExactDepartmentOnly() {
        List<Employee> results = employeeRepository.findAll(EmployeeSpecifications.hasDepartment("Engineering"));
        assertEquals(2, results.size());
    }

    @Test
    void combinesSearchCountryAndDepartmentFilters() {
        List<Employee> results = employeeRepository.findAll(
                EmployeeSpecifications.filterBy("o", "United Kingdom", "Engineering"));
        assertEquals(1, results.size());
        assertEquals("EMP-001", results.get(0).getEmployeeNumber());
    }

    @Test
    void noFiltersReturnsEveryEmployee() {
        List<Employee> results = employeeRepository.findAll(EmployeeSpecifications.filterBy(null, null, null));
        assertEquals(3, results.size());
    }
}

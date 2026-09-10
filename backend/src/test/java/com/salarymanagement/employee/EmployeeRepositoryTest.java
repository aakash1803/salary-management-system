package com.salarymanagement.employee;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Persistence tests for {@link Employee} / {@link EmployeeRepository}, run against the project's
 * real SQLite configuration (see src/test/resources/application.properties). Each test runs in
 * its own transaction which is rolled back afterwards, so tests are independent and repeatable.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class EmployeeRepositoryTest {

    @Autowired
    private EmployeeRepository employeeRepository;

    private Employee newEmployee(String employeeNumber) {
        return new Employee(employeeNumber, "Ada", "Lovelace", "ada@example.com", "UK", "Engineering");
    }

    @Test
    void savesAndReloadsAnEmployee() {
        Employee saved = employeeRepository.saveAndFlush(newEmployee("EMP-001"));

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());

        Employee reloaded = employeeRepository.findById(saved.getId()).orElseThrow();
        assertEquals("EMP-001", reloaded.getEmployeeNumber());
        assertEquals("Ada", reloaded.getFirstName());
        assertEquals("UK", reloaded.getCountry());
        assertEquals("Engineering", reloaded.getDepartment());
    }

    @Test
    void findByEmployeeNumberFindsAnExistingEmployee() {
        employeeRepository.saveAndFlush(newEmployee("EMP-002"));

        Optional<Employee> found = employeeRepository.findByEmployeeNumber("EMP-002");
        assertTrue(found.isPresent());
        assertTrue(employeeRepository.existsByEmployeeNumber("EMP-002"));
        assertFalse(employeeRepository.existsByEmployeeNumber("EMP-DOES-NOT-EXIST"));
    }


    @Test
    void rejectsAnEmployeeMissingARequiredField() {
        Employee missingFirstName = new Employee("EMP-004", "", "Lovelace", "ada@example.com", "UK", "Engineering");

        assertThrows(ConstraintViolationException.class,
                () -> employeeRepository.saveAndFlush(missingFirstName));
    }
}

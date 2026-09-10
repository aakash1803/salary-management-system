package com.salarymanagement.employee;

import com.salarymanagement.common.ConflictException;
import com.salarymanagement.common.NotFoundException;
import org.hibernate.exception.GenericJDBCException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.jpa.JpaSystemException;
import org.sqlite.SQLiteErrorCode;
import org.sqlite.SQLiteException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure Mockito unit tests for {@link EmployeeService} - no Spring context is
 * loaded, so these run fast and do not touch the database.
 */
@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    private EmployeeService employeeService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        employeeService = new EmployeeService(employeeRepository);
    }

    private Employee sampleEmployee() {
        return new Employee("EMP-001", "Alice", "Johnson",
                "alice.johnson@example.com", "United Kingdom", "Engineering");
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchReturnsThePageProducedByTheRepository() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Employee> page = new PageImpl<>(List.of(sampleEmployee()), pageable, 1);

        when(employeeRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<Employee> result = employeeService.search("alice", null, null, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("EMP-001", result.getContent().get(0).getEmployeeNumber());
    }

    @Test
    void getByIdReturnsTheEmployeeWhenFound() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(sampleEmployee()));

        Employee result = employeeService.getById(1L);

        assertEquals("EMP-001", result.getEmployeeNumber());
    }

    @Test
    void getByIdThrowsNotFoundExceptionWhenMissing() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> employeeService.getById(99L));
    }

    @Test
    void getByEmployeeNumberReturnsTheEmployeeWhenFound() {
        when(employeeRepository.findByEmployeeNumber("EMP-001")).thenReturn(Optional.of(sampleEmployee()));

        Employee result = employeeService.getByEmployeeNumber("EMP-001");

        assertEquals("Alice", result.getFirstName());
    }

    @Test
    void getByEmployeeNumberThrowsNotFoundExceptionWhenMissing() {
        when(employeeRepository.findByEmployeeNumber("EMP-999")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> employeeService.getByEmployeeNumber("EMP-999"));
    }

    @Test
    void createSavesAndReturnsANewEmployee() {
        EmployeeRequest request = new EmployeeRequest(
                "EMP-002", "Bob", "Smith", "bob.smith@example.com", "United States", "Sales");
        Employee saved = new Employee("EMP-002", "Bob", "Smith",
                "bob.smith@example.com", "United States", "Sales");

        when(employeeRepository.existsByEmployeeNumber("EMP-002")).thenReturn(false);
        when(employeeRepository.saveAndFlush(any(Employee.class))).thenReturn(saved);

        Employee result = employeeService.create(request);

        assertEquals("EMP-002", result.getEmployeeNumber());
        assertEquals("Bob", result.getFirstName());
    }

    @Test
    void createRejectsADuplicateEmployeeNumberDetectedUpfront() {
        EmployeeRequest request = new EmployeeRequest(
                "EMP-001", "Bob", "Smith", "bob.smith@example.com", "United States", "Sales");

        when(employeeRepository.existsByEmployeeNumber("EMP-001")).thenReturn(true);

        assertThrows(ConflictException.class, () -> employeeService.create(request));
        verify(employeeRepository, never()).saveAndFlush(any());
    }

    @Test
    void createTranslatesADatabaseConstraintViolationIntoAConflictException() {
        // Covers the race-condition path: existsByEmployeeNumber passes, but the database's
        // unique constraint still rejects the insert (e.g. a concurrent request won the race).
        EmployeeRequest request = new EmployeeRequest(
                "EMP-003", "Bob", "Smith", "bob.smith@example.com", "United States", "Sales");

        when(employeeRepository.existsByEmployeeNumber("EMP-003")).thenReturn(false);
        when(employeeRepository.saveAndFlush(any(Employee.class)))
                .thenThrow(new DataIntegrityViolationException("unique constraint violation"));

        assertThrows(ConflictException.class, () -> employeeService.create(request));
    }

    @Test
    void createTranslatesASqliteUniqueConstraintViolationIntoAConflictException() {
        // Covers this project's SQLite-dialect-specific exception-classification gap: unlike
        // most dialects, org.hibernate.community.dialect.SQLiteDialect does not classify a
        // SQLITE_CONSTRAINT_UNIQUE violation into DataIntegrityViolationException - it surfaces
        // as JpaSystemException wrapping a GenericJDBCException wrapping the driver's
        // SQLiteException, confirmed against a real test run. EmployeeService.create/update must
        // still translate this into ConflictException by walking the cause chain (see
        // EmployeeService.isUniqueConstraintViolation).
        EmployeeRequest request = new EmployeeRequest(
                "EMP-004", "Bob", "Smith", "bob.smith@example.com", "United States", "Sales");

        SQLiteException sqliteException = new SQLiteException(
                "[SQLITE_CONSTRAINT_UNIQUE] A UNIQUE constraint failed "
                        + "(UNIQUE constraint failed: employee.employee_number)",
                SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE);
        GenericJDBCException genericJdbcException =
                new GenericJDBCException("could not execute statement", sqliteException);
        JpaSystemException jpaSystemException = new JpaSystemException(genericJdbcException);

        when(employeeRepository.existsByEmployeeNumber("EMP-004")).thenReturn(false);
        when(employeeRepository.saveAndFlush(any(Employee.class))).thenThrow(jpaSystemException);

        assertThrows(ConflictException.class, () -> employeeService.create(request));
    }

    @Test
    void updateModifiesAndReturnsTheEmployee() {
        Employee existing = sampleEmployee();
        EmployeeRequest request = new EmployeeRequest(
                "EMP-001", "Alicia", "Johnson", "alicia.johnson@example.com", "United Kingdom", "Product");

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(employeeRepository.saveAndFlush(any(Employee.class))).thenReturn(existing);

        Employee result = employeeService.update(1L, request);

        assertEquals("Alicia", result.getFirstName());
        assertEquals("Product", result.getDepartment());
    }

    @Test
    void updateThrowsNotFoundExceptionWhenEmployeeDoesNotExist() {
        EmployeeRequest request = new EmployeeRequest(
                "EMP-001", "Alicia", "Johnson", "alicia.johnson@example.com", "United Kingdom", "Product");

        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> employeeService.update(99L, request));
    }

    @Test
    void updateRejectsAnEmployeeNumberAlreadyUsedByAnotherEmployee() {
        Employee existing = sampleEmployee(); // employeeNumber = EMP-001
        EmployeeRequest request = new EmployeeRequest(
                "EMP-999", "Alice", "Johnson", "alice.johnson@example.com", "United Kingdom", "Engineering");

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(employeeRepository.existsByEmployeeNumber("EMP-999")).thenReturn(true);

        assertThrows(ConflictException.class, () -> employeeService.update(1L, request));
        verify(employeeRepository, never()).saveAndFlush(any());
    }
}

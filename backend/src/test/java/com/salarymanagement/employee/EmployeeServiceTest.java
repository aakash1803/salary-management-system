package com.salarymanagement.employee;

import com.salarymanagement.common.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
}

package com.salarymanagement.salary;

import com.salarymanagement.common.NotFoundException;
import com.salarymanagement.employee.Employee;
import com.salarymanagement.employee.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure Mockito unit tests for {@link SalaryService} - no Spring context is loaded, so these run
 * fast and do not touch the database. Actual date-boundary filtering (a future-dated record not
 * yet being "current") is a database query concern and is verified against the real SQLite
 * datasource in {@code SalaryRecordRepositoryTest}; here we only verify that the service asks the
 * repository the right question (today's date) and maps/reacts to the result correctly.
 */
@ExtendWith(MockitoExtension.class)
class SalaryServiceTest {

    @Mock
    private SalaryRecordRepository salaryRecordRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    private SalaryService salaryService;

    @BeforeEach
    void setUp() {
        salaryService = new SalaryService(salaryRecordRepository, employeeRepository);
    }

    private Employee sampleEmployee() {
        return new Employee("EMP-201", "Ada", "Lovelace", "ada@example.com", "UK", "Engineering");
    }

    @Test
    void addSalarySavesAndReturnsTheNewRecord() {
        Employee employee = sampleEmployee();
        LocalDate effectiveFrom = LocalDate.now();
        SalaryRequest request = new SalaryRequest(new BigDecimal("50000.00"), "GBP", effectiveFrom);
        SalaryRecord saved = new SalaryRecord(employee, request.amount(), request.currency(), request.effectiveFrom());

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(salaryRecordRepository.saveAndFlush(any(SalaryRecord.class))).thenReturn(saved);

        SalaryRecord result = salaryService.addSalary(1L, request);

        ArgumentCaptor<SalaryRecord> salaryRecordCaptor = ArgumentCaptor.forClass(SalaryRecord.class);
        verify(salaryRecordRepository, times(1)).saveAndFlush(salaryRecordCaptor.capture());
        SalaryRecord persisted = salaryRecordCaptor.getValue();
        assertEquals(employee, persisted.getEmployee());
        assertEquals(new BigDecimal("50000.00"), persisted.getAmount());
        assertEquals("GBP", persisted.getCurrency());
        assertEquals(effectiveFrom, persisted.getEffectiveFrom());

        assertEquals(new BigDecimal("50000.00"), result.getAmount());
        assertEquals("GBP", result.getCurrency());
    }

    @Test
    void addSalaryThrowsNotFoundExceptionWhenEmployeeDoesNotExist() {
        SalaryRequest request = new SalaryRequest(new BigDecimal("50000.00"), "GBP", LocalDate.now());

        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> salaryService.addSalary(99L, request));
        verify(salaryRecordRepository, never()).saveAndFlush(any());
    }

    @Test
    void getCurrentSalaryReturnsTheRecordProducedByTheRepository() {
        Employee employee = sampleEmployee();
        SalaryRecord currentRecord = new SalaryRecord(employee, new BigDecimal("60000.00"), "GBP", LocalDate.now());

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(salaryRecordRepository
                .findFirstByEmployeeAndEffectiveFromLessThanEqualOrderByEffectiveFromDescIdDesc(eq(employee), any()))
                .thenReturn(Optional.of(currentRecord));

        SalaryRecord result = salaryService.getCurrentSalary(1L);

        assertEquals(new BigDecimal("60000.00"), result.getAmount());
    }

    @Test
    void getCurrentSalaryQueriesAsOfTodaysDateSoFutureDatedRecordsAreExcluded() {
        // The exclusion of future-dated records is enforced by the repository query itself
        // (findFirstBy...EffectiveFromLessThanEqual...), which is only correct if the service
        // passes today's date as the "as of" bound - this test verifies that it does.
        Employee employee = sampleEmployee();
        SalaryRecord currentRecord = new SalaryRecord(employee, new BigDecimal("60000.00"), "GBP", LocalDate.now());
        ArgumentCaptor<LocalDate> asOfDateCaptor = ArgumentCaptor.forClass(LocalDate.class);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(salaryRecordRepository.findFirstByEmployeeAndEffectiveFromLessThanEqualOrderByEffectiveFromDescIdDesc(
                eq(employee), asOfDateCaptor.capture()))
                .thenReturn(Optional.of(currentRecord));

        salaryService.getCurrentSalary(1L);

        assertEquals(LocalDate.now(), asOfDateCaptor.getValue());
    }

    @Test
    void getCurrentSalaryThrowsNotFoundExceptionWhenNoRecordIsCurrentlyEffective() {
        Employee employee = sampleEmployee();

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(salaryRecordRepository
                .findFirstByEmployeeAndEffectiveFromLessThanEqualOrderByEffectiveFromDescIdDesc(eq(employee), any()))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> salaryService.getCurrentSalary(1L));
    }

    @Test
    void getCurrentSalaryThrowsNotFoundExceptionWhenEmployeeDoesNotExist() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> salaryService.getCurrentSalary(99L));
    }

    @Test
    void getSalaryHistoryReturnsAllRecordsInRepositoryOrder() {
        Employee employee = sampleEmployee();
        SalaryRecord newer = new SalaryRecord(employee, new BigDecimal("55000.00"), "GBP", LocalDate.now());
        SalaryRecord older = new SalaryRecord(employee, new BigDecimal("50000.00"), "GBP", LocalDate.now().minusYears(1));
        List<SalaryRecord> orderedHistory = List.of(newer, older);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(salaryRecordRepository.findByEmployeeOrderByEffectiveFromDescIdDesc(employee)).thenReturn(orderedHistory);

        List<SalaryRecord> result = salaryService.getSalaryHistory(1L);

        assertEquals(orderedHistory, result);
    }

    @Test
    void getSalaryHistoryReturnsEmptyListForAnExistingEmployeeWithNoRecords() {
        Employee employee = sampleEmployee();

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(salaryRecordRepository.findByEmployeeOrderByEffectiveFromDescIdDesc(employee)).thenReturn(List.of());

        List<SalaryRecord> result = salaryService.getSalaryHistory(1L);

        assertTrue(result.isEmpty());
    }

    @Test
    void getSalaryHistoryThrowsNotFoundExceptionWhenEmployeeDoesNotExist() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> salaryService.getSalaryHistory(99L));
    }
}

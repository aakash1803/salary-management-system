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
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

    @Test
    void getCurrentSalariesByEmployeeIdsReturnsEmptyMapForEmptyInputWithoutQueryingTheRepository() {
        Map<Long, SalaryRecord> result = salaryService.getCurrentSalariesByEmployeeIds(List.of());

        assertTrue(result.isEmpty());
        // Stronger than checking one specific method wasn't called: the repository must not be
        // touched in any way for an empty employee id list.
        verifyNoInteractions(salaryRecordRepository);
    }

    @Test
    void getCurrentSalariesByEmployeeIdsMapsEachEmployeeToItsFirstReturnedRecord() {
        // The repository is documented to return records grouped by employee id with the
        // current record first per group; the service just needs to keep that first one.
        // Ids are assigned via reflection (mirroring @GeneratedValue/IDENTITY behavior on a real
        // save) since these entities are never persisted in this pure Mockito unit test.
        Employee employeeOne = new Employee("EMP-301", "Ada", "Lovelace", "ada@example.com", "UK", "Engineering");
        ReflectionTestUtils.setField(employeeOne, "id", 1L);
        Employee employeeTwo = new Employee("EMP-302", "Grace", "Hopper", "grace@example.com", "US", "Engineering");
        ReflectionTestUtils.setField(employeeTwo, "id", 2L);
        SalaryRecord currentForOne = new SalaryRecord(employeeOne, new BigDecimal("70000.00"), "GBP", LocalDate.now());
        SalaryRecord olderForOne = new SalaryRecord(employeeOne, new BigDecimal("60000.00"), "GBP", LocalDate.now().minusYears(1));
        SalaryRecord currentForTwo = new SalaryRecord(employeeTwo, new BigDecimal("90000.00"), "USD", LocalDate.now());

        when(salaryRecordRepository
                .findByEmployee_IdInAndEffectiveFromLessThanEqualOrderByEmployee_IdAscEffectiveFromDescIdDesc(any(), any()))
                .thenReturn(List.of(currentForOne, olderForOne, currentForTwo));

        Map<Long, SalaryRecord> result = salaryService.getCurrentSalariesByEmployeeIds(List.of(1L, 2L));

        assertEquals(2, result.size());
        assertEquals(currentForOne, result.get(employeeOne.getId()));
        assertEquals(currentForTwo, result.get(employeeTwo.getId()));
    }

    @Test
    void getCurrentSalariesByEmployeeIdsOmitsEmployeesWithNoApplicableRecord() {
        when(salaryRecordRepository
                .findByEmployee_IdInAndEffectiveFromLessThanEqualOrderByEmployee_IdAscEffectiveFromDescIdDesc(any(), any()))
                .thenReturn(List.of());

        Map<Long, SalaryRecord> result = salaryService.getCurrentSalariesByEmployeeIds(List.of(1L, 2L));

        assertTrue(result.isEmpty());
    }

    @Test
    void getCurrentSalariesByEmployeeIdsQueriesAsOfTodaysDate() {
        ArgumentCaptor<LocalDate> asOfDateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        when(salaryRecordRepository
                .findByEmployee_IdInAndEffectiveFromLessThanEqualOrderByEmployee_IdAscEffectiveFromDescIdDesc(
                        any(), asOfDateCaptor.capture()))
                .thenReturn(List.of());

        salaryService.getCurrentSalariesByEmployeeIds(List.of(1L));

        assertEquals(LocalDate.now(), asOfDateCaptor.getValue());
    }

    @Test
    void getCurrentSalariesByEmployeeIdsKeepsTheHigherIdRecordWhenTwoShareTheSameEffectiveFromDate() {
        // Business rule: when two applicable records for the same employee share the same
        // effectiveFrom date, the one with the higher salary-record id wins. The database-level
        // ordering that produces this (employee_id ASC, effectiveFrom DESC, id DESC) is verified
        // against the real SQLite datasource in
        // SalaryRecordRepositoryTest#findByEmployee_IdInBreaksATieOnIdenticalEffectiveFromByHighestIdWinning.
        // This test verifies the service side of the same rule: given the repository already
        // returns the higher-id record first for an employee (exactly as that ordering
        // guarantees), the service must keep that one rather than the lower-id record.
        Employee employee = new Employee("EMP-303", "Alan", "Turing", "alan@example.com", "UK", "Engineering");
        ReflectionTestUtils.setField(employee, "id", 1L);
        LocalDate sameDate = LocalDate.now();

        SalaryRecord higherId = new SalaryRecord(employee, new BigDecimal("52000.00"), "GBP", sameDate);
        ReflectionTestUtils.setField(higherId, "id", 2L);
        SalaryRecord lowerId = new SalaryRecord(employee, new BigDecimal("50000.00"), "GBP", sameDate);
        ReflectionTestUtils.setField(lowerId, "id", 1L);

        // Mirrors the real repository's own tie-break ordering: higher id first per employee.
        when(salaryRecordRepository
                .findByEmployee_IdInAndEffectiveFromLessThanEqualOrderByEmployee_IdAscEffectiveFromDescIdDesc(any(), any()))
                .thenReturn(List.of(higherId, lowerId));

        Map<Long, SalaryRecord> result = salaryService.getCurrentSalariesByEmployeeIds(List.of(1L));

        assertEquals(1, result.size());
        assertEquals(2L, result.get(1L).getId(), "the higher-id record must win the tie");
        assertEquals(new BigDecimal("52000.00"), result.get(1L).getAmount());
    }

    @Test
    void getCurrentSalariesByEmployeeIdsReturnsExactlyOneRecordPerRequestedEmployee() {
        // Even when the repository returns several applicable records for the same employee
        // (grouped together, per its documented ordering), the resulting map must end up with
        // exactly one entry per employee - never more than one candidate surviving per id.
        Employee employeeOne = new Employee("EMP-304", "Grace", "Hopper", "grace@example.com", "US", "Engineering");
        ReflectionTestUtils.setField(employeeOne, "id", 1L);
        Employee employeeTwo = new Employee("EMP-305", "Ada", "Lovelace", "ada@example.com", "UK", "Engineering");
        ReflectionTestUtils.setField(employeeTwo, "id", 2L);

        SalaryRecord currentForOne = new SalaryRecord(employeeOne, new BigDecimal("80000.00"), "USD", LocalDate.now());
        SalaryRecord olderForOne = new SalaryRecord(employeeOne, new BigDecimal("70000.00"), "USD", LocalDate.now().minusYears(1));
        SalaryRecord evenOlderForOne = new SalaryRecord(employeeOne, new BigDecimal("60000.00"), "USD", LocalDate.now().minusYears(2));
        SalaryRecord currentForTwo = new SalaryRecord(employeeTwo, new BigDecimal("90000.00"), "GBP", LocalDate.now());

        when(salaryRecordRepository
                .findByEmployee_IdInAndEffectiveFromLessThanEqualOrderByEmployee_IdAscEffectiveFromDescIdDesc(any(), any()))
                .thenReturn(List.of(currentForOne, olderForOne, evenOlderForOne, currentForTwo));

        Map<Long, SalaryRecord> result = salaryService.getCurrentSalariesByEmployeeIds(List.of(1L, 2L));

        assertEquals(2, result.size(), "exactly one current salary record per requested employee");
        assertEquals(currentForOne, result.get(1L));
        assertEquals(currentForTwo, result.get(2L));
    }

    @Test
    void getCurrentSalariesByEmployeeIdsOmitsOnlyEmployeesWithoutAnApplicableRecordWhileKeepingOthers() {
        // Requesting three employee ids where only one has an applicable record: the map must
        // contain exactly that one, and the other two must be absent entirely - not present with
        // a null value.
        Employee employeeWithSalary = new Employee("EMP-306", "Marie", "Curie", "marie@example.com", "FR", "Engineering");
        ReflectionTestUtils.setField(employeeWithSalary, "id", 10L);
        SalaryRecord currentSalary = new SalaryRecord(employeeWithSalary, new BigDecimal("65000.00"), "EUR", LocalDate.now());

        when(salaryRecordRepository
                .findByEmployee_IdInAndEffectiveFromLessThanEqualOrderByEmployee_IdAscEffectiveFromDescIdDesc(any(), any()))
                .thenReturn(List.of(currentSalary));

        Map<Long, SalaryRecord> result = salaryService.getCurrentSalariesByEmployeeIds(List.of(10L, 20L, 30L));

        assertEquals(1, result.size());
        assertTrue(result.containsKey(10L));
        assertFalse(result.containsKey(20L), "employee with no applicable salary record must be absent, not null");
        assertFalse(result.containsKey(30L), "employee with no applicable salary record must be absent, not null");
    }
}

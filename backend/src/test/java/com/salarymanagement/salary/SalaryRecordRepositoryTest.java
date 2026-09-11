package com.salarymanagement.salary;

import com.salarymanagement.employee.Employee;
import com.salarymanagement.employee.EmployeeRepository;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Persistence tests for {@link SalaryRecord} / {@link SalaryRecordRepository}, run against the
 * project's real SQLite configuration (see src/test/resources/application.properties). Each test
 * runs in its own transaction which is rolled back afterwards, so tests are independent and
 * repeatable.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class SalaryRecordRepositoryTest {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private SalaryRecordRepository salaryRecordRepository;

    private Employee saveEmployee(String employeeNumber) {
        return employeeRepository.saveAndFlush(
                new Employee(employeeNumber, "Ada", "Lovelace", "ada@example.com", "UK", "Engineering"));
    }

    private SalaryRecord saveSalaryRecord(Employee employee, String amount, LocalDate effectiveFrom) {
        return salaryRecordRepository.saveAndFlush(
                new SalaryRecord(employee, new BigDecimal(amount), "GBP", effectiveFrom));
    }

    @Test
    void savesASalaryRecordLinkedToItsEmployee() {
        Employee employee = saveEmployee("EMP-101");

        SalaryRecord saved = saveSalaryRecord(employee, "50000.00", LocalDate.now());

        assertEquals(employee.getId(), saved.getEmployee().getId());
        assertTrue(saved.getId() != null);
        assertTrue(saved.getCreatedAt() != null);
    }

    @Test
    void salaryHistoryIsPreservedWhenSalaryChanges() {
        Employee employee = saveEmployee("EMP-102");

        saveSalaryRecord(employee, "50000.00", LocalDate.now().minusYears(1));
        saveSalaryRecord(employee, "55000.00", LocalDate.now());

        List<SalaryRecord> history = salaryRecordRepository.findByEmployeeOrderByEffectiveFromDescIdDesc(employee);

        // Both records still exist; the earlier one was never overwritten or deleted.
        assertEquals(2, history.size());
        assertEquals(new BigDecimal("55000.00"), history.get(0).getAmount());
        assertEquals(new BigDecimal("50000.00"), history.get(1).getAmount());
    }

    @Test
    void findsTheLatestApplicableSalaryAsOfToday() {
        Employee employee = saveEmployee("EMP-103");

        saveSalaryRecord(employee, "50000.00", LocalDate.now().minusYears(2));
        saveSalaryRecord(employee, "55000.00", LocalDate.now().minusYears(1));
        saveSalaryRecord(employee, "60000.00", LocalDate.now());

        Optional<SalaryRecord> current = salaryRecordRepository
                .findFirstByEmployeeAndEffectiveFromLessThanEqualOrderByEffectiveFromDescIdDesc(
                        employee, LocalDate.now());

        assertTrue(current.isPresent());
        assertEquals(new BigDecimal("60000.00"), current.get().getAmount());
    }

    @Test
    void futureDatedSalaryRecordIsAllowedButNotYetCurrent() {
        Employee employee = saveEmployee("EMP-104");

        saveSalaryRecord(employee, "60000.00", LocalDate.now());
        SalaryRecord future = saveSalaryRecord(employee, "70000.00", LocalDate.now().plusMonths(6));

        // The future-dated record persists successfully and shows up in full history...
        List<SalaryRecord> history = salaryRecordRepository.findByEmployeeOrderByEffectiveFromDescIdDesc(employee);
        assertEquals(2, history.size());
        assertEquals(future.getId(), history.get(0).getId());

        // ...but it is not the "current" salary as of today.
        Optional<SalaryRecord> current = salaryRecordRepository
                .findFirstByEmployeeAndEffectiveFromLessThanEqualOrderByEffectiveFromDescIdDesc(
                        employee, LocalDate.now());
        assertTrue(current.isPresent());
        assertEquals(new BigDecimal("60000.00"), current.get().getAmount());

        // As of the future date, it does become current.
        Optional<SalaryRecord> asOfFutureDate = salaryRecordRepository
                .findFirstByEmployeeAndEffectiveFromLessThanEqualOrderByEffectiveFromDescIdDesc(
                        employee, LocalDate.now().plusMonths(6));
        assertTrue(asOfFutureDate.isPresent());
        assertEquals(new BigDecimal("70000.00"), asOfFutureDate.get().getAmount());
    }

    @Test
    void rejectsAZeroSalaryAmount() {
        Employee employee = saveEmployee("EMP-105");

        assertThrows(ConstraintViolationException.class,
                () -> saveSalaryRecord(employee, "0.00", LocalDate.now()));
    }

    @Test
    void rejectsANegativeSalaryAmount() {
        Employee employee = saveEmployee("EMP-108");

        assertThrows(ConstraintViolationException.class,
                () -> saveSalaryRecord(employee, "-100.00", LocalDate.now()));
    }

    @Test
    void rejectsAMissingCurrency() {
        Employee employee = saveEmployee("EMP-106");
        SalaryRecord missingCurrency = new SalaryRecord(employee, new BigDecimal("1000.00"), "", LocalDate.now());

        assertThrows(ConstraintViolationException.class,
                () -> salaryRecordRepository.saveAndFlush(missingCurrency));
    }

    @Test
    void rejectsAMissingEffectiveDate() {
        Employee employee = saveEmployee("EMP-107");
        SalaryRecord missingEffectiveFrom = new SalaryRecord(employee, new BigDecimal("1000.00"), "GBP", null);

        assertThrows(ConstraintViolationException.class,
                () -> salaryRecordRepository.saveAndFlush(missingEffectiveFrom));
    }

    @Test
    void rejectsAMissingEmployee() {
        SalaryRecord missingEmployee = new SalaryRecord(null, new BigDecimal("1000.00"), "GBP", LocalDate.now());

        assertThrows(ConstraintViolationException.class,
                () -> salaryRecordRepository.saveAndFlush(missingEmployee));
    }

    @Test
    void breaksATieOnIdenticalEffectiveFromByIdDescending() {
        // Two records sharing the same effectiveFrom is unusual but explicitly allowed by the
        // domain rules; ordering must still be deterministic, using id (i.e. insertion order via
        // IDENTITY generation) as the tie-breaker, most recently inserted first.
        Employee employee = saveEmployee("EMP-109");
        LocalDate sameDate = LocalDate.now();

        SalaryRecord first = saveSalaryRecord(employee, "50000.00", sameDate);
        SalaryRecord second = saveSalaryRecord(employee, "52000.00", sameDate);

        assertTrue(second.getId() > first.getId());

        Optional<SalaryRecord> current = salaryRecordRepository
                .findFirstByEmployeeAndEffectiveFromLessThanEqualOrderByEffectiveFromDescIdDesc(
                        employee, LocalDate.now());
        assertTrue(current.isPresent());
        assertEquals(second.getId(), current.get().getId());
        assertEquals(new BigDecimal("52000.00"), current.get().getAmount());

        List<SalaryRecord> history = salaryRecordRepository.findByEmployeeOrderByEffectiveFromDescIdDesc(employee);
        assertEquals(2, history.size());
        assertEquals(second.getId(), history.get(0).getId());
        assertEquals(first.getId(), history.get(1).getId());
    }

    @Test
    void findByEmployee_IdInReturnsTheApplicableRecordForEachRequestedEmployee() {
        Employee employeeOne = saveEmployee("EMP-110");
        Employee employeeTwo = saveEmployee("EMP-111");
        Employee employeeThree = saveEmployee("EMP-112");

        saveSalaryRecord(employeeOne, "50000.00", LocalDate.now().minusYears(1));
        SalaryRecord currentForOne = saveSalaryRecord(employeeOne, "55000.00", LocalDate.now());
        SalaryRecord currentForTwo = saveSalaryRecord(employeeTwo, "60000.00", LocalDate.now().minusMonths(1));
        // employeeThree has no salary record at all.

        List<SalaryRecord> results = salaryRecordRepository
                .findByEmployee_IdInAndEffectiveFromLessThanEqualOrderByEmployee_IdAscEffectiveFromDescIdDesc(
                        List.of(employeeOne.getId(), employeeTwo.getId(), employeeThree.getId()), LocalDate.now());

        // Every applicable record for employeeOne and employeeTwo is returned (grouped by
        // employee, current record first per group) - employeeThree contributes nothing.
        assertEquals(3, results.size());
        assertEquals(currentForOne.getId(), results.get(0).getId());
        assertEquals(currentForTwo.getId(), results.get(2).getId());
    }

    @Test
    void findByEmployee_IdInExcludesFutureDatedRecordsAndEmployeesOutsideTheRequestedIds() {
        Employee employeeOne = saveEmployee("EMP-113");
        Employee employeeTwo = saveEmployee("EMP-114");

        SalaryRecord currentForOne = saveSalaryRecord(employeeOne, "45000.00", LocalDate.now());
        saveSalaryRecord(employeeOne, "99000.00", LocalDate.now().plusMonths(6));
        saveSalaryRecord(employeeTwo, "70000.00", LocalDate.now());

        List<SalaryRecord> results = salaryRecordRepository
                .findByEmployee_IdInAndEffectiveFromLessThanEqualOrderByEmployee_IdAscEffectiveFromDescIdDesc(
                        List.of(employeeOne.getId()), LocalDate.now());

        assertEquals(1, results.size());
        assertEquals(currentForOne.getId(), results.get(0).getId());
    }

    @Test
    void findByEmployee_IdInReturnsEmptyListWhenNoIdsAreApplicable() {
        Employee employee = saveEmployee("EMP-115");
        saveSalaryRecord(employee, "40000.00", LocalDate.now().plusMonths(1));

        List<SalaryRecord> results = salaryRecordRepository
                .findByEmployee_IdInAndEffectiveFromLessThanEqualOrderByEmployee_IdAscEffectiveFromDescIdDesc(
                        List.of(employee.getId()), LocalDate.now());

        assertTrue(results.isEmpty());
    }

    @Test
    void findByEmployee_IdInBreaksATieOnIdenticalEffectiveFromByHighestIdWinning() {
        // Same business rule as breaksATieOnIdenticalEffectiveFromByIdDescending above, but for
        // the bulk lookup that powers the employee list's current-salary enrichment: two records
        // sharing the same effectiveFrom for one employee must still resolve deterministically,
        // with the higher-id (most recently inserted) record ordered first within that
        // employee's group - the service (SalaryService#getCurrentSalariesByEmployeeIds) relies
        // on exactly this ordering, since it just keeps whichever record comes first per
        // employee.
        Employee employee = saveEmployee("EMP-116");
        LocalDate sameDate = LocalDate.now();

        SalaryRecord lowerId = saveSalaryRecord(employee, "50000.00", sameDate);
        SalaryRecord higherId = saveSalaryRecord(employee, "52000.00", sameDate);
        assertTrue(higherId.getId() > lowerId.getId());

        List<SalaryRecord> results = salaryRecordRepository
                .findByEmployee_IdInAndEffectiveFromLessThanEqualOrderByEmployee_IdAscEffectiveFromDescIdDesc(
                        List.of(employee.getId()), LocalDate.now());

        assertEquals(2, results.size());
        assertEquals(higherId.getId(), results.get(0).getId(), "the higher-id record must win the tie and sort first");
        assertEquals(lowerId.getId(), results.get(1).getId());
    }
}

package com.salarymanagement.seed;

import com.salarymanagement.employee.Employee;
import com.salarymanagement.employee.EmployeeRepository;
import com.salarymanagement.salary.SalaryRecord;
import com.salarymanagement.salary.SalaryRecordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for {@link DatabaseSeederService#seed()} - which is destructive
 * ({@code deleteAllInBatch()} on both tables) - so this class must never run against the real
 * development database ({@code ./data/salary-management.db}) or against the anonymous shared
 * in-memory database ({@code jdbc:sqlite:file::memory:?cache=shared}, configured in
 * {@code src/test/resources/application.properties}) that every other {@code @SpringBootTest} in
 * this project uses.
 *
 * <p>The shared anonymous in-memory database is unsuitable here for a second reason beyond just
 * "don't touch dev data": {@link DatabaseSeederService#seed()} commits its own transaction (it is
 * {@code @Transactional} on the service method, not on this test method), so its 10,000 seeded
 * employees would NOT be rolled back afterward. If this test shared the same in-memory database
 * as every other {@code @SpringBootTest}-based test class, those 10,000+ leftover rows could
 * silently pollute any test that runs later in the same JVM and asserts on absolute row counts.
 *
 * <p>The {@link DynamicPropertySource} below overrides only {@code spring.datasource.url} for
 * this test class, pointing it at a distinct, uniquely-named in-memory SQLite database (not the
 * anonymous {@code file::memory:} every other test class shares). A real temporary file (e.g.
 * via JUnit's {@code @TempDir}) was considered and rejected: Spring caches the application
 * context (and its connection pool) across test classes and only tears it down at JVM shutdown,
 * by which point a JUnit-managed temp directory has already been deleted - Hibernate's
 * {@code ddl-auto=create-drop} then fails (harmlessly, but noisily) trying to drop tables in a
 * file that no longer exists. A named in-memory database has no such file to go missing out from
 * under a still-open connection. Everything else (dialect, {@code ddl-auto=create-drop}, driver)
 * is still inherited from {@code src/test/resources/application.properties}. Because the
 * effective configuration differs from every other test class, Spring also gives this class its
 * own, separate application context and connection pool - not just a different URL string - so
 * there is no possibility of sharing a connection/cache with another test's context either.
 */
@SpringBootTest
class DatabaseSeederServiceTest {

    @DynamicPropertySource
    static void useIsolatedSeederTestDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:sqlite:file:seeder-test-db?mode=memory&cache=shared&foreign_keys=on");
    }

    @Autowired
    private DatabaseSeederService seederService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private SalaryRecordRepository salaryRecordRepository;

    @Test
    void seedCreatesExactlyTenThousandEmployeesAndValidSalaryRecords() {
        DatabaseSeederService.SeedResult result = seederService.seed();

        // 1. Employee count is exactly 10,000.
        assertEquals(10000, employeeRepository.count());

        // 2. Every one of the 10,000 employees has at least one salary record - verified via a
        // single COUNT(DISTINCT ...) query rather than loading all salary records into memory.
        assertEquals(10000, salaryRecordRepository.countDistinctEmployeesWithSalaryRecords(),
                "Every seeded employee must have at least one salary record");

        // 3. Salary record count is the exact deterministic total produced by the generator:
        // recordCount cycles 1, 2, 3 across employees 1..10000 (1 + ((i - 1) % 3)), i.e. 6
        // records per 3 consecutive employees. 10,000 = 3,333 full cycles (9,999 employees, 6
        // records each = 19,998) plus employee #10,000 alone (recordCount 1) = 19,999 total.
        long totalSalaryRecords = salaryRecordRepository.count();
        assertEquals(19999, totalSalaryRecords);
        assertEquals(10000, result.createdEmployees());
        assertEquals(19999, result.createdSalaryRecords());

        // 4. EMP-1001 (i=1) has salary history, and matches its deterministic identity fields
        // exactly, derived from the generator's index-based formulas:
        //   firstName = FIRST_NAMES[(i-1) % 50]        -> FIRST_NAMES[0]  = "Ada"
        //   lastName  = LAST_NAMES[((i-1) * 7) % 50]   -> LAST_NAMES[0]   = "Adams"
        //   email     = "ada.adams1@company.com"
        //   country   = COUNTRIES[(i-1) % 7]           -> COUNTRIES[0]    = "Canada"
        //   department = DEPARTMENTS[(i-1) % 9]        -> DEPARTMENTS[0]  = "Engineering"
        Employee empFirst = employeeRepository.findByEmployeeNumber("EMP-1001").orElseThrow();
        assertEquals("EMP-1001", empFirst.getEmployeeNumber());
        assertEquals("Ada", empFirst.getFirstName());
        assertEquals("Adams", empFirst.getLastName());
        assertEquals("ada.adams1@company.com", empFirst.getEmail());
        assertEquals("Canada", empFirst.getCountry());
        assertEquals("Engineering", empFirst.getDepartment());
        List<SalaryRecord> firstHistory = salaryRecordRepository.findByEmployeeOrderByEffectiveFromDescIdDesc(empFirst);
        assertFalse(firstHistory.isEmpty(), "EMP-1001 must have at least one salary record");

        // 5. EMP-11000 (i=10000) has salary history.
        Employee empLast = employeeRepository.findByEmployeeNumber("EMP-11000").orElseThrow();
        assertEquals("EMP-11000", empLast.getEmployeeNumber());
        List<SalaryRecord> lastHistory = salaryRecordRepository.findByEmployeeOrderByEffectiveFromDescIdDesc(empLast);
        assertFalse(lastHistory.isEmpty(), "EMP-11000 must have at least one salary record");

        // 6. EMP-1002 (i=2): recordCount = 1 + ((2-1) % 3) = 2 - exactly two salary records.
        Employee empTwo = employeeRepository.findByEmployeeNumber("EMP-1002").orElseThrow();
        List<SalaryRecord> empTwoHistory = salaryRecordRepository.findByEmployeeOrderByEffectiveFromDescIdDesc(empTwo);
        assertEquals(2, empTwoHistory.size(), "EMP-1002 must have exactly two salary records");

        // 7. EMP-1003 (i=3): recordCount = 1 + ((3-1) % 3) = 3, including a deterministic
        // future-dated third record. Its effective date is derived directly from the generator's
        // formula for an odd i (i % 2 != 0): LocalDate.of(2027 + (i % 2), 1 + (i % 12), 1 + (i % 28))
        // = LocalDate.of(2028, 4, 4) - a fixed value, not LocalDate.now(), so this assertion
        // stays correct regardless of when the test runs.
        Employee empThree = employeeRepository.findByEmployeeNumber("EMP-1003").orElseThrow();
        List<SalaryRecord> empThreeHistory = salaryRecordRepository.findByEmployeeOrderByEffectiveFromDescIdDesc(empThree);
        assertEquals(3, empThreeHistory.size(), "EMP-1003 must have exactly three salary records");

        // findByEmployeeOrderByEffectiveFromDescIdDesc orders newest-first, so the future-dated
        // record (2028-04-04) is index 0. The amount itself is deliberately not asserted here:
        // it round-trips through a NUMERIC(19,2) SQLite column, and this test intentionally
        // avoids depending on exactly what scale the driver/Hibernate normalizes it to on
        // read-back, which the generator's own formula does not control or guarantee.
        SalaryRecord futureRecord = empThreeHistory.get(0);
        assertEquals(LocalDate.of(2028, 4, 4), futureRecord.getEffectiveFrom(),
                "EMP-1003's third salary record must be deterministically dated 2028-04-04");
    }

    @Test
    void seedIsIdempotentAndDoesNotDuplicateDataOnRerun() {
        seederService.seed();
        long empCountFirst = employeeRepository.count();
        long salaryCountFirst = salaryRecordRepository.count();

        DatabaseSeederService.SeedResult resultRerun = seederService.seed();

        assertEquals(10000, employeeRepository.count());
        assertEquals(salaryCountFirst, salaryRecordRepository.count());
        assertEquals(empCountFirst, resultRerun.deletedEmployees());
        assertEquals(salaryCountFirst, resultRerun.deletedSalaryRecords());
        assertEquals(10000, resultRerun.createdEmployees());
        assertEquals(salaryCountFirst, resultRerun.createdSalaryRecords());
    }
}

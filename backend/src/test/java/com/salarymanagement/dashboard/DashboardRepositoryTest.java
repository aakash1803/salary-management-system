package com.salarymanagement.dashboard;

import com.salarymanagement.employee.Employee;
import com.salarymanagement.employee.EmployeeRepository;
import com.salarymanagement.salary.SalaryRecord;
import com.salarymanagement.salary.SalaryRecordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Persistence tests for {@link DashboardRepository}'s aggregate queries, run against the
 * project's real SQLite configuration (see src/test/resources/application.properties) - these
 * specifically verify the window-function/CTE-based "current salary per employee" logic actually
 * behaves as intended on SQLite, not just that it looks like valid SQL. Each test runs in its own
 * transaction which is rolled back afterwards, so tests are independent and repeatable.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class DashboardRepositoryTest {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private SalaryRecordRepository salaryRecordRepository;

    @Autowired
    private DashboardRepository dashboardRepository;

    private static final BigDecimal BAND_1 = new BigDecimal("30000");
    private static final BigDecimal BAND_2 = new BigDecimal("60000");
    private static final BigDecimal BAND_3 = new BigDecimal("100000");
    private static final BigDecimal BAND_4 = new BigDecimal("150000");

    private Employee saveEmployee(String employeeNumber, String country) {
        return employeeRepository.saveAndFlush(new Employee(
                employeeNumber, "First", "Last", employeeNumber.toLowerCase() + "@example.com", country,
                "Engineering"));
    }

    private SalaryRecord saveSalaryRecord(Employee employee, String amount, String currency, LocalDate effectiveFrom) {
        return salaryRecordRepository.saveAndFlush(new SalaryRecord(employee, new BigDecimal(amount), currency, effectiveFrom));
    }

    /**
     * Seeds a small, deliberately mixed dataset used by most tests below:
     * <ul>
     *   <li>EMP-301 (UK): salary changed from 50,000 GBP a year ago to 55,000 GBP today - current
     *       salary is 55,000 GBP.</li>
     *   <li>EMP-302 (UK): 45,000 GBP today, plus a 90,000 GBP record effective 6 months from now
     *       - the future record must not affect any aggregate.</li>
     *   <li>EMP-303 (US): 70,000 USD today.</li>
     *   <li>EMP-304 (US): no salary record at all - must still count as an employee/towards its
     *       country, but must not appear in any salary-based aggregate.</li>
     *   <li>EMP-305 (UK): two records effective today, 60,000 GBP then 62,000 GBP - the
     *       later-inserted (higher id) 62,000 record must win the tie-break.</li>
     * </ul>
     * Expected current salaries: GBP {55000, 45000, 62000} -> min 45000, max 62000, avg 54000,
     * total 162000; USD {70000} -> min/max/avg/total all 70000.
     */
    private void seedTypicalDashboardData() {
        Employee a = saveEmployee("EMP-301", "UK");
        saveSalaryRecord(a, "50000.00", "GBP", LocalDate.now().minusYears(1));
        saveSalaryRecord(a, "55000.00", "GBP", LocalDate.now());

        Employee b = saveEmployee("EMP-302", "UK");
        saveSalaryRecord(b, "45000.00", "GBP", LocalDate.now());
        saveSalaryRecord(b, "90000.00", "GBP", LocalDate.now().plusMonths(6));

        Employee c = saveEmployee("EMP-303", "US");
        saveSalaryRecord(c, "70000.00", "USD", LocalDate.now());

        saveEmployee("EMP-304", "US");

        Employee e = saveEmployee("EMP-305", "UK");
        saveSalaryRecord(e, "60000.00", "GBP", LocalDate.now());
        saveSalaryRecord(e, "62000.00", "GBP", LocalDate.now());
    }

    private DashboardRepository.SalaryMetricsRow metricsFor(
            List<DashboardRepository.SalaryMetricsRow> rows, String currency) {
        return rows.stream().filter(row -> row.getCurrency().equals(currency)).findFirst().orElseThrow();
    }

    @Test
    void countsEmployeesByCountry() {
        seedTypicalDashboardData();

        List<DashboardRepository.EmployeeCountByCountryRow> rows = dashboardRepository.countEmployeesByCountry();

        assertEquals(2, rows.size());
        assertEquals("UK", rows.get(0).getCountry());
        assertEquals(3, rows.get(0).getEmployeeCount());
        assertEquals("US", rows.get(1).getCountry());
        assertEquals(2, rows.get(1).getEmployeeCount());
    }

    @Test
    void computesSalaryMetricsByCurrencyExcludingFutureDatedRecords() {
        seedTypicalDashboardData();

        List<DashboardRepository.SalaryMetricsRow> rows =
                dashboardRepository.findSalaryMetricsByCurrency(LocalDate.now());

        DashboardRepository.SalaryMetricsRow gbp = metricsFor(rows, "GBP");
        assertEquals(45000.0, gbp.getMinimum());
        assertEquals(62000.0, gbp.getMaximum());
        assertEquals(54000.0, gbp.getAverage());

        DashboardRepository.SalaryMetricsRow usd = metricsFor(rows, "USD");
        assertEquals(70000.0, usd.getMinimum());
        assertEquals(70000.0, usd.getMaximum());
        assertEquals(70000.0, usd.getAverage());
    }

    @Test
    void computesPayrollByCurrencyExcludingFutureDatedRecords() {
        seedTypicalDashboardData();

        List<DashboardRepository.PayrollByCurrencyRow> rows =
                dashboardRepository.findPayrollByCurrency(LocalDate.now());

        double gbpTotal = rows.stream()
                .filter(row -> row.getCurrency().equals("GBP")).findFirst().orElseThrow().getTotalPayroll();
        assertEquals(162000.0, gbpTotal);

        double usdTotal = rows.stream()
                .filter(row -> row.getCurrency().equals("USD")).findFirst().orElseThrow().getTotalPayroll();
        assertEquals(70000.0, usdTotal);
    }

    @Test
    void computesPayrollByCountryGroupedByCountryAndCurrency() {
        seedTypicalDashboardData();

        List<DashboardRepository.PayrollByCountryRow> rows =
                dashboardRepository.findPayrollByCountry(LocalDate.now());

        // Only two rows: UK/GBP and US/USD. EMP-304 (US, no salary) contributes no row at all.
        assertEquals(2, rows.size());

        DashboardRepository.PayrollByCountryRow uk = rows.stream()
                .filter(row -> row.getCountry().equals("UK")).findFirst().orElseThrow();
        assertEquals("GBP", uk.getCurrency());
        assertEquals(162000.0, uk.getTotalPayroll());

        DashboardRepository.PayrollByCountryRow us = rows.stream()
                .filter(row -> row.getCountry().equals("US")).findFirst().orElseThrow();
        assertEquals("USD", us.getCurrency());
        assertEquals(70000.0, us.getTotalPayroll());
    }

    @Test
    void computesSalaryDistributionBandsPerCurrency() {
        seedTypicalDashboardData();

        List<DashboardRepository.SalaryDistributionRow> rows = dashboardRepository.findSalaryDistribution(
                LocalDate.now(), BAND_1, BAND_2, BAND_3, BAND_4);

        // GBP current salaries: 45000 and 55000 fall in band 1 (30000-59999.99); 62000 in band 2.
        long gbpBand1 = rows.stream()
                .filter(row -> row.getCurrency().equals("GBP") && row.getBandIndex() == 1)
                .findFirst().orElseThrow().getEmployeeCount();
        assertEquals(2, gbpBand1);

        long gbpBand2 = rows.stream()
                .filter(row -> row.getCurrency().equals("GBP") && row.getBandIndex() == 2)
                .findFirst().orElseThrow().getEmployeeCount();
        assertEquals(1, gbpBand2);

        // USD: 70000 falls in band 2 as well, in its own currency group.
        long usdBand2 = rows.stream()
                .filter(row -> row.getCurrency().equals("USD") && row.getBandIndex() == 2)
                .findFirst().orElseThrow().getEmployeeCount();
        assertEquals(1, usdBand2);
    }

    @Test
    void employeeWithNoCurrentSalaryCountsTowardEmployeeTotalsButNotSalaryAggregates() {
        seedTypicalDashboardData();

        // EMP-304 (US) has no salary record at all, yet must still be counted here...
        assertEquals(5, employeeRepository.count());
        long usEmployeeCount = dashboardRepository.countEmployeesByCountry().stream()
                .filter(row -> row.getCountry().equals("US")).findFirst().orElseThrow().getEmployeeCount();
        assertEquals(2, usEmployeeCount);

        // ...but must contribute nothing to payroll: only one US row (EMP-303's USD payroll) exists.
        long usPayrollRows = dashboardRepository.findPayrollByCountry(LocalDate.now()).stream()
                .filter(row -> row.getCountry().equals("US")).count();
        assertEquals(1, usPayrollRows);
    }

    @Test
    void breaksATieOnIdenticalEffectiveFromByIdDescending() {
        Employee employee = saveEmployee("EMP-306", "UK");
        LocalDate sameDate = LocalDate.now();
        saveSalaryRecord(employee, "60000.00", "GBP", sameDate);
        saveSalaryRecord(employee, "62000.00", "GBP", sameDate);

        List<DashboardRepository.SalaryMetricsRow> rows =
                dashboardRepository.findSalaryMetricsByCurrency(LocalDate.now());
        DashboardRepository.SalaryMetricsRow gbp = metricsFor(rows, "GBP");

        // Only this one employee's current salary contributes here, so a correct tie-break means
        // min == max == the higher-id (later-inserted) record's amount, not the lower-id one.
        assertEquals(62000.0, gbp.getMinimum());
        assertEquals(62000.0, gbp.getMaximum());
    }
}

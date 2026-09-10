package com.salarymanagement.dashboard;

import com.salarymanagement.dashboard.DashboardRepository.EmployeeCountByCountryRow;
import com.salarymanagement.dashboard.DashboardRepository.PayrollByCountryRow;
import com.salarymanagement.dashboard.DashboardRepository.PayrollByCurrencyRow;
import com.salarymanagement.dashboard.DashboardRepository.SalaryDistributionRow;
import com.salarymanagement.dashboard.DashboardRepository.SalaryMetricsRow;
import com.salarymanagement.dashboard.DashboardResponse.EmployeeCountByCountry;
import com.salarymanagement.dashboard.DashboardResponse.PayrollByCountry;
import com.salarymanagement.dashboard.DashboardResponse.PayrollByCurrency;
import com.salarymanagement.dashboard.DashboardResponse.SalaryBand;
import com.salarymanagement.dashboard.DashboardResponse.SalaryDistributionByCurrency;
import com.salarymanagement.dashboard.DashboardResponse.SalaryMetrics;
import com.salarymanagement.employee.EmployeeRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Application service for the dashboard/insights API: orchestrates {@link EmployeeRepository}
 * (reused as-is, not modified, for the one figure - total employee count - that needs no
 * dashboard-specific query) and {@link DashboardRepository} (every salary-based aggregate), and
 * maps their results to {@link DashboardResponse}. Every actual aggregation (counting, summing,
 * averaging, bucketing) happens in the database queries themselves; the mapping here only
 * reshapes already-aggregated rows into the response shape and does the final numeric
 * presentation step described below - it never loads individual employee or salary rows to
 * compute a figure in Java.
 *
 * <p>Salary band boundaries are defined once here and passed into
 * {@link DashboardRepository#findSalaryDistribution}, which does the actual bucketing in SQL;
 * {@link #SALARY_BAND_LABELS} are this class's human-readable presentation of those same five
 * bands, matched by index (0-4) to the {@code bandIndex} the query returns for each row - keep
 * these two lists in sync if the boundaries ever change. Bands are intentionally not converted to
 * or normalized across currencies: since this system never converts currencies, the same numeric
 * boundaries are applied within each currency's own group of current salaries. If currencies with
 * very different typical magnitudes are introduced (e.g. JPY alongside GBP), these fixed
 * boundaries would not be equally meaningful for both - that is a real limitation of a
 * no-currency-conversion design, not something this feature can fix without inventing exchange
 * rates, which is explicitly out of scope.
 *
 * <p>{@link DashboardRepository}'s SUM/AVG-based projections return {@code Double} rather than
 * {@link BigDecimal} (see that interface's Javadoc for why); {@link #toScaledAmount} is the one
 * place that converts each such value into the same 2-decimal-place {@link BigDecimal} shape used
 * everywhere else in this API's monetary fields.
 */
@Service
public class DashboardService {

    private static final BigDecimal BAND_BOUNDARY_1 = new BigDecimal("30000");
    private static final BigDecimal BAND_BOUNDARY_2 = new BigDecimal("60000");
    private static final BigDecimal BAND_BOUNDARY_3 = new BigDecimal("100000");
    private static final BigDecimal BAND_BOUNDARY_4 = new BigDecimal("150000");

    /** Index-aligned with the {@code bandIndex} (0-4) produced by {@code findSalaryDistribution}. */
    private static final List<String> SALARY_BAND_LABELS = List.of(
            "Under 30,000",
            "30,000 - 59,999.99",
            "60,000 - 99,999.99",
            "100,000 - 149,999.99",
            "150,000 and above"
    );

    private final EmployeeRepository employeeRepository;
    private final DashboardRepository dashboardRepository;

    public DashboardService(EmployeeRepository employeeRepository, DashboardRepository dashboardRepository) {
        this.employeeRepository = employeeRepository;
        this.dashboardRepository = dashboardRepository;
    }

    public DashboardResponse getDashboard() {
        LocalDate today = LocalDate.now();

        long totalEmployees = employeeRepository.count();

        List<EmployeeCountByCountry> employeesByCountry = mapEmployeeCounts(
                dashboardRepository.countEmployeesByCountry());

        List<SalaryMetrics> salaryMetricsByCurrency = mapSalaryMetrics(
                dashboardRepository.findSalaryMetricsByCurrency(today));

        List<SalaryDistributionByCurrency> salaryDistribution = mapSalaryDistribution(
                dashboardRepository.findSalaryDistribution(
                        today, BAND_BOUNDARY_1, BAND_BOUNDARY_2, BAND_BOUNDARY_3, BAND_BOUNDARY_4));

        List<PayrollByCountry> payrollByCountry = mapPayrollByCountry(
                dashboardRepository.findPayrollByCountry(today));

        List<PayrollByCurrency> payrollByCurrency = mapPayrollByCurrency(
                dashboardRepository.findPayrollByCurrency(today));

        return new DashboardResponse(totalEmployees, salaryMetricsByCurrency, employeesByCountry,
                salaryDistribution, payrollByCountry, payrollByCurrency);
    }

    private List<EmployeeCountByCountry> mapEmployeeCounts(List<EmployeeCountByCountryRow> rows) {
        return rows.stream()
                .map(row -> new EmployeeCountByCountry(row.getCountry(), row.getEmployeeCount()))
                .toList();
    }

    private List<SalaryMetrics> mapSalaryMetrics(List<SalaryMetricsRow> rows) {
        return rows.stream()
                .map(row -> new SalaryMetrics(
                        row.getCurrency(),
                        toScaledAmount(row.getMinimum()),
                        toScaledAmount(row.getMaximum()),
                        toScaledAmount(row.getAverage())))
                .toList();
    }

    private List<SalaryDistributionByCurrency> mapSalaryDistribution(List<SalaryDistributionRow> rows) {
        Map<String, List<SalaryBand>> bandsByCurrency = new LinkedHashMap<>();
        for (SalaryDistributionRow row : rows) {
            SalaryBand band = new SalaryBand(SALARY_BAND_LABELS.get(row.getBandIndex()), row.getEmployeeCount());
            bandsByCurrency.computeIfAbsent(row.getCurrency(), currency -> new ArrayList<>()).add(band);
        }
        return bandsByCurrency.entrySet().stream()
                .map(entry -> new SalaryDistributionByCurrency(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<PayrollByCountry> mapPayrollByCountry(List<PayrollByCountryRow> rows) {
        return rows.stream()
                .map(row -> new PayrollByCountry(row.getCountry(), row.getCurrency(), toScaledAmount(row.getTotalPayroll())))
                .toList();
    }

    private List<PayrollByCurrency> mapPayrollByCurrency(List<PayrollByCurrencyRow> rows) {
        return rows.stream()
                .map(row -> new PayrollByCurrency(row.getCurrency(), toScaledAmount(row.getTotalPayroll())))
                .toList();
    }

    private static BigDecimal toScaledAmount(Double value) {
        if (value == null) {
            return null;
        }
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}

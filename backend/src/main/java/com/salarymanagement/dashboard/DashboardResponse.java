package com.salarymanagement.dashboard;

import java.math.BigDecimal;
import java.util.List;

/**
 * API-facing shape of the dashboard/insights endpoint ({@code GET /api/dashboard}).
 *
 * <p>There is deliberately no single global "minimum/maximum/average salary" or "total payroll"
 * figure: this system never converts between currencies (see {@code DashboardService}), so a
 * cross-currency sum or average would be a mathematically meaningless number. Every monetary
 * aggregate here is therefore grouped by currency instead (and, for payroll by country,
 * additionally keyed by currency within each country) rather than following the flat example
 * shape suggested in the requirements for those fields.
 *
 * <p>The nested response types are declared here, rather than as their own top-level files: each
 * one is meaningful only as part of this one response, unlike {@code EmployeeResponse}/{@code
 * SalaryResponse}, which are reused independently across multiple endpoints.
 */
public record DashboardResponse(
        long totalEmployees,
        List<SalaryMetrics> salaryMetricsByCurrency,
        List<EmployeeCountByCountry> employeesByCountry,
        List<SalaryDistributionByCurrency> salaryDistribution,
        List<PayrollByCountry> payrollByCountry,
        List<PayrollByCurrency> payrollByCurrency
) {

    /** Minimum/maximum/average current salary for one currency. */
    public record SalaryMetrics(String currency, BigDecimal minimum, BigDecimal maximum, BigDecimal average) {
    }

    /** Employee headcount for one country - counts every employee, regardless of salary. */
    public record EmployeeCountByCountry(String country, long employeeCount) {
    }

    /**
     * Current-salary band counts for one currency. Distribution is grouped by currency for the
     * same reason {@link SalaryMetrics} is: a salary band drawn across mixed currencies (e.g. is
     * 50,000 "mid-range" if GBP, or "low-range" if JPY?) would be just as mathematically
     * misleading as a cross-currency min/max/average.
     */
    public record SalaryDistributionByCurrency(String currency, List<SalaryBand> bands) {
    }

    /** One predefined salary band (e.g. "30,000 - 59,999.99") and how many employees fall in it. */
    public record SalaryBand(String range, long employeeCount) {
    }

    /** Total current-salary payroll for one country, in one currency. */
    public record PayrollByCountry(String country, String currency, BigDecimal totalPayroll) {
    }

    /** Total current-salary payroll for one currency, across all countries. */
    public record PayrollByCurrency(String currency, BigDecimal totalPayroll) {
    }
}

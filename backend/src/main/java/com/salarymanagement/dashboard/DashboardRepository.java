package com.salarymanagement.dashboard;

import com.salarymanagement.salary.SalaryRecord;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Read-only aggregate queries backing the dashboard/insights API.
 *
 * <p>Deliberately extends the no-op {@link Repository} marker rather than
 * {@link org.springframework.data.jpa.repository.JpaRepository}: this interface exists purely for
 * cross-entity aggregate projections, not CRUD access to any single entity - {@code
 * EmployeeRepository} and {@code SalaryRecordRepository} already own that, and are reused
 * directly by {@code DashboardService} wherever a dashboard figure doesn't need a
 * dashboard-specific aggregate query (e.g. total employee count).
 *
 * <p>All salary-based aggregates below share the same "current salary per employee" definition
 * already established by {@code SalaryRecordRepository}: the {@link SalaryRecord} with the latest
 * {@code effective_from} that is not after the given date, ties broken by {@code id} descending -
 * here applied per employee at once, across potentially ~10,000 employees, rather than for a
 * single one. Computing this in Java (loading every salary record and reducing per employee)
 * would defeat the point of a database-backed aggregate, so it is computed entirely in SQL via a
 * window function: {@code ROW_NUMBER() OVER (PARTITION BY employee_id ORDER BY effective_from
 * DESC, id DESC)}, numbering each employee's salary records from most to least recent, with the
 * "current" one always numbered 1. SQLite has supported window functions since 3.25 (2018), so
 * this is safe on the bundled driver. All four salary-based queries below share this logic via
 * the {@link #CURRENT_SALARY_CTE} constant, so the "current salary" definition lives in exactly
 * one place.
 *
 * <p>These are native queries (not JPQL) specifically so this CTE/window-function logic runs on
 * SQLite exactly as written, without going through Hibernate's HQL-to-SQL translation for a
 * feature area this project has no existing usage of or confidence in - this project has already
 * hit two real, non-obvious gaps in Hibernate's translation for this exact SQLite dialect (see
 * {@code EmployeeSpecifications} and {@code EmployeeService.isUniqueConstraintViolation}), so
 * native SQL was chosen here to avoid a third. Every monetary aggregate (SUM/AVG) is additionally
 * rounded to 2 decimal places in SQL and exposed through the projections below as {@code Double}
 * (not {@code BigDecimal}) - SQLite computes AVG (and, once any input has a fractional part, SUM)
 * in floating point regardless of the stored column's declared precision/scale, so {@code
 * DashboardService} does the final, explicit conversion to a scaled {@link BigDecimal} rather than
 * relying on an implicit driver/ORM conversion from a floating-point SQL result.
 */
public interface DashboardRepository extends Repository<SalaryRecord, Long> {

    String CURRENT_SALARY_CTE = """
            WITH ranked AS (
                SELECT sr.employee_id AS employee_id,
                       sr.amount AS amount,
                       sr.currency AS currency,
                       ROW_NUMBER() OVER (
                           PARTITION BY sr.employee_id
                           ORDER BY sr.effective_from DESC, sr.id DESC
                       ) AS rn
                FROM salary_record sr
                WHERE sr.effective_from <= :asOfDate
            ),
            current_salary AS (
                SELECT employee_id, amount, currency FROM ranked WHERE rn = 1
            )
            """;

    /**
     * Employee counts by country, independent of salary entirely - an employee with no currently
     * effective salary record (or no salary record at all) is still counted here, unlike every
     * other query in this interface.
     */
    @Query(value = """
            SELECT e.country AS country, COUNT(*) AS employeeCount
            FROM employee e
            GROUP BY e.country
            ORDER BY e.country
            """, nativeQuery = true)
    List<EmployeeCountByCountryRow> countEmployeesByCountry();

    /**
     * Minimum/maximum/average current salary, grouped by currency so amounts in different
     * currencies are never combined into one mathematically meaningless figure.
     */
    @Query(value = CURRENT_SALARY_CTE + """
            SELECT cs.currency AS currency,
                   ROUND(MIN(cs.amount), 2) AS minimum,
                   ROUND(MAX(cs.amount), 2) AS maximum,
                   ROUND(AVG(cs.amount), 2) AS average
            FROM current_salary cs
            GROUP BY cs.currency
            ORDER BY cs.currency
            """, nativeQuery = true)
    List<SalaryMetricsRow> findSalaryMetricsByCurrency(@Param("asOfDate") LocalDate asOfDate);

    /**
     * Current-salary counts bucketed into predefined bands (see {@code DashboardService} for the
     * boundary values and their human-readable labels), grouped by currency for the same reason as
     * {@link #findSalaryMetricsByCurrency}. {@code bandIndex} (0-4) identifies which of the five
     * bands a row belongs to; mapping that index to its label string is presentation only and
     * happens in the service layer - the bucketing itself (which is the actual aggregation) stays
     * in this query.
     */
    @Query(value = CURRENT_SALARY_CTE + """
            SELECT cs.currency AS currency,
                   CASE
                       WHEN cs.amount < :band1 THEN 0
                       WHEN cs.amount < :band2 THEN 1
                       WHEN cs.amount < :band3 THEN 2
                       WHEN cs.amount < :band4 THEN 3
                       ELSE 4
                   END AS bandIndex,
                   COUNT(*) AS employeeCount
            FROM current_salary cs
            GROUP BY cs.currency,
                   CASE
                       WHEN cs.amount < :band1 THEN 0
                       WHEN cs.amount < :band2 THEN 1
                       WHEN cs.amount < :band3 THEN 2
                       WHEN cs.amount < :band4 THEN 3
                       ELSE 4
                   END
            ORDER BY cs.currency, bandIndex
            """, nativeQuery = true)
    List<SalaryDistributionRow> findSalaryDistribution(
            @Param("asOfDate") LocalDate asOfDate,
            @Param("band1") BigDecimal band1,
            @Param("band2") BigDecimal band2,
            @Param("band3") BigDecimal band3,
            @Param("band4") BigDecimal band4);

    /**
     * Total current-salary payroll per country, keyed additionally by currency so that different
     * currencies are never summed together into one meaningless total.
     */
    @Query(value = CURRENT_SALARY_CTE + """
            SELECT e.country AS country,
                   cs.currency AS currency,
                   ROUND(SUM(cs.amount), 2) AS totalPayroll
            FROM current_salary cs
            JOIN employee e ON e.id = cs.employee_id
            GROUP BY e.country, cs.currency
            ORDER BY e.country, cs.currency
            """, nativeQuery = true)
    List<PayrollByCountryRow> findPayrollByCountry(@Param("asOfDate") LocalDate asOfDate);

    /**
     * Total current-salary payroll per currency.
     */
    @Query(value = CURRENT_SALARY_CTE + """
            SELECT cs.currency AS currency,
                   ROUND(SUM(cs.amount), 2) AS totalPayroll
            FROM current_salary cs
            GROUP BY cs.currency
            ORDER BY cs.currency
            """, nativeQuery = true)
    List<PayrollByCurrencyRow> findPayrollByCurrency(@Param("asOfDate") LocalDate asOfDate);

    interface EmployeeCountByCountryRow {
        String getCountry();

        long getEmployeeCount();
    }

    interface SalaryMetricsRow {
        String getCurrency();

        Double getMinimum();

        Double getMaximum();

        Double getAverage();
    }

    interface SalaryDistributionRow {
        String getCurrency();

        int getBandIndex();

        long getEmployeeCount();
    }

    interface PayrollByCountryRow {
        String getCountry();

        String getCurrency();

        Double getTotalPayroll();
    }

    interface PayrollByCurrencyRow {
        String getCurrency();

        Double getTotalPayroll();
    }
}

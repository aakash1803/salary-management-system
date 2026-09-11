package com.salarymanagement.salary;

import com.salarymanagement.employee.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Data access for {@link SalaryRecord}.
 *
 * <p>There is no update/delete method here beyond what {@link JpaRepository} already provides
 * for exceptional/administrative use: the domain rule is that a salary change is recorded by
 * saving a new {@code SalaryRecord}, never by mutating an existing one.
 */
public interface SalaryRecordRepository extends JpaRepository<SalaryRecord, Long> {

    /**
     * Full salary history for an employee, most recent effective date first. Ties on
     * {@code effectiveFrom} are broken by {@code id} descending, so ordering is deterministic
     * even when multiple records share the same effective date.
     */
    List<SalaryRecord> findByEmployeeOrderByEffectiveFromDescIdDesc(Employee employee);

    /**
     * The salary record that applies as of {@code asOfDate}: the one with the latest
     * {@code effectiveFrom} that is not after {@code asOfDate}. Passing {@code LocalDate.now()}
     * gives the "current salary" while naturally excluding any future-dated record. Ties on
     * {@code effectiveFrom} (unusual, but not excluded by the domain rules) are broken by the
     * most recently created record, so the result is deterministic.
     */
    Optional<SalaryRecord> findFirstByEmployeeAndEffectiveFromLessThanEqualOrderByEffectiveFromDescIdDesc(
            Employee employee, LocalDate asOfDate);

    /**
     * Bulk lookup of every salary record applicable as of {@code asOfDate} for a set of
     * employees, ordered by employee id, then {@code effectiveFrom} descending, then id
     * descending. This lets a caller determine each employee's current salary in a single
     * query instead of one query per employee (see {@code SalaryService#getCurrentSalariesByEmployeeIds}),
     * which would otherwise be an N+1 query pattern when building a list view for many
     * employees at once.
     */
    List<SalaryRecord> findByEmployee_IdInAndEffectiveFromLessThanEqualOrderByEmployee_IdAscEffectiveFromDescIdDesc(
            List<Long> employeeIds, LocalDate asOfDate);

    /**
     * Counts how many distinct employees currently have at least one salary record, computed
     * entirely in the database via {@code COUNT(DISTINCT ...)} rather than by loading any salary
     * record into memory. Used to verify, at scale (e.g. 10,000 seeded employees), that every
     * employee has salary history without an O(n) per-employee query or materializing the full
     * salary table.
     */
    @Query("SELECT COUNT(DISTINCT sr.employee.id) FROM SalaryRecord sr")
    long countDistinctEmployeesWithSalaryRecords();
}

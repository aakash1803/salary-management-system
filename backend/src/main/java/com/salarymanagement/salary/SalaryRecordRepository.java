package com.salarymanagement.salary;

import com.salarymanagement.employee.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

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
}

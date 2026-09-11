package com.salarymanagement.salary;

import com.salarymanagement.common.NotFoundException;
import com.salarymanagement.employee.Employee;
import com.salarymanagement.employee.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Application service for salary records: adding a new salary entry, deriving the current
 * salary, and returning full salary history. Returns domain entities; mapping to API response
 * shapes is the controller's responsibility (see {@link SalaryResponse}).
 *
 * <p>This service loads the {@link Employee} directly via {@link EmployeeRepository} rather than
 * going through {@code EmployeeService}: every method here needs the {@code Employee} entity
 * itself (the {@link SalaryRecordRepository} query methods take an {@code Employee}, not an id),
 * and going through the repository keeps this module's dependency on the employee module limited
 * to data access, consistent with the {@code Controller -> Service -> Repository} layering used
 * throughout the rest of this codebase - no service here depends on another module's service.
 */
@Service
public class SalaryService {

    private final SalaryRecordRepository salaryRecordRepository;
    private final EmployeeRepository employeeRepository;

    public SalaryService(SalaryRecordRepository salaryRecordRepository, EmployeeRepository employeeRepository) {
        this.salaryRecordRepository = salaryRecordRepository;
        this.employeeRepository = employeeRepository;
    }

    /**
     * Adds a new salary record for an employee. Salary history is append-only: this never
     * updates or deletes an existing {@link SalaryRecord} (see that class's Javadoc), so no
     * conflict/uniqueness handling is needed here - multiple records, including ones sharing the
     * same {@code effectiveFrom}, are all valid history entries.
     */
    @Transactional
    public SalaryRecord addSalary(Long employeeId, SalaryRequest request) {
        Employee employee = getEmployee(employeeId);

        SalaryRecord salaryRecord = new SalaryRecord(
                employee, request.amount(), request.currency(), request.effectiveFrom());

        return salaryRecordRepository.saveAndFlush(salaryRecord);
    }

    /**
     * The salary record currently in effect: the one with the latest {@code effectiveFrom} that
     * is not after today, ties broken by {@code id} descending (see
     * {@link SalaryRecordRepository#findFirstByEmployeeAndEffectiveFromLessThanEqualOrderByEffectiveFromDescIdDesc}).
     * A future-dated record is never returned here until its effective date arrives.
     */
    public SalaryRecord getCurrentSalary(Long employeeId) {
        Employee employee = getEmployee(employeeId);

        return salaryRecordRepository
                .findFirstByEmployeeAndEffectiveFromLessThanEqualOrderByEffectiveFromDescIdDesc(
                        employee, LocalDate.now())
                .orElseThrow(() -> new NotFoundException(
                        "No salary currently effective for employee with id: " + employeeId));
    }

    /**
     * Full salary history for an employee, newest {@code effectiveFrom} first, {@code id}
     * descending as a tie-breaker. An employee with no salary records yet simply gets an empty
     * list - that is not an error, unlike a missing employee.
     */
    public List<SalaryRecord> getSalaryHistory(Long employeeId) {
        Employee employee = getEmployee(employeeId);

        return salaryRecordRepository.findByEmployeeOrderByEffectiveFromDescIdDesc(employee);
    }

    /**
     * The current salary record (if any) for each of the given employee ids, computed in a
     * single query rather than one lookup per employee. Used by other modules assembling a
     * composite view - e.g. the employee list endpoint, which shows current salary/currency for
     * a whole page of employees at once - without introducing an N+1 query pattern.
     *
     * <p>An employee id with no salary record currently in effect (including one with no salary
     * records at all) is simply absent from the returned map rather than mapped to {@code null}.
     */
    public Map<Long, SalaryRecord> getCurrentSalariesByEmployeeIds(List<Long> employeeIds) {
        if (employeeIds.isEmpty()) {
            return Map.of();
        }

        List<SalaryRecord> applicableRecords = salaryRecordRepository
                .findByEmployee_IdInAndEffectiveFromLessThanEqualOrderByEmployee_IdAscEffectiveFromDescIdDesc(
                        employeeIds, LocalDate.now());

        // Records arrive grouped by employee id, each group ordered with the current record
        // first (latest effectiveFrom not after today, ties broken by id descending) - so the
        // first record seen per employee id is exactly the one to keep.
        Map<Long, SalaryRecord> currentSalaryByEmployeeId = new LinkedHashMap<>();
        for (SalaryRecord record : applicableRecords) {
            currentSalaryByEmployeeId.putIfAbsent(record.getEmployee().getId(), record);
        }
        return currentSalaryByEmployeeId;
    }

    private Employee getEmployee(Long employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employee not found with id: " + employeeId));
    }
}

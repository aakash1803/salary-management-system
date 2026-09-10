package com.salarymanagement.employee;

import com.salarymanagement.common.ConflictException;
import com.salarymanagement.common.NotFoundException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sqlite.SQLiteErrorCode;
import org.sqlite.SQLiteException;

/**
 * Application service for employees: read/search, plus create and update. Returns domain
 * entities; mapping to API response shapes is the controller's responsibility.
 */
@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    /**
     * Paginated, optionally filtered employee search. Delegates entirely to
     * the database via {@link EmployeeRepository#findAll(org.springframework.data.jpa.domain.Specification, Pageable)},
     * so the full employee set is never loaded into memory.
     */
    public Page<Employee> search(String search, String country, String department, Pageable pageable) {
        return employeeRepository.findAll(
                EmployeeSpecifications.filterBy(search, country, department), pageable);
    }

    public Employee getById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Employee not found with id: " + id));
    }

    public Employee getByEmployeeNumber(String employeeNumber) {
        return employeeRepository.findByEmployeeNumber(employeeNumber)
                .orElseThrow(() -> new NotFoundException(
                        "Employee not found with employee number: " + employeeNumber));
    }

    /**
     * Creates a new employee.
     *
     * <p>{@code existsByEmployeeNumber} is checked first purely to give a fast, clean 409 in
     * the common case; it is not sufficient on its own, since two concurrent requests could both
     * pass it for the same employee number before either commits. The actual insert is
     * therefore also wrapped to catch the database's unique constraint - the real, final
     * protection against duplicates - and translate it into the same {@link ConflictException}
     * rather than letting a persistence exception escape to the caller.
     */
    @Transactional
    public Employee create(EmployeeRequest request) {
        if (employeeRepository.existsByEmployeeNumber(request.employeeNumber())) {
            throw conflict(request.employeeNumber());
        }

        Employee employee = new Employee(
                request.employeeNumber(),
                request.firstName(),
                request.lastName(),
                request.email(),
                request.country(),
                request.department());

        try {
            return employeeRepository.saveAndFlush(employee);
        } catch (DataAccessException ex) {
            if (isUniqueConstraintViolation(ex)) {
                throw conflict(request.employeeNumber());
            }
            throw ex;
        }
    }

    /**
     * Updates an existing employee's fields in place. Never creates a new employee, never
     * touches salary records, preserves {@code createdAt}, and updates {@code updatedAt} via the
     * existing {@code @PreUpdate} lifecycle callback on {@link Employee}.
     *
     * <p>The employee-number-conflict check only runs when the employee number is actually
     * changing (comparing against another employee's number when it hasn't changed would always
     * find the employee's own row). See {@link #create(EmployeeRequest)} for why both an
     * upfront check and a caught constraint violation are used together.
     */
    @Transactional
    public Employee update(Long id, EmployeeRequest request) {
        Employee employee = getById(id);

        boolean employeeNumberChanged = !employee.getEmployeeNumber().equals(request.employeeNumber());
        if (employeeNumberChanged && employeeRepository.existsByEmployeeNumber(request.employeeNumber())) {
            throw conflict(request.employeeNumber());
        }

        employee.setEmployeeNumber(request.employeeNumber());
        employee.setFirstName(request.firstName());
        employee.setLastName(request.lastName());
        employee.setEmail(request.email());
        employee.setCountry(request.country());
        employee.setDepartment(request.department());

        try {
            return employeeRepository.saveAndFlush(employee);
        } catch (DataAccessException ex) {
            if (isUniqueConstraintViolation(ex)) {
                throw conflict(request.employeeNumber());
            }
            throw ex;
        }
    }

    private ConflictException conflict(String employeeNumber) {
        return new ConflictException("Employee number already in use: " + employeeNumber);
    }

    /**
     * Determines whether the given exception represents a violation of {@code employee_number}'s
     * unique constraint specifically - as opposed to some other, unrelated database integrity
     * failure that happens to reach this catch block.
     *
     * <p>Most Hibernate dialects let Spring translate a unique constraint violation straight
     * into {@link DataIntegrityViolationException}, which would make this check trivial.
     * However, this project's SQLite dialect (org.hibernate.community.dialect.SQLiteDialect)
     * does not classify SQLite's constraint-violation error that way - Hibernate's generic SQL
     * exception converter does not recognize it, so it surfaces as a
     * {@link org.springframework.orm.jpa.JpaSystemException} instead (confirmed against the
     * real SQLite test datasource; see {@code EmployeeRepositoryTest}). Rather than broadly
     * treating every {@code JpaSystemException} as a conflict - which could mask an unrelated
     * database error as a false 409 - the underlying driver exception's result code is checked
     * directly via the {@code sqlite-jdbc} driver's own {@link SQLiteException}/
     * {@link SQLiteErrorCode} API, and only {@code SQLITE_CONSTRAINT_UNIQUE} is treated as a
     * conflict here.
     *
     * <p>{@code SQLITE_CONSTRAINT_PRIMARYKEY} is deliberately NOT treated as a conflict: this
     * class only ever inserts/updates rows through {@code employeeRepository.saveAndFlush}, and
     * {@link Employee#id} is a {@code GenerationType.IDENTITY} column that this code never sets
     * explicitly, so a primary-key violation is not a realistic outcome of a duplicate
     * {@code employeeNumber} here - if it ever occurred, it would indicate a genuinely different,
     * unrelated integrity problem (e.g. an ID-generation issue), and reporting that as
     * "employee number already in use" would be misleading. A prior version of this method
     * treated both codes as a conflict; narrowing to just {@code SQLITE_CONSTRAINT_UNIQUE} avoids
     * that false classification without losing any real coverage.
     *
     * <p>Similarly, the {@link DataIntegrityViolationException} fast path above is not narrowed
     * further (e.g. by inspecting a translated {@code ConstraintViolationException}'s constraint
     * name): {@link Employee} declares only one unique constraint - on {@code employeeNumber} -
     * and its NOT NULL columns are already enforced by Bean Validation on {@link EmployeeRequest}
     * before a request reaches this service, so in practice this branch is only reachable via that
     * one constraint on portable dialects. Matching by constraint name was considered but rejected
     * as unreliable here, since no explicit constraint name is declared on {@link Employee} and the
     * database would otherwise assign one dialect-specific to SQLite.
     */
    private boolean isUniqueConstraintViolation(Throwable ex) {
        if (ex instanceof DataIntegrityViolationException) {
            return true;
        }
        Throwable current = ex;
        while (current != null) {
            if (current instanceof SQLiteException sqliteException) {
                return sqliteException.getResultCode() == SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE;
            }
            current = current.getCause();
        }
        return false;
    }
}

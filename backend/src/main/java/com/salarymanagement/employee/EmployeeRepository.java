package com.salarymanagement.employee;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/**
 * Data access for {@link Employee}.
 *
 * <p>{@code findAll(Pageable)} (inherited from {@link JpaRepository}) already covers plain,
 * paginated "view employees" access. Dynamic search/filtering (by name, employee number, country,
 * department) will be implemented later via {@link JpaSpecificationExecutor}, so no ad-hoc search
 * query is declared here.
 */
public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

    /**
     * Looks up an employee by their unique business identifier (e.g. for detail views, or to
     * check for a duplicate before creating a new employee).
     */
    Optional<Employee> findByEmployeeNumber(String employeeNumber);

    boolean existsByEmployeeNumber(String employeeNumber);
}

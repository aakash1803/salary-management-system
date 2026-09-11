package com.salarymanagement.employee;

import com.salarymanagement.common.PageResponse;
import com.salarymanagement.salary.SalaryRecord;
import com.salarymanagement.salary.SalaryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Employee search/lookup/create/update API. Salary, dashboard, and authentication endpoints are
 * out of scope for this slice.
 *
 * <p>{@code create}/{@code update} deliberately return the plain {@link EmployeeResponse} body
 * (like every other method here) rather than a {@code ResponseEntity} with a {@code Location}
 * header - this keeps every endpoint in this controller consistent, and a {@code Location}
 * header is optional per the requirements for this slice.
 */
@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final String DEFAULT_SORT_PROPERTY = "employeeNumber";

    private final EmployeeService employeeService;
    private final SalaryService salaryService;

    /**
     * {@code salaryService} is used only to enrich the paginated list response with each
     * employee's current salary/currency in bulk (see {@link #search}) - it does not change the
     * rest of this controller's dependency on {@link EmployeeService} alone.
     */
    public EmployeeController(EmployeeService employeeService, SalaryService salaryService) {
        this.employeeService = employeeService;
        this.salaryService = salaryService;
    }

    @GetMapping
    public PageResponse<EmployeeResponse> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String department,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size,
            @RequestParam(defaultValue = DEFAULT_SORT_PROPERTY) String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {

        if (page < 0) {
            throw new IllegalArgumentException("page must not be negative.");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("size must be greater than zero.");
        }
        if (size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must not exceed " + MAX_PAGE_SIZE + ".");
        }

        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(sortDirection);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("sortDirection must be 'asc' or 'desc'.");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<Employee> employees = employeeService.search(search, country, department, pageable);

        // Current salary/currency for the whole page is fetched in a single bulk query rather
        // than once per employee, so the list endpoint never issues an N+1 query pattern.
        List<Long> employeeIds = employees.getContent().stream().map(Employee::getId).toList();
        Map<Long, SalaryRecord> currentSalariesByEmployeeId =
                salaryService.getCurrentSalariesByEmployeeIds(employeeIds);

        Page<EmployeeResponse> results = employees.map(
                employee -> EmployeeResponse.from(employee, currentSalariesByEmployeeId.get(employee.getId())));

        return PageResponse.from(results);
    }

    @GetMapping("/{id}")
    public EmployeeResponse getById(@PathVariable Long id) {
        return EmployeeResponse.from(employeeService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeResponse create(@Valid @RequestBody EmployeeRequest request) {
        return EmployeeResponse.from(employeeService.create(request));
    }

    @PutMapping("/{id}")
    public EmployeeResponse update(@PathVariable Long id, @Valid @RequestBody EmployeeRequest request) {
        return EmployeeResponse.from(employeeService.update(id, request));
    }
}

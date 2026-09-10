package com.salarymanagement.employee;

import com.salarymanagement.common.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only employee search/lookup API. Create/update, salary, and
 * authentication endpoints are out of scope for this slice.
 */
@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final String DEFAULT_SORT_PROPERTY = "employeeNumber";

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
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

        Page<EmployeeResponse> results = employeeService
                .search(search, country, department, pageable)
                .map(EmployeeResponse::from);

        return PageResponse.from(results);
    }

    @GetMapping("/{id}")
    public EmployeeResponse getById(@PathVariable Long id) {
        return EmployeeResponse.from(employeeService.getById(id));
    }
}

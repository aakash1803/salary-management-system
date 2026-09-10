package com.salarymanagement.salary;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Salary API for a single employee: adding a salary record, viewing the current salary, and
 * viewing full salary history. Dashboard-level salary aggregation is out of scope for this slice.
 */
@RestController
@RequestMapping("/api/employees/{employeeId}/salary")
public class SalaryController {

    private final SalaryService salaryService;

    public SalaryController(SalaryService salaryService) {
        this.salaryService = salaryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SalaryResponse addSalary(@PathVariable Long employeeId, @Valid @RequestBody SalaryRequest request) {
        return SalaryResponse.from(salaryService.addSalary(employeeId, request));
    }

    @GetMapping
    public SalaryResponse getCurrentSalary(@PathVariable Long employeeId) {
        return SalaryResponse.from(salaryService.getCurrentSalary(employeeId));
    }

    @GetMapping("/history")
    public List<SalaryResponse> getSalaryHistory(@PathVariable Long employeeId) {
        return salaryService.getSalaryHistory(employeeId).stream()
                .map(SalaryResponse::from)
                .toList();
    }
}

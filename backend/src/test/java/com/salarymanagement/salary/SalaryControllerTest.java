package com.salarymanagement.salary;

import com.salarymanagement.common.NotFoundException;
import com.salarymanagement.employee.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer slice tests for {@link SalaryController}.
 *
 * <p>JWT authentication protects these endpoints in production (see
 * {@code com.salarymanagement.auth.SecurityConfig}), but Spring Security's filter chain is
 * deliberately disabled here via {@code @AutoConfigureMockMvc(addFilters = false)}, the same as
 * {@code EmployeeControllerTest}: this class is a controller-layer slice test focused on request
 * validation, response mapping, and status-code behavior, independent of authentication.
 * Authentication/security behavior itself is covered separately by the authentication integration
 * tests.
 */
@WebMvcTest(SalaryController.class)
@AutoConfigureMockMvc(addFilters = false)
class SalaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SalaryService salaryService;

    @Autowired
    private ObjectMapper objectMapper;

    private Employee sampleEmployee() {
        return new Employee("EMP-201", "Ada", "Lovelace", "ada@example.com", "UK", "Engineering");
    }

    private SalaryRecord salaryRecord(BigDecimal amount, String currency, LocalDate effectiveFrom) {
        return new SalaryRecord(sampleEmployee(), amount, currency, effectiveFrom);
    }

    @Test
    void addSalaryReturnsCreated() throws Exception {
        SalaryRequest request = new SalaryRequest(new BigDecimal("50000.00"), "GBP", LocalDate.now());
        SalaryRecord created = salaryRecord(new BigDecimal("50000.00"), "GBP", LocalDate.now());

        when(salaryService.addSalary(eq(1L), any(SalaryRequest.class))).thenReturn(created);

        mockMvc.perform(post("/api/employees/1/salary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(50000.00))
                .andExpect(jsonPath("$.currency").value("GBP"));
    }

    @Test
    void addSalaryForNonexistentEmployeeReturnsNotFound() throws Exception {
        SalaryRequest request = new SalaryRequest(new BigDecimal("50000.00"), "GBP", LocalDate.now());

        when(salaryService.addSalary(eq(99L), any(SalaryRequest.class)))
                .thenThrow(new NotFoundException("Employee not found with id: 99"));

        mockMvc.perform(post("/api/employees/99/salary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void addSalaryWithNonPositiveAmountReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/employees/1/salary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":0,\"currency\":\"GBP\",\"effectiveFrom\":\"2026-01-01\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addSalaryWithNegativeAmountReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/employees/1/salary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":-100.00,\"currency\":\"GBP\",\"effectiveFrom\":\"2026-01-01\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addSalaryWithBlankCurrencyReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/employees/1/salary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":50000.00,\"currency\":\"\",\"effectiveFrom\":\"2026-01-01\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addSalaryWithWrongLengthCurrencyReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/employees/1/salary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":50000.00,\"currency\":\"GBPX\",\"effectiveFrom\":\"2026-01-01\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addSalaryWithMissingEffectiveFromReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/employees/1/salary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":50000.00,\"currency\":\"GBP\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addSalaryWithMalformedJsonReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/employees/1/salary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-valid-json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCurrentSalaryReturnsOk() throws Exception {
        SalaryRecord current = salaryRecord(new BigDecimal("60000.00"), "GBP", LocalDate.now());

        when(salaryService.getCurrentSalary(1L)).thenReturn(current);

        mockMvc.perform(get("/api/employees/1/salary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(60000.00))
                .andExpect(jsonPath("$.currency").value("GBP"));
    }

    @Test
    void getCurrentSalaryForNonexistentEmployeeReturnsNotFound() throws Exception {
        when(salaryService.getCurrentSalary(99L))
                .thenThrow(new NotFoundException("Employee not found with id: 99"));

        mockMvc.perform(get("/api/employees/99/salary"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCurrentSalaryWithOnlyFutureDatedSalariesReturnsNotFound() throws Exception {
        when(salaryService.getCurrentSalary(1L))
                .thenThrow(new NotFoundException("No salary currently effective for employee with id: 1"));

        mockMvc.perform(get("/api/employees/1/salary"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getSalaryHistoryReturnsOk() throws Exception {
        SalaryRecord newer = salaryRecord(new BigDecimal("55000.00"), "GBP", LocalDate.now());
        SalaryRecord older = salaryRecord(new BigDecimal("50000.00"), "GBP", LocalDate.now().minusYears(1));

        when(salaryService.getSalaryHistory(1L)).thenReturn(List.of(newer, older));

        mockMvc.perform(get("/api/employees/1/salary/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount").value(55000.00))
                .andExpect(jsonPath("$[1].amount").value(50000.00));
    }

    @Test
    void getSalaryHistoryReturnsEmptyListWhenEmployeeHasNoRecords() throws Exception {
        when(salaryService.getSalaryHistory(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/employees/1/salary/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getSalaryHistoryForNonexistentEmployeeReturnsNotFound() throws Exception {
        when(salaryService.getSalaryHistory(99L))
                .thenThrow(new NotFoundException("Employee not found with id: 99"));

        mockMvc.perform(get("/api/employees/99/salary/history"))
                .andExpect(status().isNotFound());
    }
}

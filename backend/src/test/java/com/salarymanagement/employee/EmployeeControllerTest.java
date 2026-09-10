package com.salarymanagement.employee;

import com.salarymanagement.common.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer slice tests for {@link EmployeeController}. Spring Security's
 * default auto-configured filter chain is disabled here (authentication is
 * not implemented yet); production security configuration is untouched.
 */
@WebMvcTest(EmployeeController.class)
@AutoConfigureMockMvc(addFilters = false)
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmployeeService employeeService;

    private Employee sampleEmployee() {
        return new Employee("EMP-001", "Alice", "Johnson",
                "alice.johnson@example.com", "United Kingdom", "Engineering");
    }

    @Test
    void listEmployeesReturnsAPageOfResults() throws Exception {
        Page<Employee> page = new PageImpl<>(List.of(sampleEmployee()));
        when(employeeService.search(any(), any(), any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].employeeNumber").value("EMP-001"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listEmployeesRejectsNegativePage() throws Exception {
        mockMvc.perform(get("/api/employees").param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listEmployeesRejectsNonPositiveSize() throws Exception {
        mockMvc.perform(get("/api/employees").param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listEmployeesRejectsSizeAboveMaximum() throws Exception {
        mockMvc.perform(get("/api/employees").param("size", "1000"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getByIdReturnsTheEmployeeWhenFound() throws Exception {
        when(employeeService.getById(1L)).thenReturn(sampleEmployee());

        mockMvc.perform(get("/api/employees/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeNumber").value("EMP-001"));
    }

    @Test
    void getByIdReturnsNotFoundWhenEmployeeDoesNotExist() throws Exception {
        when(employeeService.getById(99L)).thenThrow(new NotFoundException("Employee not found with id: 99"));

        mockMvc.perform(get("/api/employees/99"))
                .andExpect(status().isNotFound());
    }
}

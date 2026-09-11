package com.salarymanagement.employee;

import com.salarymanagement.common.ConflictException;
import com.salarymanagement.common.NotFoundException;
import com.salarymanagement.salary.SalaryRecord;
import com.salarymanagement.salary.SalaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer slice tests for {@link EmployeeController}.
 *
 * <p>JWT authentication is implemented and protects these endpoints in production (see
 * {@code com.salarymanagement.auth.SecurityConfig}), but Spring Security's filter chain is
 * deliberately disabled here via {@code @AutoConfigureMockMvc(addFilters = false)}: this class is
 * a controller-layer slice test focused on request validation, response mapping, and status-code
 * behavior, independent of authentication. Authentication/security behavior itself (valid/invalid/
 * missing JWT, login success/failure) is covered separately by the authentication integration
 * tests ({@code AuthControllerTest}, {@code ProtectedEndpointAccessTest}). Production security
 * configuration is untouched by disabling filters here.
 */
@WebMvcTest(EmployeeController.class)
@AutoConfigureMockMvc(addFilters = false)
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmployeeService employeeService;

    @MockitoBean
    private SalaryService salaryService;

    @Autowired
    private ObjectMapper objectMapper;

    private Employee sampleEmployee() {
        return new Employee("EMP-001", "Alice", "Johnson",
                "alice.johnson@example.com", "United Kingdom", "Engineering");
    }

    /**
     * Default stub so every existing test - most of which don't care about salary data at all -
     * keeps working unchanged: {@link EmployeeController#search} always calls
     * {@code salaryService.getCurrentSalariesByEmployeeIds(...)} to enrich the page, and this
     * gives it an empty map (i.e. "no employee on this page has a current salary") unless a test
     * overrides it.
     */
    @BeforeEach
    void stubNoCurrentSalariesByDefault() {
        // Collections.emptyMap() rather than Map.of(): several existing tests below use
        // transient (never-persisted) Employee instances whose id is null, and looking such an
        // id up in this default stub must not itself throw (Map.of()'s get() rejects a null
        // key with NullPointerException; Collections.emptyMap() simply returns null).
        when(salaryService.getCurrentSalariesByEmployeeIds(any())).thenReturn(Collections.emptyMap());
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
    void listEmployeesIncludesCurrentSalaryWhenAnActiveSalaryRecordExists() throws Exception {
        Employee employee = sampleEmployee();
        // Id assigned via reflection (mirroring @GeneratedValue/IDENTITY behavior on a real
        // save) since this entity is never persisted in this controller slice test.
        ReflectionTestUtils.setField(employee, "id", 1L);
        Page<Employee> page = new PageImpl<>(List.of(employee));
        SalaryRecord currentSalary = new SalaryRecord(employee, new BigDecimal("85000.00"), "GBP", LocalDate.now());

        when(employeeService.search(any(), any(), any(), any(Pageable.class))).thenReturn(page);
        when(salaryService.getCurrentSalariesByEmployeeIds(any()))
                .thenReturn(Map.of(employee.getId(), currentSalary));

        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].currentSalary").value(85000.00))
                .andExpect(jsonPath("$.content[0].currency").value("GBP"));
    }

    @Test
    void listEmployeesReturnsNullSalaryFieldsWhenNoActiveSalaryRecordExists() throws Exception {
        Page<Employee> page = new PageImpl<>(List.of(sampleEmployee()));

        when(employeeService.search(any(), any(), any(), any(Pageable.class))).thenReturn(page);
        when(salaryService.getCurrentSalariesByEmployeeIds(any())).thenReturn(Collections.emptyMap());

        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].currentSalary").doesNotExist())
                .andExpect(jsonPath("$.content[0].currency").doesNotExist());
    }

    @Test
    void listEmployeesStillSupportsPaginationAlongsideSalaryEnrichment() throws Exception {
        Page<Employee> page = new PageImpl<>(
                List.of(sampleEmployee()), org.springframework.data.domain.PageRequest.of(1, 5), 12);

        when(employeeService.search(any(), any(), any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/employees").param("page", "1").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(12));
    }

    @Test
    void listEmployeesStillSupportsSearchAndFilterParamsAlongsideSalaryEnrichment() throws Exception {
        Page<Employee> page = new PageImpl<>(List.of(sampleEmployee()));

        when(employeeService.search(eq("Alice"), eq("United Kingdom"), eq("Engineering"), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/employees")
                        .param("search", "Alice")
                        .param("country", "United Kingdom")
                        .param("department", "Engineering"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].employeeNumber").value("EMP-001"));
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

    @Test
    void createEmployeeReturnsCreated() throws Exception {
        EmployeeRequest request = new EmployeeRequest(
                "EMP-002", "Bob", "Smith", "bob.smith@example.com", "United States", "Sales");
        Employee created = new Employee("EMP-002", "Bob", "Smith",
                "bob.smith@example.com", "United States", "Sales");

        when(employeeService.create(any(EmployeeRequest.class))).thenReturn(created);

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeNumber").value("EMP-002"));
    }

    @Test
    void createEmployeeWithBlankFieldReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeNumber\":\"\",\"firstName\":\"Bob\",\"lastName\":\"Smith\","
                                + "\"email\":\"bob.smith@example.com\",\"country\":\"US\",\"department\":\"Sales\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createEmployeeWithMalformedJsonReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-valid-json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createEmployeeWithDuplicateEmployeeNumberReturnsConflict() throws Exception {
        EmployeeRequest request = new EmployeeRequest(
                "EMP-001", "Bob", "Smith", "bob.smith@example.com", "United States", "Sales");

        when(employeeService.create(any(EmployeeRequest.class)))
                .thenThrow(new ConflictException("Employee number already in use: EMP-001"));

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void updateEmployeeReturnsOk() throws Exception {
        EmployeeRequest request = new EmployeeRequest(
                "EMP-001", "Alicia", "Johnson", "alicia.johnson@example.com", "United Kingdom", "Product");
        Employee updated = new Employee("EMP-001", "Alicia", "Johnson",
                "alicia.johnson@example.com", "United Kingdom", "Product");

        when(employeeService.update(eq(1L), any(EmployeeRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/employees/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Alicia"));
    }

    @Test
    void updateNonexistentEmployeeReturnsNotFound() throws Exception {
        EmployeeRequest request = new EmployeeRequest(
                "EMP-001", "Alicia", "Johnson", "alicia.johnson@example.com", "United Kingdom", "Product");

        when(employeeService.update(eq(99L), any(EmployeeRequest.class)))
                .thenThrow(new NotFoundException("Employee not found with id: 99"));

        mockMvc.perform(put("/api/employees/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateEmployeeWithDuplicateEmployeeNumberReturnsConflict() throws Exception {
        EmployeeRequest request = new EmployeeRequest(
                "EMP-999", "Alice", "Johnson", "alice.johnson@example.com", "United Kingdom", "Engineering");

        when(employeeService.update(eq(1L), any(EmployeeRequest.class)))
                .thenThrow(new ConflictException("Employee number already in use: EMP-999"));

        mockMvc.perform(put("/api/employees/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }
}

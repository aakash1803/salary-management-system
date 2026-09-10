package com.salarymanagement.dashboard;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer slice tests for {@link DashboardController}.
 *
 * <p>JWT authentication protects this endpoint in production purely because it falls under
 * {@code SecurityConfig}'s {@code anyRequest().authenticated()} rule, the same as every other
 * business endpoint - but as with {@code EmployeeControllerTest}/{@code SalaryControllerTest},
 * Spring Security's filter chain is deliberately disabled here via
 * {@code @AutoConfigureMockMvc(addFilters = false)}, since this class is a controller-layer slice
 * test focused on response shape, independent of authentication. That authentication actually
 * applies to {@code /api/dashboard} is verified separately, with the real filter chain enabled,
 * in {@code ProtectedEndpointAccessTest}.
 */
@WebMvcTest(DashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @Test
    void getDashboardReturnsOkWithTheExpectedShape() throws Exception {
        DashboardResponse response = new DashboardResponse(
                2,
                List.of(new DashboardResponse.SalaryMetrics(
                        "GBP", new BigDecimal("45000.00"), new BigDecimal("62000.00"), new BigDecimal("53500.00"))),
                List.of(new DashboardResponse.EmployeeCountByCountry("UK", 2)),
                List.of(new DashboardResponse.SalaryDistributionByCurrency("GBP", List.of(
                        new DashboardResponse.SalaryBand("30,000 - 59,999.99", 1),
                        new DashboardResponse.SalaryBand("60,000 - 99,999.99", 1)))),
                List.of(new DashboardResponse.PayrollByCountry("UK", "GBP", new BigDecimal("107000.00"))),
                List.of(new DashboardResponse.PayrollByCurrency("GBP", new BigDecimal("107000.00")))
        );
        when(dashboardService.getDashboard()).thenReturn(response);

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEmployees").value(2))
                .andExpect(jsonPath("$.salaryMetricsByCurrency[0].currency").value("GBP"))
                .andExpect(jsonPath("$.salaryMetricsByCurrency[0].minimum").value(45000.00))
                .andExpect(jsonPath("$.employeesByCountry[0].country").value("UK"))
                .andExpect(jsonPath("$.employeesByCountry[0].employeeCount").value(2))
                .andExpect(jsonPath("$.salaryDistribution[0].currency").value("GBP"))
                .andExpect(jsonPath("$.salaryDistribution[0].bands[0].range").value("30,000 - 59,999.99"))
                .andExpect(jsonPath("$.payrollByCountry[0].country").value("UK"))
                .andExpect(jsonPath("$.payrollByCountry[0].totalPayroll").value(107000.00))
                .andExpect(jsonPath("$.payrollByCurrency[0].currency").value("GBP"));
    }

    @Test
    void getDashboardReturnsOkWithEmptyAggregatesWhenThereIsNoData() throws Exception {
        DashboardResponse empty = new DashboardResponse(0, List.of(), List.of(), List.of(), List.of(), List.of());
        when(dashboardService.getDashboard()).thenReturn(empty);

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEmployees").value(0))
                .andExpect(jsonPath("$.salaryMetricsByCurrency").isArray())
                .andExpect(jsonPath("$.salaryMetricsByCurrency").isEmpty())
                .andExpect(jsonPath("$.employeesByCountry").isEmpty())
                .andExpect(jsonPath("$.salaryDistribution").isEmpty())
                .andExpect(jsonPath("$.payrollByCountry").isEmpty())
                .andExpect(jsonPath("$.payrollByCurrency").isEmpty());
    }
}

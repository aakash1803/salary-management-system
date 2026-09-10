package com.salarymanagement.dashboard;

import com.salarymanagement.employee.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure Mockito unit tests for {@link DashboardService} - no Spring context is loaded, so these
 * run fast and do not touch the database. The actual aggregation (counting, summing, averaging,
 * bucketing) is a database query concern and is verified against the real SQLite datasource in
 * {@code DashboardRepositoryTest}; here we only verify that the service maps already-aggregated
 * repository results into the response shape correctly, and orchestrates its repositories with
 * the right arguments.
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DashboardRepository dashboardRepository;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(employeeRepository, dashboardRepository);
    }

    private DashboardRepository.EmployeeCountByCountryRow countryRow(String country, long count) {
        DashboardRepository.EmployeeCountByCountryRow row = mock(DashboardRepository.EmployeeCountByCountryRow.class);
        when(row.getCountry()).thenReturn(country);
        when(row.getEmployeeCount()).thenReturn(count);
        return row;
    }

    private DashboardRepository.SalaryMetricsRow metricsRow(String currency, double min, double max, double avg) {
        DashboardRepository.SalaryMetricsRow row = mock(DashboardRepository.SalaryMetricsRow.class);
        when(row.getCurrency()).thenReturn(currency);
        when(row.getMinimum()).thenReturn(min);
        when(row.getMaximum()).thenReturn(max);
        when(row.getAverage()).thenReturn(avg);
        return row;
    }

    private DashboardRepository.SalaryDistributionRow distributionRow(String currency, int bandIndex, long count) {
        DashboardRepository.SalaryDistributionRow row = mock(DashboardRepository.SalaryDistributionRow.class);
        when(row.getCurrency()).thenReturn(currency);
        when(row.getBandIndex()).thenReturn(bandIndex);
        when(row.getEmployeeCount()).thenReturn(count);
        return row;
    }

    private DashboardRepository.PayrollByCountryRow payrollByCountryRow(String country, String currency, double total) {
        DashboardRepository.PayrollByCountryRow row = mock(DashboardRepository.PayrollByCountryRow.class);
        when(row.getCountry()).thenReturn(country);
        when(row.getCurrency()).thenReturn(currency);
        when(row.getTotalPayroll()).thenReturn(total);
        return row;
    }

    private DashboardRepository.PayrollByCurrencyRow payrollByCurrencyRow(String currency, double total) {
        DashboardRepository.PayrollByCurrencyRow row = mock(DashboardRepository.PayrollByCurrencyRow.class);
        when(row.getCurrency()).thenReturn(currency);
        when(row.getTotalPayroll()).thenReturn(total);
        return row;
    }

    @Test
    void getDashboardMapsAggregatedRepositoryResultsIntoTheResponseShape() {
        // Each row list is built into a local variable BEFORE any when(...).thenReturn(...) call
        // below starts: countryRow()/metricsRow()/etc. each do their own when(...).thenReturn(...)
        // stubbing internally, and constructing them inline as an argument to an outer
        // when(...).thenReturn(...) call trips Mockito's UnfinishedStubbingException, since Java
        // evaluates the outer when(...) before evaluating its thenReturn(...) argument.
        List<DashboardRepository.EmployeeCountByCountryRow> countryRows =
                List.of(countryRow("UK", 3), countryRow("US", 2));
        List<DashboardRepository.SalaryMetricsRow> metricsRows =
                List.of(metricsRow("GBP", 45000.0, 62000.0, 54000.0));
        List<DashboardRepository.SalaryDistributionRow> distributionRows =
                List.of(distributionRow("GBP", 1, 2), distributionRow("GBP", 2, 1));
        List<DashboardRepository.PayrollByCountryRow> payrollByCountryRows =
                List.of(payrollByCountryRow("UK", "GBP", 162000.0));
        List<DashboardRepository.PayrollByCurrencyRow> payrollByCurrencyRows =
                List.of(payrollByCurrencyRow("GBP", 162000.0));

        when(employeeRepository.count()).thenReturn(5L);
        when(dashboardRepository.countEmployeesByCountry()).thenReturn(countryRows);
        when(dashboardRepository.findSalaryMetricsByCurrency(any())).thenReturn(metricsRows);
        when(dashboardRepository.findSalaryDistribution(any(), any(), any(), any(), any()))
                .thenReturn(distributionRows);
        when(dashboardRepository.findPayrollByCountry(any())).thenReturn(payrollByCountryRows);
        when(dashboardRepository.findPayrollByCurrency(any())).thenReturn(payrollByCurrencyRows);

        DashboardResponse response = dashboardService.getDashboard();

        assertEquals(5L, response.totalEmployees());

        assertEquals(2, response.employeesByCountry().size());
        assertEquals("UK", response.employeesByCountry().get(0).country());
        assertEquals(3, response.employeesByCountry().get(0).employeeCount());

        assertEquals(1, response.salaryMetricsByCurrency().size());
        DashboardResponse.SalaryMetrics gbpMetrics = response.salaryMetricsByCurrency().get(0);
        assertEquals("GBP", gbpMetrics.currency());
        assertEquals(new BigDecimal("45000.00"), gbpMetrics.minimum());
        assertEquals(new BigDecimal("62000.00"), gbpMetrics.maximum());
        assertEquals(new BigDecimal("54000.00"), gbpMetrics.average());

        assertEquals(1, response.salaryDistribution().size());
        DashboardResponse.SalaryDistributionByCurrency gbpDistribution = response.salaryDistribution().get(0);
        assertEquals("GBP", gbpDistribution.currency());
        assertEquals(2, gbpDistribution.bands().size());
        assertEquals("30,000 - 59,999.99", gbpDistribution.bands().get(0).range());
        assertEquals(2, gbpDistribution.bands().get(0).employeeCount());
        assertEquals("60,000 - 99,999.99", gbpDistribution.bands().get(1).range());
        assertEquals(1, gbpDistribution.bands().get(1).employeeCount());

        assertEquals(1, response.payrollByCountry().size());
        assertEquals("UK", response.payrollByCountry().get(0).country());
        assertEquals("GBP", response.payrollByCountry().get(0).currency());
        assertEquals(new BigDecimal("162000.00"), response.payrollByCountry().get(0).totalPayroll());

        assertEquals(1, response.payrollByCurrency().size());
        assertEquals("GBP", response.payrollByCurrency().get(0).currency());
        assertEquals(new BigDecimal("162000.00"), response.payrollByCurrency().get(0).totalPayroll());
    }

    @Test
    void getDashboardReturnsEmptyAggregatesWhenThereIsNoData() {
        when(employeeRepository.count()).thenReturn(0L);
        when(dashboardRepository.countEmployeesByCountry()).thenReturn(List.of());
        when(dashboardRepository.findSalaryMetricsByCurrency(any())).thenReturn(List.of());
        when(dashboardRepository.findSalaryDistribution(any(), any(), any(), any(), any())).thenReturn(List.of());
        when(dashboardRepository.findPayrollByCountry(any())).thenReturn(List.of());
        when(dashboardRepository.findPayrollByCurrency(any())).thenReturn(List.of());

        DashboardResponse response = dashboardService.getDashboard();

        assertEquals(0L, response.totalEmployees());
        assertTrue(response.employeesByCountry().isEmpty());
        assertTrue(response.salaryMetricsByCurrency().isEmpty());
        assertTrue(response.salaryDistribution().isEmpty());
        assertTrue(response.payrollByCountry().isEmpty());
        assertTrue(response.payrollByCurrency().isEmpty());
    }

    @Test
    void getDashboardQueriesAsOfTodayUsingTheDocumentedBandBoundaries() {
        when(employeeRepository.count()).thenReturn(0L);
        when(dashboardRepository.countEmployeesByCountry()).thenReturn(List.of());
        when(dashboardRepository.findSalaryMetricsByCurrency(any())).thenReturn(List.of());
        when(dashboardRepository.findSalaryDistribution(any(), any(), any(), any(), any())).thenReturn(List.of());
        when(dashboardRepository.findPayrollByCountry(any())).thenReturn(List.of());
        when(dashboardRepository.findPayrollByCurrency(any())).thenReturn(List.of());

        dashboardService.getDashboard();

        ArgumentCaptor<LocalDate> asOfDateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(dashboardRepository).findSalaryMetricsByCurrency(asOfDateCaptor.capture());
        assertEquals(LocalDate.now(), asOfDateCaptor.getValue());

        ArgumentCaptor<BigDecimal> band1Captor = ArgumentCaptor.forClass(BigDecimal.class);
        ArgumentCaptor<BigDecimal> band2Captor = ArgumentCaptor.forClass(BigDecimal.class);
        ArgumentCaptor<BigDecimal> band3Captor = ArgumentCaptor.forClass(BigDecimal.class);
        ArgumentCaptor<BigDecimal> band4Captor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(dashboardRepository).findSalaryDistribution(any(), band1Captor.capture(), band2Captor.capture(),
                band3Captor.capture(), band4Captor.capture());
        assertEquals(new BigDecimal("30000"), band1Captor.getValue());
        assertEquals(new BigDecimal("60000"), band2Captor.getValue());
        assertEquals(new BigDecimal("100000"), band3Captor.getValue());
        assertEquals(new BigDecimal("150000"), band4Captor.getValue());
    }
}

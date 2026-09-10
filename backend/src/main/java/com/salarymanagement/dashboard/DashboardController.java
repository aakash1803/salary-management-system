package com.salarymanagement.dashboard;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only dashboard/insights API. A single endpoint returning every figure at once - there is
 * no per-metric endpoint - since the dashboard is consumed as one summary view.
 *
 * <p>No security annotations appear here: like every other business endpoint in this project,
 * this path requires authentication purely because {@code SecurityConfig}'s
 * {@code anyRequest().authenticated()} rule already covers it (only {@code /api/auth/**} is
 * exempted) - no security configuration changes were needed for this endpoint.
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public DashboardResponse getDashboard() {
        return dashboardService.getDashboard();
    }
}

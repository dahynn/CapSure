// #Demo Setting
package com.capsule.insurance.dashboard.api;

import com.capsule.insurance.common.response.ApiResponse;
import com.capsule.insurance.dashboard.application.DashboardService;
import com.capsule.insurance.dashboard.dto.DashboardSummary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public ApiResponse<DashboardSummary> getSummary() {
        return ApiResponse.success(dashboardService.getSummary());
    }
}

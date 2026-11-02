// #Demo Setting
package com.capsule.insurance.dashboard.api;

import com.capsule.insurance.common.response.ApiResponse;
import com.capsule.insurance.dashboard.application.DashboardService;
import com.capsule.insurance.dashboard.dto.DashboardSummary;
import com.capsule.insurance.dashboard.dto.HomeDashboardResponse;
import org.springframework.security.core.Authentication;
import com.capsule.insurance.common.security.AuthenticatedUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public ApiResponse<DashboardSummary> getSummary(Authentication authentication) {
        return ApiResponse.success(dashboardService.getSummary(AuthenticatedUser.id(authentication)));
    }

    @PostMapping("/audits/read")
    public ApiResponse<DashboardSummary> markAuditsRead(Authentication authentication) {
        return ApiResponse.success(dashboardService.markAuditsRead(AuthenticatedUser.id(authentication)));
    }

    @GetMapping("/home")
    public ApiResponse<HomeDashboardResponse> getHomeDashboard(
            Authentication authentication
    ) {
        Long userId = AuthenticatedUser.id(authentication);
        return ApiResponse.success(dashboardService.getHomeDashboard(userId));
    }

}

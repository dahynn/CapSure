// #Demo Setting
package com.capsule.insurance.dashboard.api;

import com.capsule.insurance.common.response.ApiResponse;
import com.capsule.insurance.dashboard.application.DashboardService;
import com.capsule.insurance.dashboard.dto.DashboardSummary;
import com.capsule.insurance.dashboard.dto.HomeDashboardResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
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
    public ApiResponse<DashboardSummary> getSummary() {
        return ApiResponse.success(dashboardService.getSummary());
    }

    @GetMapping("/home")
    public ApiResponse<HomeDashboardResponse> getHomeDashboard(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = resolveUserId(userDetails);
        return ApiResponse.success(dashboardService.getHomeDashboard(userId));
    }

    private Long resolveUserId(UserDetails userDetails) {
        if (userDetails == null || userDetails.getUsername() == null) {
            return 1L;
        }
        try {
            return Long.parseLong(userDetails.getUsername());
        } catch (NumberFormatException exception) {
            return 1L;
        }
    }
}

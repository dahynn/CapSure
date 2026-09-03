package com.capsule.insurance.operations.dashboard.api;

import com.capsule.insurance.common.response.ApiResponse;
import com.capsule.insurance.operations.dashboard.application.OperationsDashboardService;
import com.capsule.insurance.operations.dashboard.dto.OperationsDashboardResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ops")
public class OperationsDashboardController {

    private final OperationsDashboardService service;

    public OperationsDashboardController(OperationsDashboardService service) {
        this.service = service;
    }

    @GetMapping("/dashboard")
    public ApiResponse<OperationsDashboardResponse> dashboard(
            @RequestParam(defaultValue = "8") int recentLimit
    ) {
        return ApiResponse.success(service.getDashboard(recentLimit));
    }
}

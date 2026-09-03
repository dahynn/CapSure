package com.capsule.insurance.information.api;

import com.capsule.insurance.common.response.ApiResponse;
import com.capsule.insurance.common.security.AuthenticatedUser;
import com.capsule.insurance.information.application.PolicyTimelineService;
import com.capsule.insurance.information.dto.PolicyTimelineEventResponse;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/policies")
public class PolicyTimelineController {

    private final PolicyTimelineService timelineService;

    public PolicyTimelineController(PolicyTimelineService timelineService) {
        this.timelineService = timelineService;
    }

    @GetMapping("/{policyId}/timeline")
    public ApiResponse<List<PolicyTimelineEventResponse>> get(
            @PathVariable Long policyId,
            Authentication authentication
    ) {
        return ApiResponse.success(timelineService.get(
                AuthenticatedUser.id(authentication),
                policyId
        ));
    }
}

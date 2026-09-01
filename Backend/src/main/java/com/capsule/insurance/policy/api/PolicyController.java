package com.capsule.insurance.policy.api;

import com.capsule.insurance.common.response.ApiResponse;
import com.capsule.insurance.common.security.AuthenticatedUser;
import com.capsule.insurance.policy.application.PolicyService;
import com.capsule.insurance.policy.domain.InsurancePolicy;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/policies")
public class PolicyController {

    private final PolicyService policyService;

    public PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    @GetMapping("/{policyId}")
    public ApiResponse<InsurancePolicy> get(
            @PathVariable Long policyId,
            Authentication authentication
    ) {
        return ApiResponse.success(policyService.get(
                AuthenticatedUser.id(authentication),
                policyId
        ));
    }
}

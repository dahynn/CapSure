package com.capsule.insurance.policy.change;

import com.capsule.insurance.common.response.ApiResponse;
import com.capsule.insurance.common.security.AuthenticatedUser;
import com.capsule.insurance.premiumcollection.domain.PremiumCollectionSnapshot;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/policies/{policyId}")
public class CoverageChangeController {
    private final CoverageChangeService service;
    public CoverageChangeController(CoverageChangeService service){this.service=service;}
    @PostMapping("/coverage-changes")
    public ApiResponse<CoverageChangeService.ChangeView> schedule(@PathVariable Long policyId,
            @RequestHeader("Idempotency-Key") String key,@Valid @RequestBody CoverageChangeService.ChangeRequest request,Authentication auth){
        return ApiResponse.success(service.schedule(AuthenticatedUser.id(auth),policyId,key,request));
    }
    @GetMapping("/coverage-changes")
    public ApiResponse<List<CoverageChangeService.ChangeView>> history(@PathVariable Long policyId,Authentication auth){
        return ApiResponse.success(service.history(AuthenticatedUser.id(auth),policyId));
    }
    @PostMapping("/next-premium")
    public ApiResponse<PremiumCollectionSnapshot> nextPremium(@PathVariable Long policyId,Authentication auth){
        return ApiResponse.success(service.createNextPremium(AuthenticatedUser.id(auth),policyId));
    }
}

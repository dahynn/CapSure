package com.capsule.insurance.claim.api;

import com.capsule.insurance.claim.application.ClaimService;
import com.capsule.insurance.claim.dto.ClaimResponse;
import com.capsule.insurance.claim.dto.CreateClaimRequest;
import com.capsule.insurance.claim.dto.UpsertClaimEvidenceRequest;
import com.capsule.insurance.common.response.ApiResponse;
import com.capsule.insurance.common.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ClaimController {

    private final ClaimService claimService;

    public ClaimController(ClaimService claimService) {
        this.claimService = claimService;
    }

    @PostMapping("/policies/{policyId}/claims")
    public ApiResponse<ClaimResponse> create(
            @PathVariable Long policyId,
            @Valid @RequestBody CreateClaimRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(claimService.create(
                AuthenticatedUser.id(authentication),
                policyId,
                request
        ));
    }

    @PutMapping("/claims/{claimId}/evidence")
    public ApiResponse<ClaimResponse> recordEvidence(
            @PathVariable Long claimId,
            @Valid @RequestBody UpsertClaimEvidenceRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(claimService.recordEvidence(
                AuthenticatedUser.id(authentication),
                claimId,
                request
        ));
    }

    @PostMapping("/claims/{claimId}/submit")
    public ApiResponse<ClaimResponse> submit(
            @PathVariable Long claimId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            Authentication authentication
    ) {
        return ApiResponse.success(claimService.submit(
                AuthenticatedUser.id(authentication),
                claimId,
                idempotencyKey
        ));
    }

    @PostMapping("/claims/{claimId}/payments")
    public ApiResponse<ClaimResponse> pay(
            @PathVariable Long claimId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            Authentication authentication
    ) {
        return ApiResponse.success(claimService.pay(
                AuthenticatedUser.id(authentication),
                claimId,
                idempotencyKey
        ));
    }

    @GetMapping("/claims/{claimId}")
    public ApiResponse<ClaimResponse> get(
            @PathVariable Long claimId,
            Authentication authentication
    ) {
        return ApiResponse.success(claimService.get(
                AuthenticatedUser.id(authentication),
                claimId
        ));
    }
}

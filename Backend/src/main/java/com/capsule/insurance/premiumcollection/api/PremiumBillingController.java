package com.capsule.insurance.premiumcollection.api;

import com.capsule.insurance.common.response.ApiResponse;
import com.capsule.insurance.common.security.AuthenticatedUser;
import com.capsule.insurance.premiumcollection.application.PremiumBillingService;
import com.capsule.insurance.premiumcollection.dto.PremiumBillingRunResponse;
import com.capsule.insurance.premiumcollection.dto.StartPremiumBillingRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ops/premium-collections/billing")
public class PremiumBillingController {
    private final PremiumBillingService service;

    public PremiumBillingController(PremiumBillingService service) {
        this.service = service;
    }

    @PostMapping("/runs")
    public ApiResponse<PremiumBillingRunResponse> run(
            @Valid @RequestBody StartPremiumBillingRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(service.run(
                request.instanceKey(),
                request.billingCycle(),
                AuthenticatedUser.id(authentication),
                request.reason()
        ));
    }

    @PostMapping("/runs/{id}/resume")
    public ApiResponse<PremiumBillingRunResponse> resume(
            @PathVariable long id,
            @Valid @RequestBody ResumeRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(service.resume(id, AuthenticatedUser.id(authentication), request.reason()));
    }

    @GetMapping("/runs")
    public ApiResponse<List<PremiumBillingRunResponse>> recent() {
        return ApiResponse.success(service.recent());
    }

    @GetMapping("/runs/{id}")
    public ApiResponse<PremiumBillingRunResponse> get(@PathVariable long id) {
        return ApiResponse.success(service.get(id));
    }

    public record ResumeRequest(@NotBlank @Size(max = 500) String reason) {
    }
}

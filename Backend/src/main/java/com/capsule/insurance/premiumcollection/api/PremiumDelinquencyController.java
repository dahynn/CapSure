package com.capsule.insurance.premiumcollection.api;

import com.capsule.insurance.common.response.ApiResponse;
import com.capsule.insurance.common.security.AuthenticatedUser;
import com.capsule.insurance.premiumcollection.application.PremiumDelinquencyService;
import com.capsule.insurance.premiumcollection.dto.DelinquencyRunResponse;
import com.capsule.insurance.premiumcollection.dto.StartDelinquencyRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ops/premium-collections/delinquency")
public class PremiumDelinquencyController {
    private final PremiumDelinquencyService service;
    public PremiumDelinquencyController(PremiumDelinquencyService service) { this.service = service; }

    @PostMapping("/runs")
    public ApiResponse<DelinquencyRunResponse> run(@Valid @RequestBody StartDelinquencyRequest request, Authentication auth) {
        return ApiResponse.success(service.run(request.instanceKey(), AuthenticatedUser.id(auth), request.reason()));
    }

    @PostMapping("/runs/{id}/resume")
    public ApiResponse<DelinquencyRunResponse> resume(@PathVariable long id,
            @Valid @RequestBody ResumeRequest request, Authentication auth) {
        return ApiResponse.success(service.resume(id, AuthenticatedUser.id(auth), request.reason()));
    }

    @GetMapping("/runs")
    public ApiResponse<List<DelinquencyRunResponse>> recent() { return ApiResponse.success(service.recent()); }

    @GetMapping("/runs/{id}")
    public ApiResponse<DelinquencyRunResponse> get(@PathVariable long id) { return ApiResponse.success(service.get(id)); }

    public record ResumeRequest(@NotBlank @Size(max = 500) String reason) { }
}

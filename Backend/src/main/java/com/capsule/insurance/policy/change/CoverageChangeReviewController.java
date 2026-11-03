package com.capsule.insurance.policy.change;

import com.capsule.insurance.common.response.ApiResponse;
import com.capsule.insurance.common.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ops/policy-changes")
public class CoverageChangeReviewController {
    private final CoverageChangeReviewService reviews;
    public CoverageChangeReviewController(CoverageChangeReviewService reviews){this.reviews=reviews;}
    @GetMapping
    public ApiResponse<List<CoverageChangeReviewService.RequestView>> pending(Authentication auth){
        return ApiResponse.success(reviews.pending(AuthenticatedUser.id(auth)));
    }
    @PostMapping("/{requestId}/decisions")
    public ApiResponse<CoverageChangeReviewService.RequestView> decide(@PathVariable Long requestId,
            @Valid @RequestBody DecisionRequest request,Authentication auth){
        return ApiResponse.success(reviews.decide(AuthenticatedUser.id(auth),requestId,request.decision(),request.reason()));
    }
    public record DecisionRequest(@NotNull CoverageChangeReviewService.Decision decision,@NotBlank @Size(max=500) String reason){}
}

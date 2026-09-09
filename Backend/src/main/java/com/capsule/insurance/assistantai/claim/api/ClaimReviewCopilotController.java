package com.capsule.insurance.assistantai.claim.api;

import com.capsule.insurance.assistantai.claim.application.ClaimAssessmentAssistantService;
import com.capsule.insurance.assistantai.claim.application.ClaimCopilotReviewService;
import com.capsule.insurance.assistantai.claim.domain.ClaimCopilotReview;
import com.capsule.insurance.assistantai.claim.domain.ClaimCopilotReviewEvent;
import com.capsule.insurance.assistantai.claim.domain.ClaimCopilotReviewStatus;
import com.capsule.insurance.common.response.ApiResponse;
import com.capsule.insurance.common.security.AuthenticatedUser;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/** 관리자 전용 보험금 심사 보조 PoC API. 청구·지급 상태를 변경하지 않습니다. */
@Validated
@RestController
@RequestMapping("/api/v1/ops/claims")
public class ClaimReviewCopilotController {
    private final ClaimAssessmentAssistantService service;
    private final ClaimCopilotReviewService reviewService;

    public ClaimReviewCopilotController(
            ClaimAssessmentAssistantService service,
            ClaimCopilotReviewService reviewService
    ) {
        this.service = service;
        this.reviewService = reviewService;
    }

    @PostMapping("/{claimId}/review-copilot/drafts")
    public ApiResponse<ClaimAssessmentAssistantService.AssistantResult> createDraft(
            @PathVariable Long claimId,
            @RequestBody DraftRequest request
    ) {
        return ApiResponse.success(service.createDraftForOperator(claimId, request.requestId(), request.instruction()));
    }

    @PostMapping("/{claimId}/review-copilot/drafts/{requestId}/review")
    public ApiResponse<ClaimCopilotReview> reviewDraft(
            @PathVariable Long claimId,
            @PathVariable String requestId,
            @RequestBody ReviewRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(reviewService.review(
                claimId, requestId, request.status(), AuthenticatedUser.id(authentication)));
    }

    @GetMapping("/{claimId}/review-copilot/drafts/{requestId}/review")
    public ApiResponse<ClaimCopilotReview> getReview(
            @PathVariable Long claimId,
            @PathVariable String requestId
    ) {
        return ApiResponse.success(reviewService.get(claimId, requestId));
    }

    @GetMapping("/{claimId}/review-copilot/drafts/{requestId}/review/history")
    public ApiResponse<List<ClaimCopilotReviewEvent>> getReviewHistory(
            @PathVariable Long claimId,
            @PathVariable String requestId
    ) {
        return ApiResponse.success(reviewService.history(claimId, requestId));
    }

    @GetMapping("/review-copilot/reviews")
    public ApiResponse<List<ClaimCopilotReview>> getRecentReviews(
            @RequestParam(required = false) ClaimCopilotReviewStatus status,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit
    ) {
        return ApiResponse.success(reviewService.recent(status, limit));
    }

    public record DraftRequest(@NotBlank String requestId, @NotBlank String instruction) {
    }

    public record ReviewRequest(@NotNull ClaimCopilotReviewStatus status) {
    }
}

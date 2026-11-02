package com.capsule.insurance.assistantai.claim.domain;

import java.time.Instant;

/** PII나 담당자 메모 원문 없이 보관하는 심사 보조 초안의 검토 상태입니다. */
public record ClaimCopilotReview(
        Long claimId,
        String requestId,
        ClaimCopilotReviewStatus status,
        Long reviewerUserId,
        Instant updatedAt
) {
}

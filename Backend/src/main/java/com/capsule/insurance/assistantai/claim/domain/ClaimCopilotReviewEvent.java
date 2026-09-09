package com.capsule.insurance.assistantai.claim.domain;

import java.time.Instant;

/** 고객 원문 없이 남기는 심사 보조 검토 상태 변경 이력입니다. */
public record ClaimCopilotReviewEvent(
        Long claimId,
        String requestId,
        ClaimCopilotReviewEventType eventType,
        ClaimCopilotReviewStatus status,
        Long reviewerUserId,
        Instant occurredAt
) {
}

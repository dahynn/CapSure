package com.capsule.insurance.assistantai.claim.domain;

/** 심사 보조 초안의 상태 이력을 설명하는 최소 감사 이벤트입니다. */
public enum ClaimCopilotReviewEventType {
    DRAFT_CREATED,
    REVIEW_CONFIRMED,
    REVIEW_REJECTED
}

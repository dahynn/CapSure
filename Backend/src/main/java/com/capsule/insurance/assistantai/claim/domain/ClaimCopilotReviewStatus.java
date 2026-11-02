package com.capsule.insurance.assistantai.claim.domain;

/** 담당자가 심사 보조 초안을 확인한 상태입니다. 보험금 지급·거절 결정 상태가 아닙니다. */
public enum ClaimCopilotReviewStatus {
    DRAFT,
    CONFIRMED,
    REJECTED
}

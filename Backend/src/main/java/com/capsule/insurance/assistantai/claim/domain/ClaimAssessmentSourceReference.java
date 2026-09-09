package com.capsule.insurance.assistantai.claim.domain;

/** 청구 심사 보조 초안이 인용할 수 있는, 해당 청구에 귀속된 최소 근거 식별자입니다. */
public record ClaimAssessmentSourceReference(
        String sourceType,
        String sourceId,
        String version,
        Long termsClauseId
) {
}

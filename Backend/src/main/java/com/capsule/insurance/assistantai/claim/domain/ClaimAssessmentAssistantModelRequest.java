package com.capsule.insurance.assistantai.claim.domain;

import java.util.List;

/** 개인정보 원문 없이 내부 모델에 전달할 수 있는 청구 심사 보조 입력입니다. */
public record ClaimAssessmentAssistantModelRequest(
        String requestId,
        List<ClaimAssessmentSourceReference> allowedSources,
        List<String> requiredEvidenceTypes,
        List<String> verifiedEvidenceTypes
) {
    public ClaimAssessmentAssistantModelRequest {
        allowedSources = List.copyOf(allowedSources);
        requiredEvidenceTypes = List.copyOf(requiredEvidenceTypes);
        verifiedEvidenceTypes = List.copyOf(verifiedEvidenceTypes);
    }
}

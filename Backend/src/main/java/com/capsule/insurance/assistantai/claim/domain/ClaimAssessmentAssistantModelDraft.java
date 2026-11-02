package com.capsule.insurance.assistantai.claim.domain;

import java.util.List;

/** 모델 또는 규칙 기반 보조기가 반환하는 초안입니다. 지급 승인·거절 결과는 담지 않습니다. */
public record ClaimAssessmentAssistantModelDraft(
        List<ClaimAssessmentSourceReference> termsToCheck,
        List<String> possibleMissingEvidence,
        List<String> additionalQuestions,
        boolean evidenceInsufficient
) {
    public ClaimAssessmentAssistantModelDraft {
        termsToCheck = List.copyOf(termsToCheck);
        possibleMissingEvidence = List.copyOf(possibleMissingEvidence);
        additionalQuestions = List.copyOf(additionalQuestions);
    }
}

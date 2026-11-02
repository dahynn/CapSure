package com.capsule.insurance.assistantai.claim.domain;

import java.util.List;

/** 담당자가 재확인할 수 있도록 원문 없이 보관하는 구조화된 심사 보조 초안입니다. */
public record ClaimCopilotReviewDraftSnapshot(
        String requestId,
        List<ClaimAssessmentSourceReference> termsToCheck,
        List<String> possibleMissingEvidence,
        List<String> additionalQuestions,
        boolean evidenceInsufficient
) {
    public ClaimCopilotReviewDraftSnapshot {
        termsToCheck = List.copyOf(termsToCheck);
        possibleMissingEvidence = List.copyOf(possibleMissingEvidence);
        additionalQuestions = List.copyOf(additionalQuestions);
    }
}

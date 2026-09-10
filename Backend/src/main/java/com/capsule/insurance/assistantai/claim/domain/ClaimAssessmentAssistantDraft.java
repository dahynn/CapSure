package com.capsule.insurance.assistantai.claim.domain;

import java.util.List;

/** 보험금 심사 보조 PoC의 결과입니다. 담당자 검토 없이는 보험금 결정을 확정할 수 없습니다. */
public record ClaimAssessmentAssistantDraft(
        String requestId,
        List<ClaimAssessmentSourceReference> termsToCheck,
        List<String> possibleMissingEvidence,
        List<String> additionalQuestions,
        boolean evidenceInsufficient,
        boolean manualReviewRequired
) {
    public ClaimAssessmentAssistantDraft {
        termsToCheck = List.copyOf(termsToCheck);
        possibleMissingEvidence = List.copyOf(possibleMissingEvidence);
        additionalQuestions = List.copyOf(additionalQuestions);
        if (!manualReviewRequired) {
            throw new IllegalArgumentException("보험금 심사 보조 초안은 담당자 검토가 필요합니다.");
        }
    }

    public boolean canFinalizeClaimDecision() {
        return false;
    }
}

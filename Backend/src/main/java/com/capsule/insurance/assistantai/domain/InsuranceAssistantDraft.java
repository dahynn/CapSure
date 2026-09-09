package com.capsule.insurance.assistantai.domain;

import java.util.List;

/**
 * 보험 업무 보조용 초안입니다. 이 객체는 보험 인수·지급·면책 판단을 확정할 수 없습니다.
 */
public record InsuranceAssistantDraft(
        String requestId,
        String content,
        List<String> evidenceIds,
        InsuranceAssistantDecision decision
) {
    public InsuranceAssistantDraft {
        evidenceIds = List.copyOf(evidenceIds);
        if (decision != InsuranceAssistantDecision.REVIEW_REQUIRED) {
            throw new IllegalArgumentException("보험 업무 초안은 담당자 검토 대기 상태여야 합니다.");
        }
    }

    public boolean canFinalizeInsuranceDecision() {
        return false;
    }
}

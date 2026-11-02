package com.capsule.insurance.assistantai.claim.infra;

import com.capsule.insurance.assistantai.application.ExternalModelCallBlockedException;
import com.capsule.insurance.assistantai.claim.application.port.ClaimAssessmentAssistantGateway;
import com.capsule.insurance.assistantai.claim.domain.ClaimAssessmentAssistantModelDraft;
import com.capsule.insurance.assistantai.claim.domain.ClaimAssessmentAssistantModelRequest;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/** 내부망 연결·공급자 승인이 검증되기 전까지 청구 심사 보조 모델 egress를 차단합니다. */
@Component
@ConditionalOnProperty(name = "copilot.claim-review.provider", havingValue = "blocked", matchIfMissing = true)
public class DefaultBlockedClaimAssessmentAssistantGateway implements ClaimAssessmentAssistantGateway {
    @Override
    public ClaimAssessmentAssistantModelDraft generate(ClaimAssessmentAssistantModelRequest request) {
        throw new ExternalModelCallBlockedException();
    }
}

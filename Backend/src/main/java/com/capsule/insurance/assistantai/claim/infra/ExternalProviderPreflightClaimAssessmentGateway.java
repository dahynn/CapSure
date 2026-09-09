package com.capsule.insurance.assistantai.claim.infra;

import com.capsule.insurance.assistantai.application.ExternalModelCallBlockedException;
import com.capsule.insurance.assistantai.claim.application.port.ClaimAssessmentAssistantGateway;
import com.capsule.insurance.assistantai.claim.domain.ClaimAssessmentAssistantModelDraft;
import com.capsule.insurance.assistantai.claim.domain.ClaimAssessmentAssistantModelRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * API 키·모델 설정을 검증하는 동안의 fail-closed gateway입니다.
 * 실제 공급자 HTTP 어댑터가 승인되기 전에는 어떤 네트워크 요청도 수행하지 않습니다.
 */
@Component
@ConditionalOnProperty(name = "copilot.claim-review.provider", havingValue = "external")
public class ExternalProviderPreflightClaimAssessmentGateway implements ClaimAssessmentAssistantGateway {
    @Override
    public ClaimAssessmentAssistantModelDraft generate(ClaimAssessmentAssistantModelRequest request) {
        throw new ExternalModelCallBlockedException();
    }
}

package com.capsule.insurance.assistantai.claim.application.port;

import com.capsule.insurance.assistantai.claim.domain.ClaimAssessmentAssistantModelDraft;
import com.capsule.insurance.assistantai.claim.domain.ClaimAssessmentAssistantModelRequest;

/** 향후 내부망 모델 또는 별도 승인된 공급자로 교체할 청구 심사 보조 전용 경계입니다. */
public interface ClaimAssessmentAssistantGateway {
    ClaimAssessmentAssistantModelDraft generate(ClaimAssessmentAssistantModelRequest request);
}

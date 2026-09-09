package com.capsule.insurance.assistantai.application.port;

import com.capsule.insurance.assistantai.domain.InsuranceAssistantRequest;

/**
 * 승인된 내부망 모델 또는 별도 승인된 공급자로 교체할 확장 지점입니다.
 * 기본 구현은 어떤 외부 호출도 허용하지 않습니다.
 */
public interface InsuranceAssistantGateway {

    String generateDraft(InsuranceAssistantRequest request);
}

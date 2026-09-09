package com.capsule.insurance.assistantai.infra;

import com.capsule.insurance.assistantai.application.ExternalModelCallBlockedException;
import com.capsule.insurance.assistantai.application.port.InsuranceAssistantGateway;
import com.capsule.insurance.assistantai.domain.InsuranceAssistantRequest;
import org.springframework.stereotype.Component;

/** 운영 허가와 내부망 연결이 검증되기 전까지 모든 모델 전송을 차단합니다. */
@Component
public class DefaultBlockedInsuranceAssistantGateway implements InsuranceAssistantGateway {
    @Override
    public String generateDraft(InsuranceAssistantRequest request) {
        throw new ExternalModelCallBlockedException();
    }
}

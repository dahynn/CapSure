package com.capsule.insurance.assistantai.claim.infra;

import com.capsule.insurance.assistantai.claim.application.port.ClaimAssessmentAssistantGateway;
import com.capsule.insurance.assistantai.claim.domain.ClaimAssessmentAssistantModelDraft;
import com.capsule.insurance.assistantai.claim.domain.ClaimAssessmentAssistantModelRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** 외부 전송 없는 결정적 demo provider입니다. 실제 LLM이 아닙니다. */
@Component
@ConditionalOnProperty(name = "copilot.claim-review.provider", havingValue = "demo")
public class DemoClaimAssessmentAssistantGateway implements ClaimAssessmentAssistantGateway {
    @Override
    public ClaimAssessmentAssistantModelDraft generate(ClaimAssessmentAssistantModelRequest request) {
        return new ClaimAssessmentAssistantModelDraft(
                request.allowedSources().stream().filter(source -> "TERMS_CLAUSE".equals(source.sourceType())).toList(),
                java.util.List.of(),
                java.util.List.of("약관 조항과 제출 증빙의 일치 여부를 담당자가 확인해 주세요."),
                false
        );
    }
}

package com.capsule.insurance.assistantai.application;

import com.capsule.insurance.assistantai.domain.InsuranceAssistantDecision;
import java.time.Instant;
import java.util.List;

/** 개인정보·프롬프트 원문 없이 남기는 최소 감사 정보입니다. */
public record InsuranceAssistantAuditRecord(
        String requestId,
        InsuranceAssistantDecision decision,
        List<String> evidenceIds,
        Instant recordedAt
) {
    public InsuranceAssistantAuditRecord {
        evidenceIds = List.copyOf(evidenceIds);
    }
}

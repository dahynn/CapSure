package com.capsule.insurance.assistantai.domain;

import java.util.Objects;

public record InsuranceAssistantEvidence(
        String evidenceId,
        InsuranceEvidenceType type,
        String content
) {
    public InsuranceAssistantEvidence {
        Objects.requireNonNull(evidenceId, "evidenceId must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(content, "content must not be null");
    }
}

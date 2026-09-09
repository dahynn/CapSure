package com.capsule.insurance.assistantai.domain;

import java.util.List;
import java.util.Objects;

public record InsuranceAssistantRequest(
        String requestId,
        String instruction,
        List<InsuranceAssistantEvidence> evidence
) {
    public InsuranceAssistantRequest {
        Objects.requireNonNull(requestId, "requestId must not be null");
        Objects.requireNonNull(instruction, "instruction must not be null");
        evidence = List.copyOf(Objects.requireNonNull(evidence, "evidence must not be null"));
    }
}

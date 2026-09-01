package com.capsule.insurance.application.domain;

import java.time.Instant;
import java.util.List;

public record UnderwritingDecision(
        Long underwritingDecisionId,
        Long applicationId,
        int decisionVersion,
        String decision,
        String ruleVersion,
        List<String> reasonCodes,
        String inputHash,
        Instant decidedAt
) {

    public UnderwritingDecision {
        reasonCodes = List.copyOf(reasonCodes);
    }
}

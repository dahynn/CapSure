package com.capsule.insurance.claim.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ClaimDecision(
        Long claimDecisionId,
        Long claimId,
        int decisionVersion,
        String result,
        BigDecimal benefitAmount,
        String currencyCode,
        List<String> reasonCodes,
        Long termsClauseId,
        String ruleVersion,
        String inputHash,
        String actorType,
        Instant decidedAt
) {
}

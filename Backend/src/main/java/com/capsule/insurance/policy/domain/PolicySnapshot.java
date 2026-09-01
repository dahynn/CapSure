package com.capsule.insurance.policy.domain;

import com.capsule.insurance.quote.domain.QuoteSnapshot;
import java.util.List;

public record PolicySnapshot(
        QuoteSnapshot quote,
        List<ClaimRuleSnapshot> claimRules
) {

    public PolicySnapshot {
        claimRules = List.copyOf(claimRules);
    }

    public record ClaimRuleSnapshot(
            Long productCoverageId,
            String ruleVersion,
            List<String> diagnosisCategories,
            List<String> requiredEvidence,
            boolean firstDiagnosisOnly,
            Long eligibilityClauseId,
            Long missingEvidenceClauseId,
            Long denialClauseId
    ) {
        public ClaimRuleSnapshot {
            diagnosisCategories = List.copyOf(diagnosisCategories);
            requiredEvidence = List.copyOf(requiredEvidence);
        }
    }
}

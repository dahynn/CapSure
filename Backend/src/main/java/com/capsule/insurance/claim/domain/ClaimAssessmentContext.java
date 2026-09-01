package com.capsule.insurance.claim.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ClaimAssessmentContext(
        InsuranceClaim claim,
        Long policyVersionId,
        String policyStatus,
        Long productCoverageId,
        String coverageCode,
        BigDecimal insuredAmount,
        String currencyCode,
        Instant coverageStartAt,
        Instant coverageEndAt,
        int paidBenefitCount,
        int reductionPeriodDays,
        BigDecimal reductionRate,
        String termsHash,
        String ruleVersion,
        List<String> diagnosisCategories,
        List<String> requiredEvidence,
        boolean firstDiagnosisOnly,
        Long eligibilityClauseId,
        Long missingEvidenceClauseId,
        Long denialClauseId
) {
}

package com.capsule.insurance.claim.dto;

import com.capsule.insurance.claim.domain.ClaimDecision;
import com.capsule.insurance.claim.domain.ClaimEvidence;
import com.capsule.insurance.claim.domain.ClaimPayment;
import java.time.Instant;
import java.util.List;

public record ClaimResponse(
        Long claimId,
        String claimNo,
        Long policyId,
        Long policyCoverageId,
        Instant incidentAt,
        String diagnosisCategory,
        String status,
        List<ClaimEvidence> evidence,
        ClaimDecision decision,
        ClaimPayment payment,
        Instant submittedAt,
        Instant createdAt,
        Instant updatedAt
) {
}

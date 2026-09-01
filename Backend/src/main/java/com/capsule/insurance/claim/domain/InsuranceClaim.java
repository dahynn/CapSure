package com.capsule.insurance.claim.domain;

import java.time.Instant;

public record InsuranceClaim(
        Long claimId,
        String claimNo,
        Long policyId,
        Long policyCoverageId,
        Long claimantUserId,
        Instant incidentAt,
        String diagnosisCategory,
        String eventFingerprint,
        String status,
        String submissionIdempotencyKey,
        Instant submittedAt,
        Instant createdAt,
        Instant updatedAt
) {
}

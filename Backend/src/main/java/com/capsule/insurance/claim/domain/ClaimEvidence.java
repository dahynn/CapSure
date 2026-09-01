package com.capsule.insurance.claim.domain;

import java.time.Instant;
import java.util.Map;

public record ClaimEvidence(
        Long claimEvidenceId,
        Long claimId,
        String evidenceType,
        String syntheticReference,
        String checksum,
        Map<String, Object> metadata,
        boolean verified,
        Instant createdAt
) {
}

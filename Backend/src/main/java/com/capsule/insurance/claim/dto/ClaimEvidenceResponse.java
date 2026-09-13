package com.capsule.insurance.claim.dto;

import java.time.Instant;
import java.util.Map;

public record ClaimEvidenceResponse(
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

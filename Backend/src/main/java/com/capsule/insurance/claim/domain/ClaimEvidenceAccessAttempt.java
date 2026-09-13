package com.capsule.insurance.claim.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** 개인정보 원문 없이 청구 상세 조회의 주체·대상·결과만 전달한다. */
public record ClaimEvidenceAccessAttempt(
        UUID accessRequestId,
        Long actorUserId,
        Long claimId,
        List<Long> evidenceIds,
        AccessResult result,
        Instant occurredAt
) {

    public ClaimEvidenceAccessAttempt {
        evidenceIds = List.copyOf(evidenceIds);
    }

    public static ClaimEvidenceAccessAttempt allowed(
            UUID accessRequestId,
            Long actorUserId,
            Long claimId,
            List<Long> evidenceIds,
            Instant occurredAt
    ) {
        return new ClaimEvidenceAccessAttempt(
                accessRequestId,
                actorUserId,
                claimId,
                evidenceIds,
                AccessResult.ALLOWED,
                occurredAt
        );
    }

    public static ClaimEvidenceAccessAttempt deniedNotFound(
            UUID accessRequestId,
            Long actorUserId,
            Long claimId,
            Instant occurredAt
    ) {
        return new ClaimEvidenceAccessAttempt(
                accessRequestId,
                actorUserId,
                claimId,
                List.of(),
                AccessResult.DENIED_NOT_FOUND,
                occurredAt
        );
    }

    public enum AccessResult {
        ALLOWED,
        DENIED_NOT_FOUND
    }
}

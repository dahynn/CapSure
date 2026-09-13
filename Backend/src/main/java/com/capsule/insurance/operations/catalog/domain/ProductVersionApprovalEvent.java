package com.capsule.insurance.operations.catalog.domain;

import java.time.Instant;

public record ProductVersionApprovalEvent(
        Long approvalEventId,
        Long productVersionId,
        String productCode,
        String productVersion,
        Long termsDocumentId,
        String termsVersion,
        String decision,
        String previousStatus,
        String resultingStatus,
        Long actorUserId,
        String reason,
        Instant decidedAt
) {
}


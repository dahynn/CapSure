package com.capsule.insurance.operations.catalog.dto;

import com.capsule.insurance.operations.catalog.domain.ProductVersionApprovalEvent;
import java.time.Instant;

public record ProductVersionApprovalEventResponse(
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
    public static ProductVersionApprovalEventResponse from(ProductVersionApprovalEvent event) {
        return new ProductVersionApprovalEventResponse(
                event.approvalEventId(),
                event.productVersionId(),
                event.productCode(),
                event.productVersion(),
                event.termsDocumentId(),
                event.termsVersion(),
                event.decision(),
                event.previousStatus(),
                event.resultingStatus(),
                event.actorUserId(),
                event.reason(),
                event.decidedAt()
        );
    }
}


package com.capsule.insurance.operations.outbox.dto;

public record OutboxSummaryResponse(
        long pendingCount,
        long processingCount,
        long publishedCount,
        long failedCount,
        long deadLetterCount,
        long projectedAuditCount
) {
}

package com.capsule.insurance.operations.dashboard.domain;

import java.time.Instant;
import java.util.List;

public record OperationsDashboardSnapshot(
        OutboxMetrics outbox,
        ReconciliationMetrics reconciliation,
        List<JobExecutionItem> recentJobs,
        List<DeadLetterItem> deadLetters,
        List<ReconciliationItem> recentReconciliations
) {

    public record OutboxMetrics(
            long pendingCount,
            long processingCount,
            long publishedCount,
            long failedCount,
            long pendingDeadLetterCount,
            long projectedAuditCount,
            Instant oldestUnpublishedAt
    ) {
    }

    public record ReconciliationMetrics(
            long waitingOrderCount,
            long dueOrderCount,
            long lockedOrderCount,
            long totalExecutionCount,
            long runningExecutionCount,
            long failedLatestExecutionCount,
            long processedCount,
            long resolvedCount,
            long stillUnknownCount,
            long failedCount
    ) {
    }

    public record JobExecutionItem(
            long jobExecutionId,
            String jobName,
            String instanceKey,
            int executionNo,
            String status,
            long inputCount,
            long acceptedCount,
            long duplicateCount,
            long quarantinedCount,
            long processedCount,
            long resolvedCount,
            long stillUnknownCount,
            long failedCount,
            Instant startedAt,
            Instant finishedAt,
            String errorReason
    ) {
    }

    public record DeadLetterItem(
            long deadLetterId,
            String eventId,
            String replayStatus,
            String errorReason,
            String replayReason,
            Instant createdAt,
            Instant replayedAt
    ) {
    }

    public record ReconciliationItem(
            long reconciliationId,
            String targetType,
            String targetId,
            String provider,
            String localStatus,
            String providerStatus,
            String result,
            Instant executedAt
    ) {
    }
}

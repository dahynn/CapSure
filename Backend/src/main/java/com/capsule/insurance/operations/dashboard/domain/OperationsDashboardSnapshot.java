package com.capsule.insurance.operations.dashboard.domain;

import java.time.Instant;
import java.util.List;

public record OperationsDashboardSnapshot(
        OutboxMetrics outbox,
        ReconciliationMetrics reconciliation,
        RecoveryMetrics recovery,
        PaymentInterfaceMetrics paymentInterface,
        List<JobExecutionItem> recentJobs,
        List<DeadLetterItem> deadLetters,
        List<ReconciliationItem> recentReconciliations,
        List<RecoveryActionItem> recentRecoveryActions,
        List<PaymentInterfaceMessageItem> recentPaymentInterfaceMessages
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

    public record RecoveryMetrics(
            long totalActionCount,
            long succeededActionCount,
            long failedActionCount,
            long averageRecoveryTimeMs,
            long latestRecoveryTimeMs
    ) {
    }

    public record PaymentInterfaceMetrics(
            long totalMessageCount,
            long succeededResponseCount,
            long timeoutResponseCount,
            long circuitOpenResponseCount,
            Instant latestMessageAt
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

    public record RecoveryActionItem(
            long recoveryActionId,
            String actionType,
            String targetType,
            String targetId,
            long actorUserId,
            String actorName,
            String reason,
            String status,
            Instant detectedAt,
            Instant startedAt,
            Instant completedAt,
            Long actionDurationMs,
            Long recoveryTimeMs,
            String errorReason
    ) {
    }

    public record PaymentInterfaceMessageItem(
            long financialMessageId,
            String interfaceName,
            String messageType,
            String direction,
            String correlationId,
            String idempotencyKey,
            String businessKey,
            String status,
            String errorCode,
            Instant occurredAt
    ) {
    }
}

package com.capsule.insurance.operations.reconciliation.dto;

import java.time.Instant;

public record PaymentReconciliationExecutionResponse(
        Long jobExecutionId,
        String jobName,
        String instanceKey,
        int executionNo,
        String status,
        Instant cutoffAt,
        long lastPaymentOrderId,
        int processedChunks,
        String workerId,
        long processedCount,
        long resolvedCount,
        long stillUnknownCount,
        long failedCount,
        boolean controlTotalMatched,
        Instant startedAt,
        Instant finishedAt,
        String errorReason
) {
}

package com.capsule.insurance.operations.recovery.domain;

import java.time.Instant;

public record OperationsRecoveryAction(
        long recoveryActionId,
        String actionType,
        String targetType,
        String targetId,
        Long actorUserId,
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

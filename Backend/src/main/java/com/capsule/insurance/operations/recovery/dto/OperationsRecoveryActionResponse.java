package com.capsule.insurance.operations.recovery.dto;

import com.capsule.insurance.operations.recovery.domain.OperationsRecoveryAction;
import java.time.Instant;

public record OperationsRecoveryActionResponse(
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

    public static OperationsRecoveryActionResponse from(OperationsRecoveryAction action) {
        return new OperationsRecoveryActionResponse(
                action.recoveryActionId(),
                action.actionType(),
                action.targetType(),
                action.targetId(),
                action.actorUserId(),
                action.reason(),
                action.status(),
                action.detectedAt(),
                action.startedAt(),
                action.completedAt(),
                action.actionDurationMs(),
                action.recoveryTimeMs(),
                action.errorReason()
        );
    }
}

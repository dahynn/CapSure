package com.capsule.insurance.operations.recovery.application.port;

import com.capsule.insurance.operations.recovery.domain.OperationsRecoveryAction;
import java.time.Instant;
import java.util.Optional;

public interface OperationsRecoveryRepository {

    Optional<OperationsRecoveryAction> startDlqReplay(
            String eventId,
            Long actorUserId,
            String reason,
            Instant startedAt
    );

    OperationsRecoveryAction startPaymentReconciliation(
            Long actorUserId,
            String reason,
            Instant startedAt
    );

    OperationsRecoveryAction succeed(
            long recoveryActionId,
            String targetId,
            String resultJson,
            Instant completedAt
    );

    OperationsRecoveryAction fail(
            long recoveryActionId,
            String targetId,
            String errorReason,
            Instant completedAt
    );
}

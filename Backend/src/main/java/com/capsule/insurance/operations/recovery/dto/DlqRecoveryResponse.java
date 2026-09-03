package com.capsule.insurance.operations.recovery.dto;

import com.capsule.insurance.operations.outbox.dto.OutboxReplayResponse;

public record DlqRecoveryResponse(
        OperationsRecoveryActionResponse recovery,
        OutboxReplayResponse replay
) {
}

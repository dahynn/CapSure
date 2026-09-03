package com.capsule.insurance.operations.recovery.dto;

import com.capsule.insurance.operations.outbox.dto.OutboxReplayResponse;
import com.capsule.insurance.operations.outbox.dto.OutboxRelayRunResponse;

public record DlqRecoveryResponse(
        OperationsRecoveryActionResponse recovery,
        OutboxReplayResponse replay,
        OutboxRelayRunResponse relay
) {
}

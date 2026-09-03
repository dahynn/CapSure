package com.capsule.insurance.operations.outbox.dto;

import java.time.Instant;

public record OutboxReplayResponse(
        String eventId,
        String outboxStatus,
        String replayStatus,
        int attemptCount,
        Long replayActorUserId,
        String replayReason,
        Instant availableAt,
        Instant replayedAt
) {
}

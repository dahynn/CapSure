package com.capsule.insurance.operations.outbox.domain;

public record OutboxReplayTarget(
        Long outboxEventId,
        String eventId,
        String outboxStatus,
        int attemptCount,
        Long deadLetterId,
        String replayStatus
) {
}

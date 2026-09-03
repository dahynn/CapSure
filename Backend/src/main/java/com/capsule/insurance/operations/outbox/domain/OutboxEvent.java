package com.capsule.insurance.operations.outbox.domain;

import java.time.Instant;

public record OutboxEvent(
        Long outboxEventId,
        String eventId,
        String aggregateType,
        String aggregateId,
        String eventType,
        String payloadJson,
        String status,
        int attemptCount,
        Instant availableAt,
        Instant lockedAt,
        String lockedBy,
        Instant createdAt
) {
}

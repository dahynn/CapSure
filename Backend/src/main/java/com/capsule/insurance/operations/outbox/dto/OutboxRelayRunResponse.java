package com.capsule.insurance.operations.outbox.dto;

import java.time.Instant;

public record OutboxRelayRunResponse(
        String workerId,
        int claimedCount,
        int publishedCount,
        int retryScheduledCount,
        int deadLetterCount,
        Instant startedAt,
        Instant finishedAt
) {
}

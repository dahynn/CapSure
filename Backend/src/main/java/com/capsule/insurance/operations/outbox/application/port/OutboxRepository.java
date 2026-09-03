package com.capsule.insurance.operations.outbox.application.port;

import com.capsule.insurance.operations.outbox.domain.OutboxEvent;
import com.capsule.insurance.operations.outbox.domain.OutboxReplayTarget;
import com.capsule.insurance.operations.outbox.dto.OutboxReplayResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface OutboxRepository {

    List<OutboxEvent> claimAvailable(
            String workerId,
            int batchSize,
            Instant now,
            Instant staleLockBefore
    );

    void markPublished(Long outboxEventId, String workerId);

    void scheduleRetry(
            Long outboxEventId,
            String workerId,
            Instant availableAt,
            String errorReason
    );

    void moveToDeadLetter(OutboxEvent event, String workerId, String errorReason);

    Optional<OutboxReplayTarget> lockReplayTarget(String eventId);

    OutboxReplayResponse replay(
            OutboxReplayTarget target,
            Long actorUserId,
            String reason,
            Instant replayedAt
    );

    Map<String, Long> countByStatus();

    long countProjectedAuditEvents();
}

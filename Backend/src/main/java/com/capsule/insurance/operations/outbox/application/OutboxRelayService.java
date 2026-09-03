package com.capsule.insurance.operations.outbox.application;

import com.capsule.insurance.common.exception.BusinessException;
import com.capsule.insurance.common.exception.ErrorCode;
import com.capsule.insurance.operations.outbox.application.port.OutboxEventHandler;
import com.capsule.insurance.operations.outbox.application.port.OutboxRepository;
import com.capsule.insurance.operations.outbox.domain.OutboxEvent;
import com.capsule.insurance.operations.outbox.domain.OutboxReplayTarget;
import com.capsule.insurance.operations.outbox.dto.OutboxRelayRunResponse;
import com.capsule.insurance.operations.outbox.dto.OutboxReplayResponse;
import com.capsule.insurance.operations.outbox.dto.OutboxSummaryResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Service
public class OutboxRelayService {

    static final int MAX_ATTEMPTS = 3;
    private static final int MAX_BATCH_SIZE = 500;
    private static final Duration LOCK_LEASE = Duration.ofMinutes(1);
    private static final Duration BASE_RETRY_DELAY = Duration.ofSeconds(1);

    private final OutboxRepository repository;
    private final List<OutboxEventHandler> handlers;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    @Autowired
    public OutboxRelayService(
            OutboxRepository repository,
            List<OutboxEventHandler> handlers,
            PlatformTransactionManager transactionManager
    ) {
        this(repository, handlers, transactionManager, Clock.systemUTC());
    }

    public OutboxRelayService(
            OutboxRepository repository,
            List<OutboxEventHandler> handlers,
            PlatformTransactionManager transactionManager,
            Clock clock
    ) {
        this.repository = repository;
        this.handlers = List.copyOf(handlers);
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    public OutboxRelayRunResponse relay(int batchSize) {
        validateBatchSize(batchSize);
        Instant startedAt = Instant.now(clock);
        String workerId = "OUTBOX-" + UUID.randomUUID();
        List<OutboxEvent> claimed = Objects.requireNonNull(transactionTemplate.execute(status ->
                repository.claimAvailable(
                        workerId,
                        batchSize,
                        startedAt,
                        startedAt.minus(LOCK_LEASE)
                )
        ));

        return processClaimed(workerId, claimed, startedAt);
    }

    public OutboxRelayRunResponse relayEvent(String eventId) {
        if (!StringUtils.hasText(eventId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "eventId가 필요합니다.");
        }
        validateBatchSize(1);
        Instant startedAt = Instant.now(clock);
        String workerId = "OUTBOX-RECOVERY-" + UUID.randomUUID();
        List<OutboxEvent> claimed = Objects.requireNonNull(transactionTemplate.execute(status ->
                repository.claimAvailableByEventId(
                        workerId,
                        eventId,
                        startedAt,
                        startedAt.minus(LOCK_LEASE)
                )
        ));
        if (claimed.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.INVALID_STATE_TRANSITION,
                    "재발행할 수 있는 Outbox 이벤트가 없습니다."
            );
        }
        return processClaimed(workerId, claimed, startedAt);
    }

    private OutboxRelayRunResponse processClaimed(
            String workerId,
            List<OutboxEvent> claimed,
            Instant startedAt
    ) {

        int published = 0;
        int retries = 0;
        int deadLetters = 0;
        for (OutboxEvent event : claimed) {
            try {
                transactionTemplate.executeWithoutResult(status -> {
                    for (OutboxEventHandler handler : handlers) {
                        handler.handle(event);
                    }
                    repository.markPublished(event.outboxEventId(), workerId);
                });
                published++;
            } catch (RuntimeException exception) {
                String errorReason = compactError(exception);
                boolean nonRetryable = hasCause(exception, NonRetryableOutboxException.class);
                if (nonRetryable || event.attemptCount() >= MAX_ATTEMPTS) {
                    transactionTemplate.executeWithoutResult(status ->
                            repository.moveToDeadLetter(event, workerId, errorReason)
                    );
                    deadLetters++;
                } else {
                    Instant availableAt = Instant.now(clock).plus(retryDelay(event.attemptCount()));
                    transactionTemplate.executeWithoutResult(status ->
                            repository.scheduleRetry(
                                    event.outboxEventId(),
                                    workerId,
                                    availableAt,
                                    errorReason
                            )
                    );
                    retries++;
                }
            }
        }

        return new OutboxRelayRunResponse(
                workerId,
                claimed.size(),
                published,
                retries,
                deadLetters,
                startedAt,
                Instant.now(clock)
        );
    }

    public OutboxReplayResponse replay(String eventId, Long actorUserId, String reason) {
        if (!StringUtils.hasText(eventId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "eventId가 필요합니다.");
        }
        if (!StringUtils.hasText(reason) || reason.length() > 500) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "재처리 사유는 1자 이상 500자 이하여야 합니다.");
        }
        return Objects.requireNonNull(transactionTemplate.execute(status -> {
            OutboxReplayTarget target = repository.lockReplayTarget(eventId)
                    .orElseThrow(() -> new BusinessException(
                            ErrorCode.RESOURCE_NOT_FOUND,
                            "재처리할 Outbox 이벤트를 찾을 수 없습니다."
                    ));
            if (!"DEAD_LETTER".equals(target.outboxStatus())
                    || target.deadLetterId() == null
                    || !"PENDING".equals(target.replayStatus())) {
                throw new BusinessException(
                        ErrorCode.INVALID_STATE_TRANSITION,
                        "대기 중인 DLQ 이벤트만 재처리할 수 있습니다."
                );
            }
            return repository.replay(target, actorUserId, reason, Instant.now(clock));
        }));
    }

    public OutboxSummaryResponse summary() {
        Map<String, Long> counts = repository.countByStatus();
        return new OutboxSummaryResponse(
                counts.getOrDefault("PENDING", 0L),
                counts.getOrDefault("PROCESSING", 0L),
                counts.getOrDefault("PUBLISHED", 0L),
                counts.getOrDefault("FAILED", 0L),
                counts.getOrDefault("DEAD_LETTER", 0L),
                repository.countProjectedAuditEvents()
        );
    }

    private void validateBatchSize(int batchSize) {
        if (batchSize < 1 || batchSize > MAX_BATCH_SIZE) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "batchSize는 1 이상 " + MAX_BATCH_SIZE + " 이하여야 합니다."
            );
        }
        if (handlers.isEmpty()) {
            throw new IllegalStateException("Outbox 이벤트 처리기가 없습니다.");
        }
    }

    private Duration retryDelay(int attemptCount) {
        int exponent = Math.max(0, Math.min(attemptCount - 1, 5));
        return BASE_RETRY_DELAY.multipliedBy(1L << exponent);
    }

    private String compactError(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        String message = root.getClass().getSimpleName() + ": " + root.getMessage();
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }

    private boolean hasCause(Throwable throwable, Class<? extends Throwable> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}

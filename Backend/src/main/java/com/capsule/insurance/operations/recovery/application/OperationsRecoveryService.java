package com.capsule.insurance.operations.recovery.application;

import com.capsule.insurance.common.exception.BusinessException;
import com.capsule.insurance.common.exception.ErrorCode;
import com.capsule.insurance.operations.outbox.application.OutboxRelayService;
import com.capsule.insurance.operations.outbox.dto.OutboxReplayResponse;
import com.capsule.insurance.operations.reconciliation.application.PaymentReconciliationBatchService;
import com.capsule.insurance.operations.reconciliation.domain.PaymentReconciliationRunOptions;
import com.capsule.insurance.operations.reconciliation.dto.PaymentReconciliationExecutionResponse;
import com.capsule.insurance.operations.recovery.application.port.OperationsRecoveryRepository;
import com.capsule.insurance.operations.recovery.domain.OperationsRecoveryAction;
import com.capsule.insurance.operations.recovery.dto.DlqRecoveryResponse;
import com.capsule.insurance.operations.recovery.dto.OperationsRecoveryActionResponse;
import com.capsule.insurance.operations.recovery.dto.PaymentReconciliationRecoveryResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OperationsRecoveryService {

    private final OperationsRecoveryRepository repository;
    private final OutboxRelayService outboxRelayService;
    private final PaymentReconciliationBatchService reconciliationBatchService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public OperationsRecoveryService(
            OperationsRecoveryRepository repository,
            OutboxRelayService outboxRelayService,
            PaymentReconciliationBatchService reconciliationBatchService,
            ObjectMapper objectMapper
    ) {
        this(
                repository,
                outboxRelayService,
                reconciliationBatchService,
                objectMapper,
                Clock.systemUTC()
        );
    }

    OperationsRecoveryService(
            OperationsRecoveryRepository repository,
            OutboxRelayService outboxRelayService,
            PaymentReconciliationBatchService reconciliationBatchService,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.repository = repository;
        this.outboxRelayService = outboxRelayService;
        this.reconciliationBatchService = reconciliationBatchService;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public DlqRecoveryResponse replayDlq(String eventId, Long actorUserId, String reason) {
        validateReason(reason);
        Instant startedAt = Instant.now(clock);
        OperationsRecoveryAction action = repository.startDlqReplay(
                eventId,
                actorUserId,
                reason.trim(),
                startedAt
        ).orElseThrow(() -> new BusinessException(
                ErrorCode.INVALID_STATE_TRANSITION,
                "대기 중인 DLQ 이벤트만 재처리할 수 있습니다."
        ));

        try {
            OutboxReplayResponse replay = outboxRelayService.replay(
                    eventId,
                    actorUserId,
                    reason.trim()
            );
            OperationsRecoveryAction completed = repository.succeed(
                    action.recoveryActionId(),
                    eventId,
                    toJson(Map.of(
                            "outboxStatus", replay.outboxStatus(),
                            "replayStatus", replay.replayStatus(),
                            "attemptCount", replay.attemptCount()
                    )),
                    Instant.now(clock)
            );
            return new DlqRecoveryResponse(
                    OperationsRecoveryActionResponse.from(completed),
                    replay
            );
        } catch (RuntimeException exception) {
            repository.fail(
                    action.recoveryActionId(),
                    eventId,
                    compactError(exception),
                    Instant.now(clock)
            );
            throw exception;
        }
    }

    public PaymentReconciliationRecoveryResponse runPaymentReconciliation(
            Long actorUserId,
            String reason,
            int chunkSize,
            long staleAfterSeconds
    ) {
        validateReason(reason);
        if (chunkSize < 1 || chunkSize > 500) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "chunkSize는 1 이상 500 이하여야 합니다.");
        }
        if (staleAfterSeconds < 0 || staleAfterSeconds > 604800) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "staleAfterSeconds는 0 이상 604800 이하여야 합니다."
            );
        }

        OperationsRecoveryAction action = repository.startPaymentReconciliation(
                actorUserId,
                reason.trim(),
                Instant.now(clock)
        );
        String instanceKey = "MANUAL-RECOVERY-" + action.recoveryActionId();

        try {
            PaymentReconciliationExecutionResponse execution = reconciliationBatchService.run(
                    instanceKey,
                    PaymentReconciliationRunOptions.production(
                            chunkSize,
                            Duration.ofSeconds(staleAfterSeconds)
                    )
            );
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("jobExecutionId", execution.jobExecutionId());
            result.put("status", execution.status());
            result.put("processedCount", execution.processedCount());
            result.put("resolvedCount", execution.resolvedCount());
            result.put("stillUnknownCount", execution.stillUnknownCount());
            result.put("failedCount", execution.failedCount());
            result.put("controlTotalMatched", execution.controlTotalMatched());
            OperationsRecoveryAction completed = repository.succeed(
                    action.recoveryActionId(),
                    execution.jobExecutionId().toString(),
                    toJson(result),
                    Instant.now(clock)
            );
            return new PaymentReconciliationRecoveryResponse(
                    OperationsRecoveryActionResponse.from(completed),
                    execution
            );
        } catch (RuntimeException exception) {
            repository.fail(
                    action.recoveryActionId(),
                    null,
                    compactError(exception),
                    Instant.now(clock)
            );
            throw exception;
        }
    }

    private void validateReason(String reason) {
        if (!StringUtils.hasText(reason) || reason.trim().length() > 500) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "운영 조치 사유는 1자 이상 500자 이하여야 합니다."
            );
        }
    }

    private String toJson(Map<String, Object> result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("운영 복구 결과를 기록할 수 없습니다.", exception);
        }
    }

    private String compactError(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        String message = root.getClass().getSimpleName() + ": " + root.getMessage();
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}

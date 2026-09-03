package com.capsule.insurance.operations.reconciliation.application;

import com.capsule.insurance.common.exception.BusinessException;
import com.capsule.insurance.common.exception.ErrorCode;
import com.capsule.insurance.operations.reconciliation.application.port.PaymentReconciliationJobRepository;
import com.capsule.insurance.operations.reconciliation.domain.PaymentReconciliationExecution;
import com.capsule.insurance.operations.reconciliation.domain.PaymentReconciliationInterruptedException;
import com.capsule.insurance.operations.reconciliation.domain.PaymentReconciliationOutcome;
import com.capsule.insurance.operations.reconciliation.domain.PaymentReconciliationRunOptions;
import com.capsule.insurance.operations.reconciliation.domain.PaymentReconciliationTarget;
import com.capsule.insurance.operations.reconciliation.dto.PaymentReconciliationExecutionResponse;
import com.capsule.insurance.payment.application.PaymentService;
import com.capsule.insurance.payment.dto.PaymentOrderResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Service
public class PaymentReconciliationBatchService {

    static final String JOB_NAME = "PAYMENT_RECONCILIATION";
    private static final Duration LOCK_LEASE = Duration.ofMinutes(2);
    private static final Duration BASE_RETRY_DELAY = Duration.ofSeconds(30);

    private final PaymentReconciliationJobRepository repository;
    private final PaymentService paymentService;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    @Autowired
    public PaymentReconciliationBatchService(
            PaymentReconciliationJobRepository repository,
            PaymentService paymentService,
            PlatformTransactionManager transactionManager
    ) {
        this(repository, paymentService, transactionManager, Clock.systemUTC());
    }

    public PaymentReconciliationBatchService(
            PaymentReconciliationJobRepository repository,
            PaymentService paymentService,
            PlatformTransactionManager transactionManager,
            Clock clock
    ) {
        this.repository = repository;
        this.paymentService = paymentService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    public PaymentReconciliationExecutionResponse run(
            String instanceKey,
            PaymentReconciliationRunOptions options
    ) {
        validate(instanceKey, options);
        String workerId = "PAY-RECON-" + UUID.randomUUID();
        Instant requestedAt = Instant.now(clock);

        PaymentReconciliationExecution execution = Objects.requireNonNull(
                transactionTemplate.execute(status -> {
                    repository.lockInstance(JOB_NAME, instanceKey);
                    return acquireExecution(
                            instanceKey,
                            requestedAt.minus(options.staleAfter()),
                            workerId
                    );
                })
        );
        if ("COMPLETED".equals(execution.status())) {
            return toResponse(execution);
        }

        int chunksProcessedThisRun = 0;
        try {
            while (true) {
                PaymentReconciliationExecution current = execution;
                Instant claimedAt = Instant.now(clock);
                List<PaymentReconciliationTarget> targets = Objects.requireNonNull(
                        transactionTemplate.execute(status -> repository.claimChunk(
                                workerId,
                                current.lastPaymentOrderId(),
                                options.chunkSize(),
                                current.cutoffAt(),
                                claimedAt.minus(LOCK_LEASE),
                                claimedAt
                        ))
                );
                if (targets.isEmpty()) {
                    break;
                }

                for (int index = 0; index < targets.size(); index++) {
                    PaymentReconciliationTarget target = targets.get(index);
                    TargetResult targetResult = reconcile(target);
                    long processedCount = execution.processedCount() + 1;
                    long resolvedCount = execution.resolvedCount()
                            + count(targetResult.outcome(), PaymentReconciliationOutcome.RESOLVED);
                    long stillUnknownCount = execution.stillUnknownCount()
                            + count(targetResult.outcome(), PaymentReconciliationOutcome.STILL_UNKNOWN);
                    long failedCount = execution.failedCount()
                            + count(targetResult.outcome(), PaymentReconciliationOutcome.FAILED);
                    int processedChunks = execution.processedChunks()
                            + (index == targets.size() - 1 ? 1 : 0);
                    Instant nextAvailableAt = Instant.now(clock).plus(
                            nextDelay(target, targetResult.outcome())
                    );

                    execution = Objects.requireNonNull(transactionTemplate.execute(status -> {
                        if (targetResult.outcome() == PaymentReconciliationOutcome.FAILED) {
                            repository.recordFailure(target, targetResult.errorReason());
                        }
                        return repository.saveTargetResult(
                                current.jobExecutionId(),
                                target,
                                workerId,
                                targetResult.outcome(),
                                nextAvailableAt,
                                processedChunks,
                                processedCount,
                                resolvedCount,
                                stillUnknownCount,
                                failedCount
                        );
                    }));
                }

                chunksProcessedThisRun++;
                if (options.failAfterChunks() != null
                        && chunksProcessedThisRun >= options.failAfterChunks()) {
                    PaymentReconciliationExecution interrupted = execution;
                    transactionTemplate.execute(status -> repository.fail(
                            interrupted.jobExecutionId(),
                            "INJECTED_FAILURE_AFTER_CHUNK_" + interrupted.processedChunks()
                    ));
                    throw new PaymentReconciliationInterruptedException(
                            "검증용 강제 실패가 chunk " + interrupted.processedChunks() + " 이후 발생했습니다."
                    );
                }
            }

            PaymentReconciliationExecution completed = execution;
            return toResponse(Objects.requireNonNull(transactionTemplate.execute(status ->
                    repository.complete(completed.jobExecutionId())
            )));
        } catch (PaymentReconciliationInterruptedException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            PaymentReconciliationExecution failedExecution = execution;
            transactionTemplate.execute(status -> repository.fail(
                    failedExecution.jobExecutionId(),
                    compactError(exception)
            ));
            throw exception;
        }
    }

    public PaymentReconciliationExecutionResponse getExecution(Long jobExecutionId) {
        return repository.findById(jobExecutionId)
                .map(this::toResponse)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "결제 대사 실행 원장을 찾을 수 없습니다."
                ));
    }

    private PaymentReconciliationExecution acquireExecution(
            String instanceKey,
            Instant cutoffAt,
            String workerId
    ) {
        return repository.findLatest(JOB_NAME, instanceKey)
                .map(existing -> switch (existing.status()) {
                    case "COMPLETED" -> existing;
                    case "FAILED", "STOPPED" -> repository.resume(existing.jobExecutionId(), workerId);
                    case "STARTING", "RUNNING" -> throw new BusinessException(
                            ErrorCode.DUPLICATED_RESOURCE,
                            "동일한 결제 대사 instance가 이미 실행 중입니다."
                    );
                    default -> throw new IllegalStateException(
                            "지원하지 않는 결제 대사 상태입니다: " + existing.status()
                    );
                })
                .orElseGet(() -> repository.create(JOB_NAME, instanceKey, cutoffAt, workerId));
    }

    private TargetResult reconcile(PaymentReconciliationTarget target) {
        try {
            PaymentOrderResponse response = paymentService.reconcile(target.paymentOrderId());
            PaymentReconciliationOutcome outcome = "UNKNOWN".equals(response.status())
                    ? PaymentReconciliationOutcome.STILL_UNKNOWN
                    : PaymentReconciliationOutcome.RESOLVED;
            return new TargetResult(outcome, null);
        } catch (RuntimeException exception) {
            return new TargetResult(PaymentReconciliationOutcome.FAILED, compactError(exception));
        }
    }

    private Duration nextDelay(
            PaymentReconciliationTarget target,
            PaymentReconciliationOutcome outcome
    ) {
        if (outcome == PaymentReconciliationOutcome.RESOLVED) {
            return Duration.ZERO;
        }
        int exponent = Math.max(0, Math.min(target.reconciliationAttemptCount() - 1, 5));
        return BASE_RETRY_DELAY.multipliedBy(1L << exponent);
    }

    private long count(PaymentReconciliationOutcome actual, PaymentReconciliationOutcome expected) {
        return actual == expected ? 1L : 0L;
    }

    private void validate(String instanceKey, PaymentReconciliationRunOptions options) {
        if (!StringUtils.hasText(instanceKey) || instanceKey.length() > 255) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "instanceKey는 1자 이상 255자 이하여야 합니다."
            );
        }
        if (options == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "결제 대사 실행 옵션이 필요합니다.");
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

    private PaymentReconciliationExecutionResponse toResponse(
            PaymentReconciliationExecution execution
    ) {
        return new PaymentReconciliationExecutionResponse(
                execution.jobExecutionId(),
                execution.jobName(),
                execution.instanceKey(),
                execution.executionNo(),
                execution.status(),
                execution.cutoffAt(),
                execution.lastPaymentOrderId(),
                execution.processedChunks(),
                execution.workerId(),
                execution.processedCount(),
                execution.resolvedCount(),
                execution.stillUnknownCount(),
                execution.failedCount(),
                execution.controlTotalMatched(),
                execution.startedAt(),
                execution.finishedAt(),
                execution.errorReason()
        );
    }

    private record TargetResult(PaymentReconciliationOutcome outcome, String errorReason) {
    }
}

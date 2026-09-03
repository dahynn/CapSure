package com.capsule.insurance.operations.reconciliation.application.port;

import com.capsule.insurance.operations.reconciliation.domain.PaymentReconciliationExecution;
import com.capsule.insurance.operations.reconciliation.domain.PaymentReconciliationOutcome;
import com.capsule.insurance.operations.reconciliation.domain.PaymentReconciliationTarget;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PaymentReconciliationJobRepository {

    void lockInstance(String jobName, String instanceKey);

    Optional<PaymentReconciliationExecution> findLatest(String jobName, String instanceKey);

    Optional<PaymentReconciliationExecution> findById(Long jobExecutionId);

    PaymentReconciliationExecution create(
            String jobName,
            String instanceKey,
            Instant cutoffAt,
            String workerId
    );

    PaymentReconciliationExecution resume(Long jobExecutionId, String workerId);

    List<PaymentReconciliationTarget> claimChunk(
            String workerId,
            long afterPaymentOrderId,
            int chunkSize,
            Instant cutoffAt,
            Instant lockExpiredBefore,
            Instant claimedAt
    );

    void recordFailure(PaymentReconciliationTarget target, String errorReason);

    PaymentReconciliationExecution saveTargetResult(
            Long jobExecutionId,
            PaymentReconciliationTarget target,
            String workerId,
            PaymentReconciliationOutcome outcome,
            Instant nextAvailableAt,
            int processedChunks,
            long processedCount,
            long resolvedCount,
            long stillUnknownCount,
            long failedCount
    );

    PaymentReconciliationExecution complete(Long jobExecutionId);

    PaymentReconciliationExecution fail(Long jobExecutionId, String errorReason);
}

package com.capsule.insurance.operations.dashboard.application;

import com.capsule.insurance.common.exception.BusinessException;
import com.capsule.insurance.common.exception.ErrorCode;
import com.capsule.insurance.operations.dashboard.application.port.OperationsDashboardRepository;
import com.capsule.insurance.operations.dashboard.domain.OperationsDashboardSnapshot;
import com.capsule.insurance.operations.dashboard.dto.OperationsDashboardResponse;
import com.capsule.insurance.payment.application.port.PaymentInterfaceCircuitStatusProvider;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class OperationsDashboardService {

    private static final int MAX_RECENT_LIMIT = 50;

    private final OperationsDashboardRepository repository;
    private final PaymentInterfaceCircuitStatusProvider paymentInterfaceCircuitStatusProvider;

    public OperationsDashboardService(
            OperationsDashboardRepository repository,
            PaymentInterfaceCircuitStatusProvider paymentInterfaceCircuitStatusProvider
    ) {
        this.repository = repository;
        this.paymentInterfaceCircuitStatusProvider = paymentInterfaceCircuitStatusProvider;
    }

    public OperationsDashboardResponse getDashboard(int recentLimit) {
        if (recentLimit < 1 || recentLimit > MAX_RECENT_LIMIT) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "최근 이력 조회 건수는 1건 이상 50건 이하여야 합니다."
            );
        }

        OperationsDashboardSnapshot snapshot = repository.load(recentLimit);
        return new OperationsDashboardResponse(
                determineOverallStatus(snapshot),
                Instant.now(),
                snapshot.outbox(),
                snapshot.reconciliation(),
                snapshot.recovery(),
                snapshot.paymentInterface(),
                paymentInterfaceCircuitStatusProvider.currentStatus(),
                snapshot.recentJobs(),
                snapshot.deadLetters(),
                snapshot.recentReconciliations(),
                snapshot.recentRecoveryActions(),
                snapshot.recentPaymentInterfaceMessages()
        );
    }

    private String determineOverallStatus(OperationsDashboardSnapshot snapshot) {
        OperationsDashboardSnapshot.OutboxMetrics outbox = snapshot.outbox();
        OperationsDashboardSnapshot.ReconciliationMetrics reconciliation = snapshot.reconciliation();

        if (paymentInterfaceCircuitStatusProvider.currentStatus().open()
                || outbox.failedCount() > 0
                || outbox.pendingDeadLetterCount() > 0
                || reconciliation.failedLatestExecutionCount() > 0) {
            return "CRITICAL";
        }
        if (outbox.pendingCount() > 0
                || outbox.processingCount() > 0
                || reconciliation.waitingOrderCount() > 0
                || reconciliation.runningExecutionCount() > 0) {
            return "ATTENTION";
        }
        return "HEALTHY";
    }
}

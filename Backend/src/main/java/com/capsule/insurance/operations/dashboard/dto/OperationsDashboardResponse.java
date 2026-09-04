package com.capsule.insurance.operations.dashboard.dto;

import com.capsule.insurance.operations.dashboard.domain.OperationsDashboardSnapshot;
import com.capsule.insurance.payment.application.port.PaymentInterfaceCircuitStatusProvider;
import java.time.Instant;
import java.util.List;

public record OperationsDashboardResponse(
        String overallStatus,
        Instant refreshedAt,
        OperationsDashboardSnapshot.OutboxMetrics outbox,
        OperationsDashboardSnapshot.ReconciliationMetrics reconciliation,
        OperationsDashboardSnapshot.RecoveryMetrics recovery,
        OperationsDashboardSnapshot.PaymentInterfaceMetrics paymentInterface,
        PaymentInterfaceCircuitStatusProvider.CircuitStatus paymentInterfaceCircuit,
        List<OperationsDashboardSnapshot.JobExecutionItem> recentJobs,
        List<OperationsDashboardSnapshot.DeadLetterItem> deadLetters,
        List<OperationsDashboardSnapshot.ReconciliationItem> recentReconciliations,
        List<OperationsDashboardSnapshot.RecoveryActionItem> recentRecoveryActions,
        List<OperationsDashboardSnapshot.PaymentInterfaceMessageItem> recentPaymentInterfaceMessages
) {
}

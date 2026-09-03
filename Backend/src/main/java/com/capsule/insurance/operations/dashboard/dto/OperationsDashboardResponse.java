package com.capsule.insurance.operations.dashboard.dto;

import com.capsule.insurance.operations.dashboard.domain.OperationsDashboardSnapshot;
import java.time.Instant;
import java.util.List;

public record OperationsDashboardResponse(
        String overallStatus,
        Instant refreshedAt,
        OperationsDashboardSnapshot.OutboxMetrics outbox,
        OperationsDashboardSnapshot.ReconciliationMetrics reconciliation,
        OperationsDashboardSnapshot.RecoveryMetrics recovery,
        List<OperationsDashboardSnapshot.JobExecutionItem> recentJobs,
        List<OperationsDashboardSnapshot.DeadLetterItem> deadLetters,
        List<OperationsDashboardSnapshot.ReconciliationItem> recentReconciliations,
        List<OperationsDashboardSnapshot.RecoveryActionItem> recentRecoveryActions
) {
}

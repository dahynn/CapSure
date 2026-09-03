package com.capsule.insurance.operations.recovery.dto;

import com.capsule.insurance.operations.reconciliation.dto.PaymentReconciliationExecutionResponse;

public record PaymentReconciliationRecoveryResponse(
        OperationsRecoveryActionResponse recovery,
        PaymentReconciliationExecutionResponse execution
) {
}

package com.capsule.insurance.operations.reconciliation.domain;

public record PaymentReconciliationTarget(
        Long paymentOrderId,
        String localStatus,
        int reconciliationAttemptCount
) {
}

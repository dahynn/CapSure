package com.capsule.insurance.operations.reconciliation.domain;

public class PaymentReconciliationInterruptedException extends RuntimeException {

    public PaymentReconciliationInterruptedException(String message) {
        super(message);
    }
}

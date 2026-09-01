package com.capsule.insurance.payment.domain;

import java.time.Instant;

public record PaymentAttempt(
        Long paymentAttemptId,
        Long paymentOrderId,
        int attemptNo,
        String provider,
        String providerPaymentKey,
        String idempotencyKey,
        String status,
        String errorCode,
        Instant requestedAt,
        Instant completedAt
) {
}

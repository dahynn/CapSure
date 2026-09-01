package com.capsule.insurance.payment.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentOrder(
        Long paymentOrderId,
        String orderNo,
        String businessKey,
        Long applicationId,
        Long policyId,
        Long ownerUserId,
        String purpose,
        BigDecimal amount,
        String currencyCode,
        String status,
        String creationIdempotencyKey,
        Instant expiresAt,
        Instant paidAt,
        Instant createdAt,
        Instant updatedAt
) {
}

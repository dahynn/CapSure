package com.capsule.insurance.claim.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record ClaimPayment(
        Long claimPaymentId,
        Long claimDecisionId,
        String payoutOrderNo,
        BigDecimal amount,
        String currencyCode,
        String status,
        String idempotencyKey,
        Instant paidAt,
        Instant createdAt,
        Instant updatedAt
) {
}

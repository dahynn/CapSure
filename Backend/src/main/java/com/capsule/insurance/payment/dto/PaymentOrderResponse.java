package com.capsule.insurance.payment.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PaymentOrderResponse(
        Long paymentOrderId,
        String orderNo,
        Long applicationId,
        Long policyId,
        String purpose,
        BigDecimal amount,
        String currencyCode,
        String status,
        List<AttemptResponse> attempts,
        String policyStatus,
        Instant expiresAt,
        Instant paidAt,
        Instant createdAt,
        Instant updatedAt
) {

    public record AttemptResponse(
            Long paymentAttemptId,
            int attemptNo,
            String provider,
            String providerPaymentKey,
            String status,
            String errorCode,
            Instant requestedAt,
            Instant completedAt
    ) {
    }
}

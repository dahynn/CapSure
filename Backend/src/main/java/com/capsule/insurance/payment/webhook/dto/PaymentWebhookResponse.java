package com.capsule.insurance.payment.webhook.dto;

import java.time.Instant;

public record PaymentWebhookResponse(
        Long paymentWebhookEventId,
        String providerEventId,
        String processingStatus,
        boolean duplicate,
        Long paymentOrderId,
        String paymentStatus,
        String policyStatus,
        Instant receivedAt,
        Instant processedAt
) {
}

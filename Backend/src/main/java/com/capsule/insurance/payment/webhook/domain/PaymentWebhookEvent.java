package com.capsule.insurance.payment.webhook.domain;

import java.time.Instant;

public record PaymentWebhookEvent(
        Long paymentWebhookEventId,
        String provider,
        String providerEventId,
        String providerPaymentKey,
        String eventType,
        String payloadHash,
        String processingStatus,
        Instant receivedAt,
        Instant processedAt,
        String errorReason
) {
}

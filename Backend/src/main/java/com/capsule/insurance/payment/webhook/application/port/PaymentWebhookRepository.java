package com.capsule.insurance.payment.webhook.application.port;

import com.capsule.insurance.payment.webhook.domain.PaymentWebhookEvent;
import java.util.Optional;

public interface PaymentWebhookRepository {

    Optional<PaymentWebhookEvent> findByProviderEventId(String provider, String providerEventId);

    PaymentWebhookEvent saveReceived(
            String provider,
            String providerEventId,
            String providerPaymentKey,
            String eventType,
            String payloadJson,
            String payloadHash
    );

    PaymentWebhookEvent markProcessed(Long paymentWebhookEventId);

    PaymentWebhookEvent markFailed(Long paymentWebhookEventId, String errorReason);
}

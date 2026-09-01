package com.capsule.insurance.payment.webhook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record FakePaymentWebhookRequest(
        @NotBlank String providerEventId,
        @NotBlank String providerPaymentKey,
        @NotBlank String eventType,
        @NotBlank @Pattern(regexp = "PAID|FAILED|UNKNOWN") String status
) {
}

package com.capsule.insurance.subscription.dto;

public record CurrentPaymentMethodResponse(
        String provider,
        String methodType,
        String maskedNumber,
        boolean active
) {
}

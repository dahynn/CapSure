package com.capsule.insurance.subscription.dto;

public record ConfirmNextResponse(
        Long subscriptionId,
        String nextBillingAt,
        Integer confirmedItemCount
) {}

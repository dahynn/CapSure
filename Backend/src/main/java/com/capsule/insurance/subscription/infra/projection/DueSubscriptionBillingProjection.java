package com.capsule.insurance.subscription.infra.projection;

import java.time.Instant;

public record DueSubscriptionBillingProjection(
        Long subscriptionId,
        Long userId,
        String capsuleName,
        Instant nextBillingAt
) {
}

package com.capsule.insurance.subscription.infra.projection;

import java.math.BigDecimal;
import java.time.Instant;

public record RecentSubscriptionHomeProjection(
        Long capsuleSnapshotId,
        Long subscriptionId,
        String capsuleName,
        Instant createdAt,
        Instant nextBillingAt,
        BigDecimal expectedNextAmount
) {
}

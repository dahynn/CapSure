// #Demo Setting
package com.capsule.insurance.subscription.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Subscription {

    private final Long subscriptionId;
    private final Long userId;
    private final SubscriptionStatus subscriptionStatus;
    private final Integer billingAnchorDay;
    private final LocalDateTime currentCycleStartAt;
    private final LocalDateTime currentCycleEndAt;
    private final LocalDateTime nextBillingAt;
    private final BigDecimal expectedNextAmount;
    private final LocalDateTime pausedAt;
    private final LocalDateTime cancelledAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}

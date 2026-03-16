// #Demo Setting
package com.capsule.insurance.subscription.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SubscriptionItem {

    private final Long subscriptionItemId;
    private final Long subscriptionId;
    private final Long capsuleProductId;
    private final PlanVersion planVersion;
    private final SubscriptionItemStatus itemStatus;
    private final BigDecimal coverageAmountSnapshot;
    private final BigDecimal monthlyPriceSnapshot;
    private final LocalDateTime effectiveStartAt;
    private final LocalDateTime effectiveEndAt;
    private final LocalDateTime editableAfterAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}

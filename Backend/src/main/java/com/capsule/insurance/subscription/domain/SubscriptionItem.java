// #Demo Setting
package com.capsule.insurance.subscription.domain;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class SubscriptionItem {

    private Long subscriptionItemId;
    private Long subscriptionId;
    private Long capsuleProductId;
    private PlanVersion planVersion;
    private SubscriptionItemStatus itemStatus;
    private BigDecimal coverageAmountSnapshot;
    private BigDecimal monthlyPriceSnapshot;
    private Instant effectiveStartAt;
    private Instant effectiveEndAt;
    private Instant editableAfterAt;
    private Instant createdAt;
    private Instant updatedAt;

    public boolean isCurrentPlan() {
        return planVersion == PlanVersion.CURRENT;
    }

    public boolean isNextPlan() {
        return planVersion == PlanVersion.NEXT;
    }
}

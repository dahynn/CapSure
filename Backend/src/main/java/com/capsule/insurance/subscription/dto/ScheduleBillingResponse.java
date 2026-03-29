package com.capsule.insurance.subscription.dto;

import java.math.BigDecimal;
import java.util.List;

public record ScheduleBillingResponse(
        Long subscriptionId,
        Integer activeSubscriptionCount,
        Integer billingAnchorDay,
        String nextBillingAt,
        BigDecimal expectedNextAmount,
        List<UpcomingBilling> upcomingBillings,
        List<ScheduleBillingItem> currentItems,
        List<ScheduleBillingItem> nextItems
) {
    public record UpcomingBilling(
            Long subscriptionId,
            String capsuleName,
            Integer billingAnchorDay,
            String nextBillingAt,
            BigDecimal expectedAmount
    ) {
    }

    public record ScheduleBillingItem(
            Long subscriptionId,
            String capsuleName,
            Long subscriptionItemId,
            Long productSourceId,
            String productName,
            BigDecimal monthlyPrice,
            String itemStatus
    ) {
    }
}

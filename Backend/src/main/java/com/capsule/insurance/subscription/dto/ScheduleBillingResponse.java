package com.capsule.insurance.subscription.dto;

import java.math.BigDecimal;
import java.util.List;

public record ScheduleBillingResponse(
        Long subscriptionId,
        Integer billingAnchorDay,
        String nextBillingAt,
        BigDecimal expectedNextAmount,
        List<ScheduleBillingItem> currentItems,
        List<ScheduleBillingItem> nextItems
) {
    public record ScheduleBillingItem(
            Long subscriptionItemId,
            Long capsuleProductId,
            String productName,
            BigDecimal monthlyPrice,
            String itemStatus
    ) {
    }
}

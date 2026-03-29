package com.capsule.insurance.subscription.dto;

import java.math.BigDecimal;
import java.util.List;

public record MonthlyBillingResponse(
        Long subscriptionId,
        Integer activeSubscriptionCount,
        BigDecimal totalMonthlyBilling,
        String nextBillingAt,
        List<MonthlyBillingItem> items
) {
    public record MonthlyBillingItem(
            Long subscriptionId,
            String capsuleName,
            Long subscriptionItemId,
            Long capsuleProductId,
            String productName,
            BigDecimal monthlyPrice,
            String itemStatus
    ) {
    }
}

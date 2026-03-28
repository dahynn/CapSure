package com.capsule.insurance.subscription.dto;

import java.math.BigDecimal;
import java.util.List;

public record MonthlyBillingResponse(
        Long subscriptionId,
        BigDecimal totalMonthlyBilling,
        String nextBillingAt,
        List<MonthlyBillingItem> items
) {
    public record MonthlyBillingItem(
            Long subscriptionItemId,
            Long capsuleProductId,
            String productName,
            BigDecimal monthlyPrice,
            String itemStatus
    ) {
    }
}

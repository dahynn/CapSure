package com.capsule.insurance.subscription.dto;

import java.math.BigDecimal;
import java.util.List;

public record MyCapsuleSummaryResponse(
        Long subscriptionId,
        String capsuleName,
        String subscribedDate,
        String nextBillingAt,
        BigDecimal monthlyTotalPremium,
        List<String> categories
) {
}

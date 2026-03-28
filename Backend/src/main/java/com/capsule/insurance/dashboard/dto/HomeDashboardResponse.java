package com.capsule.insurance.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;

public record HomeDashboardResponse(
        List<SubscribedCapsuleCard> subscribedCapsules,
        List<ActiveInsuranceCard> activeInsurances
) {
    public record SubscribedCapsuleCard(
            Long capsuleSnapshotId,
            Long subscriptionId,
            String capsuleName,
            String subscribedDate,
            String nextBillingAt,
            BigDecimal monthlyTotalPremium,
            List<String> categories
    ) {
    }

    public record ActiveInsuranceCard(
            Long subscriptionId,
            Long productSourceId,
            String productName,
            String companyName,
            String category,
            BigDecimal monthlyPremium,
            String nextBillingAt,
            long daysUntilRenewal
    ) {
    }
}

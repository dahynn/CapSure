package com.capsule.insurance.subscription.infra.projection;

import java.math.BigDecimal;
import java.time.Instant;

public record RenewalSoonInsuranceProjection(
        Long subscriptionId,
        Long productSourceId,
        String productName,
        String companyName,
        String coverageCategoryCode,
        BigDecimal monthlyPrice,
        Instant nextBillingAt
) {
}

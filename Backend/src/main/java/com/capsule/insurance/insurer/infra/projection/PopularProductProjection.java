package com.capsule.insurance.insurer.infra.projection;

import java.math.BigDecimal;

public record PopularProductProjection(
        Long productSourceId,
        String companyName,
        String productName,
        String coverageCategoryCode,
        BigDecimal monthlyPrice,
        long subscriberCount,
        int categoryRank
) {
}

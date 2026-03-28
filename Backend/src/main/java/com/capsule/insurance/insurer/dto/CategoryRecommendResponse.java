package com.capsule.insurance.insurer.dto;

import java.math.BigDecimal;

public record CategoryRecommendResponse(
        Long productSourceId,
        String companyName,
        String productName,
        String coverageCategoryCode,
        BigDecimal monthlyPrice,
        long subscriberCount
) {
}

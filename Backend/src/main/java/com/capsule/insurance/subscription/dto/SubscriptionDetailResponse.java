package com.capsule.insurance.subscription.dto;

import java.math.BigDecimal;
import java.util.List;

public record SubscriptionDetailResponse(
        Long subscriptionId,
        String name,
        String status,
        String date,
        BigDecimal totalPremium,
        List<ProductDto> products,
        List<CoverageDto> coverages
) {
    public record ProductDto(
            Long id,
            String productName,  // name -> productName
            String companyName,  // company -> companyName
            String type
    ) {}

    public record CoverageDto(
            String label,
            String amount
    ) {}
}

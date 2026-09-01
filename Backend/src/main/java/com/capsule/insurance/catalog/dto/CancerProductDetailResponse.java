package com.capsule.insurance.catalog.dto;

import java.math.BigDecimal;
import java.util.List;

public record CancerProductDetailResponse(
        CancerProductSummaryResponse product,
        List<CoverageResponse> coverages
) {

    public record CoverageResponse(
            Long productCoverageId,
            String coverageCode,
            String coverageName,
            String coverageCategory,
            String benefitType,
            String description,
            BigDecimal insuredAmount,
            String currencyCode,
            int waitingPeriodDays,
            int reductionPeriodDays,
            BigDecimal reductionRate,
            String coverageStartRule,
            int displayOrder,
            Long termsClauseId,
            String termsClauseCode
    ) {
    }
}

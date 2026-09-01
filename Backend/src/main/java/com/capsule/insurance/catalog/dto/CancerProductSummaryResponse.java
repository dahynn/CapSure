package com.capsule.insurance.catalog.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CancerProductSummaryResponse(
        Long productVersionId,
        String productCode,
        String version,
        String productName,
        String insurerName,
        String insurerSector,
        LocalDate saleFrom,
        LocalDate saleTo,
        String status,
        BigDecimal baseMonthlyPremium,
        String currencyCode,
        boolean simulation,
        long coverageCount,
        Long termsDocumentId,
        String termsVersion,
        String termsSourceHash
) {
}

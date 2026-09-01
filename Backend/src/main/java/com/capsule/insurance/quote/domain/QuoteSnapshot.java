package com.capsule.insurance.quote.domain;

import java.math.BigDecimal;
import java.util.List;

public record QuoteSnapshot(
        Long productVersionId,
        String productCode,
        String productVersion,
        String productName,
        BigDecimal monthlyPremium,
        String currencyCode,
        Long termsDocumentId,
        String termsVersion,
        String termsHash,
        List<CoverageSnapshot> coverages
) {

    public QuoteSnapshot {
        coverages = List.copyOf(coverages);
    }

    public record CoverageSnapshot(
            Long productCoverageId,
            String coverageCode,
            String coverageName,
            BigDecimal insuredAmount,
            String currencyCode,
            int waitingPeriodDays,
            int reductionPeriodDays,
            BigDecimal reductionRate,
            String coverageStartRule,
            Long termsClauseId,
            String termsClauseCode
    ) {
    }
}

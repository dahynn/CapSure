package com.capsule.insurance.catalog.domain;

import java.math.BigDecimal;

public record Coverage(
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

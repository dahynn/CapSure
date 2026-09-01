package com.capsule.insurance.operations.catalog.domain;

import java.math.BigDecimal;

public record CatalogImportRow(
        String sourceKey,
        String productCode,
        String productVersion,
        String coverageCode,
        BigDecimal insuredAmount,
        String currencyCode,
        Integer waitingPeriodDays,
        Integer reductionPeriodDays,
        BigDecimal reductionRate,
        String coverageStartRule,
        Integer displayOrder
) {
}

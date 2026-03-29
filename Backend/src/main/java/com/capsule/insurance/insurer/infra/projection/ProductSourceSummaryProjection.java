package com.capsule.insurance.insurer.infra.projection;

import com.capsule.insurance.insurer.domain.InsurerSector;
import java.math.BigDecimal;
import java.time.Instant;

public record ProductSourceSummaryProjection(
        Long productSourceId,
        String companyName,
        String productName,
        InsurerSector insurerSector,
        String coverageCategoryCode,
        String coverageCode,
        BigDecimal monthlyPrice,
        String priceIndexText,
        String termsUri,
        Instant loadedAt,
        Instant updatedAt
) {
}

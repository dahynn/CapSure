package com.capsule.insurance.insurer.infra.projection;

import com.capsule.insurance.insurer.domain.InsurerSector;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductSourceDetailProjection(
        Long productSourceId,
        String companyName,
        String productName,
        InsurerSector insurerSector,
        String saleChannel,
        String coverageName,
        String claimReason,
        String payoutAmount,
        String joinAmount,
        String minimumJoinPremium,
        String paymentCycle,
        String paymentTerm,
        String coverageTerm,
        String coverageCategoryCode,
        String coverageCode,
        BigDecimal monthlyPrice,
        String productSummary,
        String productFeature,
        String specialNote,
        String contactPhone,
        LocalDate saleDate,
        String currentAnnouncedRate,
        String fixedRate,
        String minimumGuaranteedRate
) {
}

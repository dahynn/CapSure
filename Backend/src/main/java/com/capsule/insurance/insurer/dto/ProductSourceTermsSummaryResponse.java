package com.capsule.insurance.insurer.dto;

import java.util.List;

public record ProductSourceTermsSummaryResponse(
        Long productSourceId,
        String companyName,
        String productName,
        String saleChannel,
        String coverageName,
        String headline,
        List<String> clauseHighlights,
        String coverageSummary,
        String subscriptionConditions,
        String premiumAndPriceIndex,
        String refundAndInterest,
        String specialNotes,
        PriceComparison priceComparison,
        String disclaimer
) {

    public record PriceComparison(
            String maleMonthlyPremium,
            String femaleMonthlyPremium,
            String malePriceIndex,
            String femalePriceIndex,
            String advantageousGender,
            String advantageousReason
    ) {
    }
}

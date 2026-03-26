package com.capsule.insurance.insurer.dto;

public record ProductSourceLightSummaryResponse(
        Long productSourceId,
        String companyName,
        String productName,
        String paymentSummary,
        String coverageSummary,
        String featureSummary,
        String disclaimer
) {
}

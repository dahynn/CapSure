package com.capsule.insurance.premiumcollection.dto;

import java.time.LocalDate;

public record PremiumBillingRunResponse(
        long runId,
        String instanceKey,
        LocalDate billingCycle,
        LocalDate businessDate,
        String status,
        long targetCount,
        long processedCount,
        long createdCount,
        long existingCount,
        long ineligibleCount,
        long remainingCount,
        boolean controlTotalMatched,
        String errorReason
) {
}

package com.capsule.insurance.premiumcollection.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PremiumCollectionTimelineResponse(
        long dueCount, long graceCount, long overpaidCount, long refundPendingCount,
        List<Item> items
) {
    public record Item(Long premiumReceivableId, Long policyId, LocalDate billingCycle,
                       BigDecimal amountDue, BigDecimal amountSettled, String receivableStatus,
                       String instructionStatus, String refundStatus, Instant updatedAt) { }
}

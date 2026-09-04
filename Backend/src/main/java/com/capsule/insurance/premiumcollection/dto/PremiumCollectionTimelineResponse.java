package com.capsule.insurance.premiumcollection.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PremiumCollectionTimelineResponse(
        long dueCount, long graceCount, long overpaidCount, long refundPendingCount,
        long lapsedPolicyCount, long lateSettlementReviewCount,
        List<Item> items
) {
    public record Item(Long premiumReceivableId, Long policyId, LocalDate billingCycle,
                       BigDecimal amountDue, BigDecimal amountSettled, String receivableStatus,
                       String instructionStatus, String refundStatus, Instant updatedAt,
                       String policyStatus, LocalDate dueDate, LocalDate effectiveGraceEndsOn,
                       String noticeStatus, String changeReason, Instant lapsedAt, long lateReviewCount) { }
}

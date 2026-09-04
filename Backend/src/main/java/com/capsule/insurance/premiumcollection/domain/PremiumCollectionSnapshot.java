package com.capsule.insurance.premiumcollection.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record PremiumCollectionSnapshot(
        Long premiumReceivableId,
        Long policyId,
        LocalDate billingCycle,
        LocalDate dueDate,
        LocalDate graceEndsOn,
        BigDecimal amountDue,
        BigDecimal amountSettled,
        String status,
        Long collectionInstructionId,
        String collectionStatus,
        Instant updatedAt
) {
}

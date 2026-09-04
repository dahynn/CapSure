package com.capsule.insurance.premiumcollection.dto;

import com.capsule.insurance.premiumcollection.domain.PremiumCollectionSnapshot;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PremiumCollectionResponse(
        Long premiumReceivableId,
        Long policyId,
        LocalDate billingCycle,
        BigDecimal amountDue,
        BigDecimal amountSettled,
        String status,
        Long collectionInstructionId,
        String collectionStatus
) {
    public static PremiumCollectionResponse from(PremiumCollectionSnapshot snapshot) {
        return new PremiumCollectionResponse(snapshot.premiumReceivableId(), snapshot.policyId(), snapshot.billingCycle(),
                snapshot.amountDue(), snapshot.amountSettled(), snapshot.status(), snapshot.collectionInstructionId(), snapshot.collectionStatus());
    }
}

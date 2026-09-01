package com.capsule.insurance.payment.domain;

import java.math.BigDecimal;

public record ApprovedApplication(
        Long applicationId,
        Long applicantUserId,
        Long insuredUserId,
        String applicationStatus,
        BigDecimal initialPremium,
        String currencyCode
) {
}

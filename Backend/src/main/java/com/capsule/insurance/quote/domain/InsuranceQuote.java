package com.capsule.insurance.quote.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record InsuranceQuote(
        Long quoteId,
        String quoteNo,
        Long userId,
        Long productVersionId,
        String status,
        BigDecimal monthlyPremium,
        String currencyCode,
        QuoteSnapshot snapshot,
        String termsDocumentHash,
        Instant expiresAt,
        Instant usedAt,
        Instant createdAt
) {
}

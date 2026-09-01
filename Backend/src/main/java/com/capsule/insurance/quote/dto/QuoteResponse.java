package com.capsule.insurance.quote.dto;

import com.capsule.insurance.quote.domain.QuoteSnapshot;
import java.math.BigDecimal;
import java.time.Instant;

public record QuoteResponse(
        Long quoteId,
        String quoteNo,
        String status,
        Long productVersionId,
        BigDecimal monthlyPremium,
        String currencyCode,
        QuoteSnapshot snapshot,
        String termsDocumentHash,
        Instant expiresAt,
        Instant usedAt,
        Instant createdAt
) {
}

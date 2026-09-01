package com.capsule.insurance.quote.application.port;

import com.capsule.insurance.quote.domain.InsuranceQuote;
import com.capsule.insurance.quote.domain.QuoteSnapshot;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

public interface QuoteRepository {

    InsuranceQuote save(
            String quoteNo,
            Long userId,
            Long productVersionId,
            BigDecimal monthlyPremium,
            String currencyCode,
            QuoteSnapshot snapshot,
            String termsDocumentHash,
            Instant expiresAt
    );

    Optional<InsuranceQuote> findOwned(Long quoteId, Long userId);

    void expireIfNeeded(Long quoteId, Instant now);
}

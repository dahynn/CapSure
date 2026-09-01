package com.capsule.insurance.application.domain;

import java.time.Instant;

public record ApplicationQuote(
        Long quoteId,
        Long userId,
        String status,
        Instant expiresAt,
        Long termsDocumentId,
        String termsDocumentHash
) {
}

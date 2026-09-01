package com.capsule.insurance.application.domain;

import java.time.Instant;

public record ApplicationConsent(
        Long consentId,
        Long applicationId,
        String consentType,
        Long termsDocumentId,
        String documentHash,
        boolean required,
        boolean agreed,
        Long actorUserId,
        Instant agreedAt,
        String requestId,
        Instant createdAt
) {
}

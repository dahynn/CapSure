package com.capsule.insurance.application.domain;

import java.time.Instant;

public record InsuranceApplication(
        Long applicationId,
        String applicationNo,
        Long quoteId,
        Long applicantUserId,
        Long insuredUserId,
        String status,
        DisclosureAnswers disclosureAnswers,
        String submissionIdempotencyKey,
        Long termsDocumentId,
        String termsDocumentHash,
        Instant submittedAt,
        Instant createdAt,
        Instant updatedAt
) {
}

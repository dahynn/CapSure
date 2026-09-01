package com.capsule.insurance.catalog.domain;

import java.time.LocalDate;

public record TermsDocument(
        Long termsDocumentId,
        String documentCode,
        String documentVersion,
        String title,
        String sourceType,
        String sourceUri,
        String sourceHash,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        String status,
        boolean simulation
) {
}
